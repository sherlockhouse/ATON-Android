package com.juzix.wallet.component.ui.contract;

import com.juzix.wallet.component.ui.base.IPresenter;
import com.juzix.wallet.component.ui.base.IView;
import com.juzix.wallet.entity.Transaction;

import java.util.List;

/**
 * @author matrixelement
 */
public class TransactionsContract {

    public interface View extends IView {

        void notifyItemRangeInserted(List<Transaction> transactionList, int positionStart, int itemCount);

        void notifyItemChanged(List<Transaction> transactionList,int position);

        void finishLoadMore();
    }

    public interface Presenter extends IPresenter<View> {

        void autoRefresh();

        void loadMore();

        void addNewTransaction(Transaction transaction);

    }

}
