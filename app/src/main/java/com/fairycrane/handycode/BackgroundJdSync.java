package com.fairycrane.handycode;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;

/** Opt-in bounded JD sync using a separate official WebView and the system cookie store. */
public final class BackgroundJdSync extends JobService {
    private static final String PREF = "parcel_background", KEY = "enabled";
    private static final int JOB = 19082, STARTUP_JOB = 19083;
    private static boolean startupScheduled;
    private static BackgroundJdSync running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web;
    private JobParameters active;
    private String script;
    private boolean inFlight, startup;
    private int generation;
    private long deadline;

    private static void diagnostic(Context context, String message) {
        context.getSharedPreferences(PREF, MODE_PRIVATE).edit().putString("outcome", message).putLong("attempted", System.currentTimeMillis()).apply();
    }
    static String diagnostic(Context context) {
        return context.getSharedPreferences(PREF, MODE_PRIVATE).getString("outcome", "尚未运行");
    }
    static boolean startupEnabled(Context context) { return context.getSharedPreferences(PREF, MODE_PRIVATE).getBoolean("startup", false); }
    static void setStartupEnabled(Context context, boolean value) {
        context.getSharedPreferences(PREF, MODE_PRIVATE).edit().putBoolean("startup", value).apply();
        if (!value) {
            JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
            if (scheduler != null) scheduler.cancel(STARTUP_JOB);
            if (running != null && running.startup) running.finishRun(null, true);
        }
    }
    static void scheduleStartupOnce(Context context) {
        if (startupScheduled || !startupEnabled(context)) return;
        startupScheduled = true;
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return;
        JobInfo job = new JobInfo.Builder(STARTUP_JOB, new ComponentName(context, BackgroundJdSync.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1000).build();
        if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) diagnostic(context, "启动同步调度失败");
    }
    static boolean enabled(Context context) { return context.getSharedPreferences(PREF, MODE_PRIVATE).getBoolean(KEY, false); }
    static boolean setEnabled(Context context, boolean value) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler == null) return false;
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREF, MODE_PRIVATE);
        if (!prefs.edit().putBoolean(KEY, value).commit()) return false;
        if (value) {
            JobInfo job = new JobInfo.Builder(JOB, new ComponentName(context, BackgroundJdSync.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setRequiresBatteryNotLow(true)
                .setPeriodic(6L * 60 * 60 * 1000).build();
            if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) { prefs.edit().putBoolean(KEY, false).commit(); return false; }
        } else scheduler.cancel(JOB);
        if (!value && running != null && !running.startup) running.finishRun("后台同步已关闭", true);
        return true;
    }
    static void restoreSchedule(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (enabled(context) && scheduler != null && scheduler.getPendingJob(JOB) == null) setEnabled(context, true);
    }
    static void stopForForeground() { if (running != null && !running.startup) { diagnostic(running, "应用已回到前台，本次后台任务已停止"); running.finishRun(null, true); } }
    static void stopForCacheClear(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (scheduler != null) scheduler.cancel(STARTUP_JOB);
        if (running != null) running.finishRun(null, true);
    }
    static void stopForJdPage() { if (running != null) { diagnostic(running, "已进入京东页面，本次任务停止"); running.finishRun(null, true); } }
    private boolean blockedByForeground() {
        return MainActivity.isForegroundForParcelSync() && (!startup || MainActivity.isJdVisibleForParcelSync());
    }

    @Override public boolean onStartJob(JobParameters params) {
        boolean requestedStartup = params.getJobId() == STARTUP_JOB;
        if (requestedStartup ? !startupEnabled(this) : !enabled(this)) { diagnostic(this, "同步已关闭"); return false; }
        if (active != null || (running != null && running != this) || SiteDataClearer.isBusy()
                || (MainActivity.isForegroundForParcelSync() && (!requestedStartup || MainActivity.isJdVisibleForParcelSync()))) {
            diagnostic(this, "前台或其他任务忙，本次未运行"); return false;
        }
        startup = requestedStartup;
        diagnostic(this, "正在启动后台网页");
        try {
            if (!WebViewFeature.isFeatureSupported("DOCUMENT_START_SCRIPT")) {
                diagnostic(this, "系统网页组件不支持后台读取"); PackageRepository.shared(this).jdFailure("系统网页组件不支持后台读取"); return false;
            }
            script = WebAssets.readText(getAssets(), "jd-orders.js");
            active = params; running = this; int token = ++generation;
            web = new WebView(this);
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); settings.setSafeBrowsingEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(false); settings.setSupportMultipleWindows(false);
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
            WebViewCompat.addDocumentStartJavaScript(web, script, new HashSet<>(Arrays.asList("https://jd.com", "https://*.jd.com")));
            web.setWebChromeClient(new WebChromeClient() {
                @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            });
            web.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return !JdUrlPolicy.allowed(request.getUrl().toString()); }
                @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return !JdUrlPolicy.allowed(url); }
                @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                    if (!JdUrlPolicy.allowed(url)) { view.stopLoading(); finishRun("京东地址校验失败", true); }
                }
                @Override public void onReceivedSslError(WebView view, SslErrorHandler ssl, SslError error) { ssl.cancel(); finishRun("京东安全连接验证失败", true); }
                @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request.isForMainFrame()) finishRun("京东网络连接失败", true);
                }
                @Override public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                    finishRun("京东网页组件停止", true); return true;
                }
            });
            diagnostic(this, "正在读取京东官方订单页");
            deadline = android.os.SystemClock.elapsedRealtime() + 25000;
            web.loadUrl(JdUrlPolicy.ORDERS);
            handler.postDelayed(() -> poll(token), 1000);
            return true;
        } catch (Exception | LinkageError error) {
            finishRun("后台网页组件不可用", false); return false;
        }
    }
    private void poll(int token) {
        if (token != generation || active == null || web == null) return;
        if ((startup ? !startupEnabled(this) : !enabled(this)) || blockedByForeground() || SiteDataClearer.isBusy()) { finishRun(null, true); return; }
        if (android.os.SystemClock.elapsedRealtime() >= deadline) { finishRun("京东后台读取超时或页面未就绪", true); return; }
        final WebView expected = web;
        String url = expected.getUrl();
        if (!inFlight && JdUrlPolicy.allowed(url)) {
            inFlight = true;
            expected.evaluateJavascript(script, result -> {
                if (token != generation || active == null || web != expected) return;
                inFlight = false;
                if (blockedByForeground() || SiteDataClearer.isBusy()) { finishRun(null, true); return; }
                try {
                    JSONObject data = new JSONObject(result);
                    if (!data.optString("href").equals(expected.getUrl())) return;
                    if (data.optBoolean("login")) { finishRun("京东需要登录或会话已失效", true); return; }
                    if (!data.optString("problem").isEmpty()) { finishRun("京东官方页面繁忙或不可用", true); return; }
                    if (data.optBoolean("ready") && data.optJSONArray("orders") != null) {
                        PackageRepository.shared(this).acceptJd(data.optJSONArray("orders")); diagnostic(this, "已读取，数据范围可能不完整"); finishRun(null, true);
                    }
                } catch (Exception ignored) { /* Continue until the bounded deadline. */ }
            });
        }
        handler.postDelayed(() -> poll(token), 1000);
    }
    private void finishRun(String reason, boolean reportFinished) {
        JobParameters old = active; active = null; generation++; inFlight = false;
        if (running == this) running = null;
        if (reason != null) { diagnostic(this, reason); PackageRepository.shared(this).jdFailure(reason); }
        WebView oldWeb = web; web = null;
        if (oldWeb != null) { oldWeb.stopLoading(); oldWeb.setWebViewClient(new WebViewClient()); oldWeb.destroy(); }
        if (old != null && reportFinished) jobFinished(old, false);
    }
    @Override public boolean onStopJob(JobParameters params) { if (active != null && active.getJobId() == params.getJobId()) finishRun(null, false); return false; }
    @Override public void onDestroy() { finishRun(null, false); super.onDestroy(); }
}
