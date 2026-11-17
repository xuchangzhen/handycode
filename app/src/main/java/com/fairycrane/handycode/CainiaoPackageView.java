package com.fairycrane.handycode;

import android.content.Context;
import android.net.http.SslError;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.Collections;

/** Displays the user's official page; it does not extract or synchronize its data. */
final class CainiaoPackageView extends FrameLayout {
    private static final String ENTRY = "https://page.cainiao.com/guoguo/app-myexpress-taobao/";
    private WebView web;
    private final ProgressBar progress;
    private final TextView error;
    private final String themeScript;
    private boolean dark, active, failed;

    CainiaoPackageView(Context context) {
        super(context);
        String script;
        try { script = WebAssets.readText(context.getAssets(), "cainiao-packages-theme.js"); }
        catch (java.io.IOException e) { script = ""; }
        themeScript = script;
        progress = new ProgressBar(context);
        addView(progress, new LayoutParams(dp(32), dp(32), Gravity.CENTER));
        error = new TextView(context); error.setGravity(Gravity.CENTER); error.setTextSize(14);
        error.setPadding(dp(24), dp(16), dp(24), dp(16));
        error.setText("官方网页暂时无法打开，请刷新或打开菜鸟 App");
        error.setVisibility(GONE); addView(error, new LayoutParams(-1, -1));
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    void setDark(boolean value) {
        dark = value; setBackgroundColor(AppTheme.bg(dark)); error.setTextColor(AppTheme.muted(dark));
        if (web != null) { web.setBackgroundColor(AppTheme.bg(dark)); applyTheme(web); }
    }
    private void applyTheme(WebView view) {
        if (view == null || themeScript.isEmpty() || !UrlPolicy.isCainiaoH5Entry(view.getUrl())) return;
        view.evaluateJavascript(themeScript + "\nwindow.HandyCainiaoPackagesTheme&&window.HandyCainiaoPackagesTheme.update(" + dark + ")", null);
    }
    void showPage() {
        active = true;
        if (web == null) create();
        if (web != null) web.onResume();
    }
    private void create() {
        final WebView view = new WebView(getContext()); web = view;
        WebSettings settings = view.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); settings.setSafeBrowsingEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
        view.setBackgroundColor(AppTheme.bg(dark));
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return !UrlPolicy.isAllowed(request.getUrl().toString());
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request) {
                if (UrlPolicy.isCainiaoH5Resource(request.getUrl().toString())) return null;
                return new WebResourceResponse("text/plain", "utf-8", 403, "Forbidden", Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                if (web != v) return; failed = false; progress.setVisibility(VISIBLE); error.setVisibility(GONE);
            }
            @Override public void onPageCommitVisible(WebView v, String url) { if (web == v) applyTheme(v); }
            @Override public void onPageFinished(WebView v, String url) {
                if (web != v || !url.equals(v.getUrl())) return;
                applyTheme(v); progress.setVisibility(GONE); if (!failed) error.setVisibility(GONE);
            }
            @Override public void onReceivedSslError(WebView v, SslErrorHandler handler, SslError ssl) { handler.cancel(); fail(v); }
            @Override public void onReceivedError(WebView v, WebResourceRequest request, android.webkit.WebResourceError e) {
                if (request.isForMainFrame()) fail(v);
            }
            @Override public void onReceivedHttpError(WebView v, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame()) fail(v);
            }
            @Override public boolean onRenderProcessGone(WebView v, android.webkit.RenderProcessGoneDetail detail) {
                if (web == v) { reset(); failed = true; progress.setVisibility(GONE); error.setVisibility(VISIBLE); }
                return true;
            }
        });
        addView(view, 0, new LayoutParams(-1, -1));
        progress.setVisibility(VISIBLE); error.setVisibility(GONE); view.loadUrl(ENTRY);
    }
    private void fail(WebView view) {
        if (web != view) return; failed = true; progress.setVisibility(GONE); error.setVisibility(VISIBLE);
    }
    void refresh() { if (web == null) { if (active) create(); } else web.loadUrl(ENTRY); }
    boolean goBack() { if (web != null && web.canGoBack()) { web.goBack(); return true; } return false; }
    void pausePage() { active = false; if (web != null) web.onPause(); }
    void reset() {
        active = false;
        if (web != null) { WebView old = web; web = null; old.stopLoading(); old.setWebViewClient(new WebViewClient()); removeView(old); old.destroy(); }
        progress.setVisibility(GONE); error.setVisibility(GONE);
    }
}
