package com.fairycrane.handycode;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslError;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.util.Collections;

/** User-controlled official H5 viewing only; this is not a parcel synchronization API. */
final class CainiaoPackageConnector {
    private static final String URL = "https://page.cainiao.com/guoguo/app-myexpress-taobao/";
    private static final String APP = "com.cainiao.wireless";
    private final Activity activity;
    private final boolean dark;
    private WebView web;
    private Dialog dialog;
    private TextView notice;
    private int dp(int value) { return (int)(value * activity.getResources().getDisplayMetrics().density + .5f); }
    CainiaoPackageConnector(Activity activity, boolean dark) { this.activity = activity; this.dark = dark; }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(activity); v.setText(value); v.setTextSize(size); v.setTextColor(color); return v;
    }
    void openApp() {
        try {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(APP);
            if (intent == null || intent.getComponent() == null || !APP.equals(intent.getComponent().getPackageName())) {
                Toast.makeText(activity, "未找到菜鸟 App，请先安装官方应用", Toast.LENGTH_LONG).show(); return;
            }
            activity.startActivity(intent);
        } catch (android.content.ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, "无法打开菜鸟 App，请检查是否已安装", Toast.LENGTH_LONG).show();
        }
    }
    void showOfficialPage() {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        dialog = new Dialog(activity) {
            @Override public void onBackPressed() {
                if (web != null && web.canGoBack()) web.goBack(); else dismiss();
            }
        };
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AppTheme.bg(dark));
        LinearLayout header = new LinearLayout(activity); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(4), dp(14), dp(4)); header.setBackgroundColor(AppTheme.surface(dark));
        TextView back = text("‹ 返回", 14, AppTheme.accentText(dark)); back.setMinHeight(dp(48)); back.setGravity(Gravity.CENTER_VERTICAL);
        back.setContentDescription("返回应用"); back.setOnClickListener(v -> dialog.dismiss()); header.addView(back);
        TextView title = text("菜鸟官方包裹页", 16, AppTheme.ink(dark));
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); title.setPadding(dp(14), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView app = text("打开菜鸟 App", 13, AppTheme.accentText(dark)); app.setMinHeight(dp(48)); app.setGravity(Gravity.CENTER_VERTICAL);
        app.setOnClickListener(v -> openApp()); header.addView(app); root.addView(header);
        notice = text("官方网页可能只显示部分包裹，不会自动同步到总览。", 12, AppTheme.muted(dark));
        notice.setPadding(dp(16), dp(10), dp(16), dp(10)); root.addView(notice);
        web = new WebView(activity); web.setBackgroundColor(AppTheme.bg(dark));
        WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); settings.setSafeBrowsingEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !UrlPolicy.isAllowed(request.getUrl().toString());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (UrlPolicy.isCainiaoH5Resource(request.getUrl().toString())) return null;
                return new WebResourceResponse("text/plain", "utf-8", 403, "Forbidden", Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); notice.setText("官方网页安全连接失败，请检查网络。");
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) notice.setText("官方网页暂时无法打开，可尝试打开菜鸟 App。");
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        dialog.setContentView(root);
        dialog.setOnDismissListener(v -> {
            if (web != null) { WebView old = web; web = null; ((android.view.ViewGroup)old.getParent()).removeView(old); old.destroy(); }
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) { window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); window.setLayout(-1, -1); }
        web.loadUrl(URL);
    }
}
