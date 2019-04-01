package com.juzix.wallet.component.ui.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.juzix.wallet.R;
import com.juzix.wallet.component.ui.base.MVPBaseActivity;
import com.juzix.wallet.component.ui.contract.CreateIndividualWalletContract;
import com.juzix.wallet.component.ui.presenter.CreateIndividualWalletPresenter;
import com.juzix.wallet.component.widget.ShadowButton;
import com.juzix.wallet.utils.CheckStrength;

public class CreateIndividualWalletActivity extends MVPBaseActivity<CreateIndividualWalletPresenter> implements CreateIndividualWalletContract.View, View.OnClickListener, View.OnTouchListener, TextWatcher, View.OnFocusChangeListener {

    private final static String       TAG = CreateIndividualWalletActivity.class.getSimpleName();
    private              EditText     mEtName;
    private              TextView     mTvNameError;
    private              EditText     mEtPassword;
    private              EditText     mEtRepeatPassword;
    private              TextView     mTvPasswordDesc;
    private              TextView     mTvPasswordError;
    private              ShadowButton mSbtnCreate;
    private              TextView     mTvStrength;
    private              View         mVLine1;
    private              View         mVLine2;
    private              View         mVLine3;
    private              View         mVLine4;
    private              boolean      mShowPassword;
    private              boolean      mShowRepeatPassword;

    public static void actionStart(Context context) {
        context.startActivity(new Intent(context, CreateIndividualWalletActivity.class));
    }

    @Override
    protected CreateIndividualWalletPresenter createPresenter() {
        return new CreateIndividualWalletPresenter(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_individual_wallet);
        initView();
        showPassword();
        showRepeatPassword();
    }

    private void initView() {
        mEtName = findViewById(R.id.et_name);
        mTvNameError = findViewById(R.id.tv_name_error);
        mEtPassword = findViewById(R.id.et_password);
        mEtRepeatPassword = findViewById(R.id.et_repeat_password);
        mEtPassword.setOnTouchListener(this);
        mEtRepeatPassword.setOnTouchListener(this);
        mTvPasswordError = findViewById(R.id.tv_password_error);
        mTvPasswordDesc = findViewById(R.id.tv_password_desc);
        mSbtnCreate = findViewById(R.id.sbtn_create);
        mSbtnCreate.setOnClickListener(this);
        mTvStrength = findViewById(R.id.tv_strength);
        mVLine1 = findViewById(R.id.v_line1);
        mVLine2 = findViewById(R.id.v_line2);
        mVLine3 = findViewById(R.id.v_line3);
        mVLine4 = findViewById(R.id.v_line4);
        mEtName.addTextChangedListener(this);
        mEtPassword.addTextChangedListener(this);
        mEtRepeatPassword.addTextChangedListener(this);
        mEtName.setOnFocusChangeListener(this);
        mEtPassword.setOnFocusChangeListener(this);
        mEtRepeatPassword.setOnFocusChangeListener(this);
        showNameError("", false);
        showPasswordError("", false);
    }

    private void enableCreate(boolean enabled) {
        mSbtnCreate.setEnabled(enabled);
    }

