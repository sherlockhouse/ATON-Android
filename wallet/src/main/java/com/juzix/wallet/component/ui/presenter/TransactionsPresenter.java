package com.juzix.wallet.component.ui.presenter;


import android.annotation.SuppressLint;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.juzhen.framework.network.ApiErrorCode;
import com.juzhen.framework.network.ApiRequestBody;
import com.juzhen.framework.network.ApiResponse;
import com.juzhen.framework.network.ApiSingleObserver;
import com.juzix.wallet.app.Constants;
import com.juzix.wallet.component.ui.base.BasePresenter;
import com.juzix.wallet.component.ui.contract.TransactionsContract;
import com.juzix.wallet.component.ui.view.AssetsFragment;
import com.juzix.wallet.component.ui.view.TransactionsFragment;
import com.juzix.wallet.db.entity.TransactionEntity;
import com.juzix.wallet.db.sqlite.TransactionDao;
import com.juzix.wallet.engine.NodeManager;
import com.juzix.wallet.engine.ServerUtils;
import com.juzix.wallet.engine.TransactionManager;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.entity.Transaction;
import com.juzix.wallet.entity.TransactionStatus;
import com.juzix.wallet.entity.Wallet;
import com.juzix.wallet.utils.RxUtils;
import com.trello.rxlifecycle2.android.FragmentEvent;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import retrofit2.Response;


/**
 * @author matrixelement
 */
public class TransactionsPresenter extends BasePresenter<TransactionsContract.View> implements TransactionsContract.Presenter {

    private static final String TAG = TransactionsPresenter.class.getSimpleName();

    public final static String DIRECTION_OLD = "old";
    public final static String DIRECTION_NEW = "new";

    private List<Transaction> mTransactionList = new ArrayList<>();
    private String mWalletAddress;
    private Disposable mAutoRefreshDisposable;

    public TransactionsPresenter(TransactionsContract.View view) {
        super(view);
        mAutoRefreshDisposable = new CompositeDisposable();
        mWalletAddress = WalletManager.getInstance().getSelectedWalletAddress();
    }


    @SuppressLint("CheckResult")
    @Override
    public void autoRefresh() {

        if (TextUtils.isEmpty(mWalletAddress)) {
            return;
        }

        if (!mAutoRefreshDisposable.isDisposed()) {
            mAutoRefreshDisposable.dispose();
        }

        getTransactionListFromDB(mWalletAddress)
                .toFlowable()
                .flatMap(new Function<List<Transaction>, Publisher<List<Transaction>>>() {
                    @Override
                    public Publisher<List<Transaction>> apply(List<Transaction> transactionList) throws Exception {
                        return Flowable
                                .interval(0, Constants.Common.TRANSCTION_LIST_LOOP_TIME, TimeUnit.MILLISECONDS)
                                .flatMap(new Function<Long, Publisher<List<Transaction>>>() {
                                    @Override
                                    public Publisher<List<Transaction>> apply(Long aLong) throws Exception {
                                        return getTransactionList(mWalletAddress, DIRECTION_NEW, getBeginSequenceByDirection(DIRECTION_NEW))
                                                .flatMap(new Function<Response<ApiResponse<List<Transaction>>>, SingleSource<List<Transaction>>>() {
                                                    @Override
                                                    public SingleSource<List<Transaction>> apply(Response<ApiResponse<List<Transaction>>> apiResponseResponse) throws Exception {
                                                        if (apiResponseResponse.isSuccessful() && apiResponseResponse.body().getResult() == ApiErrorCode.SUCCESS) {
                                                            return Single.just(apiResponseResponse.body().getData());
                                                        } else {
                                                            return Single.error(new Throwable());
                                                        }
                                                    }
                                                })
                                                .toFlowable();
                                    }
                                });
                    }
                })
                .toObservable()
                .doOnNext(new Consumer<List<Transaction>>() {
                    @Override
                    public void accept(List<Transaction> transactionList) throws Exception {
                        //存在数据就刷新余额
                        if (isViewAttached()) {
                            ((AssetsFragment) (((TransactionsFragment) getView()).getParentFragment())).fetchWalletsBalance();
                        }
                    }
                })
                .compose(RxUtils.getSchedulerTransformer())
                .compose(bindUntilEvent(FragmentEvent.STOP))
                .subscribe(new Consumer<List<Transaction>>() {
                    @Override
                    public void accept(List<Transaction> transactionList) throws Exception {
                        if (isViewAttached() && !transactionList.isEmpty()) {
                            //先进行排序
                            int oldSize = mTransactionList.size();
                            mTransactionList = addAll(transactionList, DIRECTION_NEW);
                            int newSize = mTransactionList.size();
                            Collections.sort(mTransactionList);
                            getView().notifyItemRangeInserted(mTransactionList, 0, newSize - oldSize);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, "onApiFailure");
                    }
                });
    }

