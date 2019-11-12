package com.juzix.wallet.component.ui.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.just.agentweb.WebChromeClient;
import com.just.agentweb.WebViewClient;
import com.juzix.wallet.R;
import com.juzix.wallet.app.Constants;
import com.juzix.wallet.app.CustomObserver;
import com.juzix.wallet.component.ui.base.BaseAgentWebActivity;
import com.juzix.wallet.component.ui.dialog.NodeDetailMoreDialogFragment;
import com.juzix.wallet.component.widget.ShadowButton;
import com.juzix.wallet.config.AppSettings;
import com.juzix.wallet.engine.WalletManager;
import com.juzix.wallet.entity.WebType;
import com.juzix.wallet.utils.RxUtils;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;


public class CommonHybridActivity extends BaseAgentWebActivity {

    @BindView(R.id.ck_agreement)
    CheckBox ckAgreement;
    @BindView(R.id.tv_argee_agreement)
    TextView tvArgeeAgreement;
    @BindView(R.id.sbtn_next)
    ShadowButton sbtnNext;
    @BindView(R.id.layout_service_agreement)
    ConstraintLayout layoutServiceAgreement;
    @BindView(R.id.layout_node_detail_title)
    ConstraintLayout mNodeDetailTitleLayout;
    @BindView(R.id.iv_back)
    ImageView mBackIv;
    @BindView(R.id.tv_middle_title)
    TextView mMiddleTitleTv;
    @BindView(R.id.iv_exit)
    ImageView mExitIv;
    @BindView(R.id.iv_refresh)
    ImageView mRefreshIv;
    @BindView(R.id.iv_more)
    ImageView mMoreIv;

    private String mUrl;
    private @WebType
    int mWebType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitiy_common_hybrid);
        ButterKnife.bind(this);
        init();
        buildAgentWeb();
    }

    @Override
    protected void onResume() {
        if (mWebType == WebType.WEB_TYPE_OFFICIAL_COMMUNITY || mWebType == WebType.WEB_TYPE_SUPPORT_FEEDBACK) {
            MobclickAgent.onPageStart(mWebType == WebType.WEB_TYPE_OFFICIAL_COMMUNITY ? Constants.UMPages.OFFICIAL_COMMUNITY : Constants.UMPages.SUPPORT_FEEDBACK);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mWebType == WebType.WEB_TYPE_OFFICIAL_COMMUNITY || mWebType == WebType.WEB_TYPE_SUPPORT_FEEDBACK) {
            MobclickAgent.onPageEnd(mWebType == WebType.WEB_TYPE_OFFICIAL_COMMUNITY ? Constants.UMPages.OFFICIAL_COMMUNITY : Constants.UMPages.SUPPORT_FEEDBACK);
        }
        super.onPause();
    }

    private void init() {

        mUrl = buildUrl(getIntent().getStringExtra(Constants.Extra.EXTRA_URL));
        mWebType = getIntent().getIntExtra(Constants.Extra.EXTRA_WEB_TYPE, WebType.WEB_TYPE_COMMON);
        layoutServiceAgreement = findViewById(R.id.layout_service_agreement);
        layoutServiceAgreement.setVisibility(mWebType == WebType.WEB_TYPE_AGREEMENT ? View.VISIBLE : View.GONE);

        RxView
                .clicks(sbtnNext)
                .compose(RxUtils.getClickTransformer())
                .compose(RxUtils.bindToLifecycle(this))
                .subscribe(new CustomObserver<Object>() {
                    @Override
                    public void accept(Object object) {
                        AppSettings.getInstance().setFirstEnter(false);
                        if (AppSettings.getInstance().getOperateMenuFlag()) {
                            OperateMenuActivity.actionStart(CommonHybridActivity.this);
                            CommonHybridActivity.this.finish();
                            return;
                        }
                        if (AppSettings.getInstance().getFaceTouchIdFlag() &&
                                !WalletManager.getInstance().getWalletList().isEmpty()) {
                            UnlockFigerprintActivity.actionStartMainActivity(CommonHybridActivity.this);
                            CommonHybridActivity.this.finish();
                            return;
                        }
                        MainActivity.actionStart(CommonHybridActivity.this);
                        CommonHybridActivity.this.finish();
                    }
                });

        RxCompoundButton
                .checkedChanges(ckAgreement)
                .compose(bindToLifecycle())
                .subscribe(new CustomObserver<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) {
                        sbtnNext.setEnabled(aBoolean);
                    }
                });

        RxView
                .clicks(mBackIv)
                .compose(RxUtils.getClickTransformer())
                .compose(RxUtils.bindToLifecycle(this))
                .subscribe(new CustomObserver<Object>() {

                    @Override
                    public void accept(Object o) {
                        if (mAgentWeb == null || !mAgentWeb.back()) {
                            finish();
                        }
                    }
                });
        RxView
                .clicks(mExitIv)
                .compose(RxUtils.getClickTransformer())
                .compose(RxUtils.bindToLifecycle(this))
                .subscribe(new CustomObserver<Object>() {

                    @Override
                    public void accept(Object o) {
                        finish();
                    }
                });
        RxView
                .clicks(mRefreshIv)
                .compose(RxUtils.getClickTransformer())
                .compose(RxUtils.bindToLifecycle(this))
                .subscribe(new CustomObserver<Object>() {

                    @Override
                    public void accept(Object o) {
                        mAgentWeb.getUrlLoader().reload();
                    }
                });
        RxView
                .clicks(mMoreIv)
                .compose(RxUtils.getClickTransformer())
                .compose(RxUtils.bindToLifecycle(this))
                .subscribe(new CustomObserver<Object>() {

                    @Override
                    public void accept(Object o) {
                        NodeDetailMoreDialogFragment.newInstance(mUrl).show(getSupportFragmentManager(), "showNodeDetailMoreDialog");
                    }
                });
    }

    private String buildUrl(String originalUrl) {
        if (TextUtils.isEmpty(originalUrl)) {
            return "";
        }
        Uri uri = Uri.parse(originalUrl);

        if (TextUtils.isEmpty(uri.getScheme())) {
            return "https://".concat(originalUrl);
        }

        return originalUrl;
    }

    @Override
    protected boolean immersiveBarViewEnabled() {
        return true;
    }

    @NonNull
    @Override
    protected ViewGroup getAgentWebParent() {
        return (ViewGroup) this.findViewById(R.id.layout_container);
    }

    @Override
    protected void setTitle(WebView view, String title) {
        super.setTitle(view, title);
        mMiddleTitleTv.setText(title);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAgentWeb != null && mAgentWeb.handleKeyEvent(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected int getIndicatorColor() {
        return getResources().getColor(R.color.color_0077ff);
    }

    @Override
    protected int getIndicatorHeight() {
        return 3;
    }

    @Nullable
    @Override
    protected WebViewClient getWebViewClient() {
        return webViewClient;
    }

    @Nullable
    @Override
    protected WebChromeClient getWebChromeClient() {
        return webChromeClient;
    }

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

    };

    @Nullable
    @Override
    protected String getUrl() {
        return mUrl;
    }

    private WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

        }
    };

    public static void actionStart(Context context, String url, @WebType int webType) {
        Intent intent = new Intent(context, CommonHybridActivity.class);
        intent.putExtra(Constants.Extra.EXTRA_URL, url);
        intent.putExtra(Constants.Extra.EXTRA_WEB_TYPE, webType);
        context.startActivity(intent);
    }
}