    private void showPassword() {
        if (mShowPassword) {
            // 显示密码
            mEtPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_open_eyes, 0);
            mEtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mEtPassword.setSelection(mEtPassword.getText().toString().length());
            mShowPassword = !mShowPassword;
        } else {
            // 隐藏密码
            mEtPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_close_eyes, 0);
            mEtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            mEtPassword.setSelection(mEtPassword.getText().toString().length());
            mShowPassword = !mShowPassword;
        }
    }

    private void showRepeatPassword() {
        if (mShowRepeatPassword) {
            // 显示密码
            mEtRepeatPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_open_eyes, 0);
            mEtRepeatPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            mEtRepeatPassword.setSelection(mEtRepeatPassword.getText().toString().length());
            mShowRepeatPassword = !mShowRepeatPassword;
        } else {
            // 隐藏密码
            mEtRepeatPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_close_eyes, 0);
            mEtRepeatPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            mEtRepeatPassword.setSelection(mEtRepeatPassword.getText().toString().length());
            mShowRepeatPassword = !mShowRepeatPassword;
        }
    }

    @Override
    public void showNameError(String text, boolean isVisible) {
        mTvNameError.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mTvNameError.setText(text);
    }

    @Override
    public void showPasswordError(String text, boolean isVisible) {
        mTvPasswordError.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        mTvPasswordError.setText(text);
        mTvPasswordDesc.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == mEtPassword) {
            Drawable drawable = mEtPassword.getCompoundDrawables()[2];
            if (drawable == null)
                return false;
            if (event.getAction() != MotionEvent.ACTION_UP)
                return false;
            if (event.getX() > mEtPassword.getWidth() - mEtPassword.getPaddingRight() - drawable.getIntrinsicWidth()) {
                showPassword();
            }
            return false;
        } else if (v == mEtRepeatPassword) {
            Drawable drawable = mEtRepeatPassword.getCompoundDrawables()[2];
            if (drawable == null)
                return false;
            if (event.getAction() != MotionEvent.ACTION_UP)
                return false;
            if (event.getX() > mEtRepeatPassword.getWidth() - mEtRepeatPassword.getPaddingRight() - drawable.getIntrinsicWidth()) {
                showRepeatPassword();
            }
            return false;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sbtn_create:
                mPresenter.createWallet(mEtName.getText().toString().trim(),
                        mEtPassword.getText().toString().trim(),
                        mEtRepeatPassword.getText().toString().trim());
                break;
            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        String name           = mEtName.getText().toString().trim();
        String password       = mEtPassword.getText().toString().trim();
        String repeatPassword = mEtRepeatPassword.getText().toString().trim();
        if (v == mEtName && !hasFocus) {
            if (TextUtils.isEmpty(name)) {
                showNameError(string(R.string.validWalletNameEmptyTips), true);
            } else if (name.length() > 12) {
                showNameError(string(R.string.validWalletNameTips), true);
            } else if (mPresenter.isExists(name)){
                showNameError(string(R.string.wallet_name_exists), true);
            }else {
                showNameError("", false);
            }
        }
        if (v == mEtPassword && !hasFocus) {
            if (TextUtils.isEmpty(password)) {
                showPasswordError(string(R.string.validPasswordEmptyTips), true);
            } else if (password.length() < 6) {
                showPasswordError(string(R.string.validPasswordTips), true);
            } else {
                if (password.equals(repeatPassword)) {
                    showPasswordError("", false);
                }
            }
        }
        if (v == mEtRepeatPassword && !hasFocus) {
            if (TextUtils.isEmpty(repeatPassword)) {
                showPasswordError(string(R.string.validRepeatPasswordEmptyTips), true);
            } else if (!repeatPassword.equals(password)) {
                showPasswordError(string(R.string.passwordTips), true);
            } else {
                if (repeatPassword.equals(password) && password.length() >= 6) {
                    showPasswordError("", false);
                }
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        String name           = mEtName.getText().toString().trim();
        String password       = mEtPassword.getText().toString().trim();
        String repeatPassword = mEtRepeatPassword.getText().toString().trim();
        enableCreate(!TextUtils.isEmpty(name) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(repeatPassword) && password.length() >= 6 && name.length() <= 12);
        checkPwdStreng(password);
    }

    private void checkPwdStreng(String password) {
        if (TextUtils.isEmpty(password)) {
            mTvStrength.setText(R.string.strength);
            mVLine1.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
            mVLine2.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
            mVLine3.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
            mVLine4.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
            return;
        }
        switch (CheckStrength.getPasswordLevelNew(password)) {
            case EASY:
                mTvStrength.setText(R.string.weak);
                mVLine1.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine2.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
                mVLine3.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
                mVLine4.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
                break;
            case MIDIUM:
                mTvStrength.setText(R.string.so_so);
                mVLine1.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine2.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine3.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
                mVLine4.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
                break;
            case STRONG:
                mTvStrength.setText(R.string.good);
                mVLine1.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine2.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine3.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine4.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_00000000));
                break;
            case VERY_STRONG:
            case EXTREMELY_STRONG:
                mTvStrength.setText(R.string.strong);
                mVLine1.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine2.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine3.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                mVLine4.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.color_ff9000));
                break;
            default:
                break;
        }
    }
}