    @Override
    public void loadMore() {

        if (TextUtils.isEmpty(mWalletAddress)) {
            return;
        }

        getTransactionList(mWalletAddress, DIRECTION_OLD, getBeginSequenceByDirection(DIRECTION_OLD))
                .compose(RxUtils.getSingleSchedulerTransformer())
                .compose(bindUntilEvent(FragmentEvent.STOP))
                .subscribe(new ApiSingleObserver<List<Transaction>>() {
                    @Override
                    public void onApiSuccess(List<Transaction> transactions) {
                        Log.e(TAG, "onApiSuccess");
                        if (isViewAttached()) {
                            //先进行排序
                            Collections.sort(transactions);
                            //累加,mTransactionList不可能为null,放在最后面
                            mTransactionList.addAll(mTransactionList.size(), transactions);
                            getView().notifyItemRangeInserted(mTransactionList, mTransactionList.size(), transactions.size());
                            getView().finishLoadMore();
                        }
                    }

                    @Override
                    public void onApiFailure(ApiResponse response) {

                    }
                });
    }

    @Override
    public void addNewTransaction(Transaction transaction) {
        if (mTransactionList.contains(transaction)) {
            //更新
            int index = mTransactionList.indexOf(transaction);
            mTransactionList.set(index, transaction);
            getView().notifyItemChanged(mTransactionList, index);
        } else {
            //添加
            mTransactionList.add(0, transaction);
            getView().notifyItemRangeInserted(mTransactionList, 0, 1);
        }
    }

    private Single<List<Transaction>> getTransactionListFromDB(String walletAddress) {

        return Flowable.fromCallable(new Callable<List<TransactionEntity>>() {
            @Override
            public List<TransactionEntity> call() throws Exception {
                return TransactionDao.getTransactionList(walletAddress);
            }
        }).flatMap(new Function<List<TransactionEntity>, Publisher<TransactionEntity>>() {
            @Override
            public Publisher<TransactionEntity> apply(List<TransactionEntity> transactionEntities) throws Exception {
                return Flowable.fromIterable(transactionEntities);
            }
        }).map(new Function<TransactionEntity, Transaction>() {
            @Override
            public Transaction apply(TransactionEntity transactionEntity) throws Exception {
                return transactionEntity.toTransaction();
            }
        }).map(new Function<Transaction, Transaction>() {
            @Override
            public Transaction apply(Transaction transaction) throws Exception {
                if (transaction.getTxReceiptStatus() == TransactionStatus.PENDING) {
                    TransactionManager.getInstance().getTransactionByLoop(transaction);
                }
                return transaction;
            }
        })
                .toList();
    }

    private List<Transaction> addAll(List<Transaction> transactionList, String direction) {
        return Flowable
                .fromIterable(transactionList)
                .collectInto(mTransactionList, new BiConsumer<List<Transaction>, Transaction>() {
                    @Override
                    public void accept(List<Transaction> transactionList, Transaction transaction) throws Exception {
                        //如果本地与服务器存在相同记录，以服务器记录为主
                        if (mTransactionList.contains(transaction)) {
                            int index = mTransactionList.indexOf(transaction);
                            mTransactionList.set(index, transaction);
                        } else {
                            if (DIRECTION_NEW.equals(direction)) {
                                transactionList.add(0, transaction);
                            } else {
                                transactionList.add(transactionList.size(), transaction);
                            }
                        }
                    }
                })
                .blockingGet();
    }

    private Single<Response<ApiResponse<List<Transaction>>>> getTransactionList(String walletAddress, String direction, long beginSequence) {
        return ServerUtils
                .getCommonApi()
                .getTransactionList(NodeManager.getInstance().getChainId(), ApiRequestBody.newBuilder()
                        .put("walletAddrs", new String[]{walletAddress})
                        .put("beginSequence", beginSequence)
                        .put("listSize", 10)
                        .put("direction", direction)
                        .build());
    }

    private long getBeginSequenceByDirection(String direction) {
        //拉最新的
        if (mTransactionList == null || mTransactionList.isEmpty()) {
            return -1;
        }
        //拉比当前最新的还大的
        if (DIRECTION_NEW.equals(direction)) {
            return getValidBiggerSequence(mTransactionList);
        } else {
            return getValidSmallerSequence(mTransactionList);
        }
    }

    private long getValidBiggerSequence(List<Transaction> transactionList) {

        return Flowable
                .range(0, transactionList.size())
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence() != 0;
                    }
                })
                .map(new Function<Integer, Long>() {
                    @Override
                    public Long apply(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence();
                    }
                })
                .firstElement()
                .blockingGet();
    }

    private long getValidSmallerSequence(List<Transaction> transactionList) {

        return Flowable
                .range(transactionList.size() - 1, transactionList.size())
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence() != 0;
                    }
                })
                .map(new Function<Integer, Long>() {
                    @Override
                    public Long apply(Integer integer) throws Exception {
                        return transactionList.get(integer).getSequence();
                    }
                })
                .lastElement()
                .blockingGet();
    }
}
