package com.juzix.wallet.engine;

import android.text.TextUtils;

import com.juzix.wallet.App;
import com.juzix.wallet.R;
import com.juzix.wallet.db.entity.OwnerInfoEntity;
import com.juzix.wallet.db.entity.SharedWalletInfoEntity;
import com.juzix.wallet.db.sqlite.SharedWalletInfoDao;
import com.juzix.wallet.entity.OwnerEntity;
import com.juzix.wallet.entity.SharedWalletEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

public class SharedWalletManager {

    private List<SharedWalletEntity> mWalletList = new ArrayList<>();


    private SharedWalletManager() {
    }

    public static SharedWalletManager getInstance() {
        return SharedWalletManager.InstanceHolder.INSTANCE;
    }

    public void init() {
        if (!mWalletList.isEmpty()) {
            mWalletList.clear();
        }
        ArrayList<SharedWalletInfoEntity> walletInfoList = SharedWalletInfoDao.getInstance().getWalletInfoList();
        for (SharedWalletInfoEntity walletInfoEntity : walletInfoList) {
            try {
                mWalletList.add(walletInfoEntity.buildWalletEntity());
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        }
    }


    public void addOrUpdateWallet(SharedWalletEntity sharedWalletEntity) {
        if (mWalletList.contains(sharedWalletEntity)) {
            int index = mWalletList.indexOf(sharedWalletEntity);
            mWalletList.set(index, sharedWalletEntity);
        } else {
            mWalletList.add(sharedWalletEntity);
        }
    }

    public int getProgressByUUID(String uuid) {

        SharedWalletEntity walletEntity = new SharedWalletEntity.Builder().uuid(uuid).build();

        int index = mWalletList.indexOf(walletEntity);
        if (index != -1) {
            SharedWalletEntity sharedWalletEntity = mWalletList.get(index);
            return sharedWalletEntity.getProgress();
        }

        return 0;
    }

    public SharedWalletEntity updateWalletProgress(String uuid, int progress) {
        SharedWalletEntity walletEntity = new SharedWalletEntity.Builder().uuid(uuid).build();
        int index = mWalletList.indexOf(walletEntity);
        if (index != -1) {
            SharedWalletEntity sharedWalletEntity = mWalletList.get(index);
            sharedWalletEntity.setProgress(progress);
            mWalletList.set(index, sharedWalletEntity);
            return sharedWalletEntity;
        }
        return null;
    }

    public void updateWalletFinished(String uuid, boolean finished) {
        SharedWalletEntity walletEntity = new SharedWalletEntity.Builder().uuid(uuid).build();
        int index = mWalletList.indexOf(walletEntity);
        if (index != -1) {
            SharedWalletEntity sharedWalletEntity = mWalletList.get(index);
            sharedWalletEntity.setFinished(finished);
            mWalletList.set(index, sharedWalletEntity);
        }
    }

    public void updateWalletUnreadCount(String walletAddress, int unreadCount) {
        Flowable.fromIterable(mWalletList)
                .filter(new Predicate<SharedWalletEntity>() {
                    @Override
                    public boolean test(SharedWalletEntity sharedWalletEntity) throws Exception {
                        return !TextUtils.isEmpty(walletAddress) && walletAddress.equals(sharedWalletEntity.getAddress());
                    }
                })
                .firstElement()
                .subscribe(new Consumer<SharedWalletEntity>() {
                    @Override
                    public void accept(SharedWalletEntity sharedWalletEntity) throws Exception {
                        sharedWalletEntity.setUnread(unreadCount);
                    }
                });
    }

    public void removeWallet(SharedWalletEntity sharedWalletEntity) {
        mWalletList.remove(sharedWalletEntity);
    }

    public List<SharedWalletEntity> getWalletList() {
        return mWalletList;
    }

    public boolean isWalletExist(String contractAddress) {
        if (!mWalletList.isEmpty()) {
            for (SharedWalletEntity walletEntity : mWalletList) {
                if (walletEntity.getPrefixAddress().equals(contractAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean createWallet(String walletName, String contractAddress, String individualWalletAddress, int requiredSignNumber,
                                List<OwnerEntity> members) {
        try {
            long time = System.currentTimeMillis();
            SharedWalletEntity sharedWalletEntity = new SharedWalletEntity.Builder()
                    .uuid(UUID.randomUUID().toString())
                    .name(walletName)
                    .creatorAddress(individualWalletAddress)
                    .walletAddress(contractAddress)
                    .requiredSignNumber(requiredSignNumber)
                    .owner(members)
                    .avatar(getWalletAvatar())
                    .finished(true)
                    .createTime(time)
                    .updateTime(time)
                    .build();
            if (SharedWalletInfoDao.getInstance().insertWalletInfo(sharedWalletEntity.buildWalletInfoEntity())) {
                mWalletList.add(sharedWalletEntity);
                return true;
            }
            return false;
        } catch (Exception exp) {
            exp.printStackTrace();
            return false;
        }
    }

    public boolean saveShareWallet(SharedWalletEntity sharedWalletEntity) {

        if (SharedWalletInfoDao.getInstance().insertWalletInfo(sharedWalletEntity.buildWalletInfoEntity())) {
            if (!mWalletList.contains(sharedWalletEntity)) {
                mWalletList.add(sharedWalletEntity);
            }
            return true;
        }
        return false;
    }

    public boolean updateWalletName(String walletUuid, String newName) {
        for (SharedWalletEntity walletEntity : mWalletList) {
            if (walletUuid.equals(walletEntity.getUuid())) {
                walletEntity.setName(newName);
                break;
            }
        }
        return SharedWalletInfoDao.getInstance().updateNameWithUuid(walletUuid, newName);
    }

    public boolean updateOwner(String walletUuid, List<OwnerEntity> newAddressEntityList) {
        ArrayList<OwnerInfoEntity> addressInfoEntityArrayList = new ArrayList<>();
        for (OwnerEntity entity : newAddressEntityList) {
            OwnerInfoEntity addressInfoEntity = new OwnerInfoEntity.Builder()
//                    .uuid(entity.getUuid())
                    .uuid(UUID.randomUUID().toString())
                    .address(entity.getAddress())
                    .name(entity.getName())
                    .build();
            addressInfoEntityArrayList.add(addressInfoEntity);
        }
        if (!SharedWalletInfoDao.getInstance().updateOwnerNameWithUuid(walletUuid, addressInfoEntityArrayList)) {
            return false;
        }
        for (SharedWalletEntity walletEntity : mWalletList) {
            if (walletUuid.equals(walletEntity.getUuid())) {
                walletEntity.setOwner(newAddressEntityList);
                break;
            }
        }
        return true;
    }

    public boolean deleteWallet(String walletUuid) {
        if (!SharedWalletInfoDao.getInstance().deleteWalletInfo(walletUuid)) {
            return false;
        }
        for (SharedWalletEntity walletEntity : mWalletList) {
            try {
                if (walletUuid.equals(walletEntity.getUuid())) {
                    SharedWalletTransactionManager.getInstance().updateTransactionForRead(walletEntity);
                    mWalletList.remove(walletEntity);
                    break;
                }
            } catch (Exception exp) {

            }
        }
        return true;
    }

    public SharedWalletEntity getWalletByUuid(String uuid) {
        for (SharedWalletEntity walletEntity : mWalletList) {
            if (uuid.equals(walletEntity.getUuid())) {
                return walletEntity;
            }
        }
        return null;
    }

    public List<SharedWalletEntity> getWalletListByWalletAddress(String walletAddress) {
        List<SharedWalletEntity> sharedWalletEntityList = new ArrayList<>();
        for (SharedWalletEntity walletEntity : mWalletList) {
            List<OwnerEntity> ownerEntityList = walletEntity.getOwner();
            for (OwnerEntity ownerEntity : ownerEntityList) {
                if (ownerEntity.getPrefixAddress().contains(walletAddress)) {
                    sharedWalletEntityList.add(walletEntity);
                }
            }
        }
        return sharedWalletEntityList;
    }

    public SharedWalletEntity getWalletByContractAddress(String contractAddress) {
        for (SharedWalletEntity walletEntity : mWalletList) {
            if (!TextUtils.isEmpty(walletEntity.getPrefixAddress()) && walletEntity.getPrefixAddress().contains(contractAddress)) {
                return walletEntity;
            }
        }
        return null;
    }

    public String getWalletAvatar() {
        String[] avatarArray = App.getContext().getResources().getStringArray(R.array.wallet_avatar);
        return avatarArray[new Random().nextInt(avatarArray.length)];
    }

    public boolean walletNameExists(String walletName) {
        try {
            for (SharedWalletEntity walletEntity : mWalletList) {
                if (walletName.equals(walletEntity.getName())) {
                    return true;
                }
            }
            return false;
        } catch (Exception exp) {
            exp.printStackTrace();
            return false;
        }
    }

    private static class InstanceHolder {
        private static volatile SharedWalletManager INSTANCE = new SharedWalletManager();
    }
}
