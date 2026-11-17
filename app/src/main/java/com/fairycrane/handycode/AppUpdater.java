package com.fairycrane.handycode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import com.fairycrane.handycode.update.UpdatePolicy;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;

/** User-initiated GitHub Release check; the Android installer remains the final authority. */
final class AppUpdater {
    static final int INSTALL_PERMISSION_REQUEST = 432;
    private static final long MAX_APK_BYTES = 64L * 1024 * 1024;
    private final Activity activity;
    private final AtomicBoolean busy = new AtomicBoolean();
    private boolean awaitingInstallPermission;
    AppUpdater(Activity activity) { this.activity = activity; }
    private void toast(String message) { if (!activity.isFinishing() && !activity.isDestroyed()) Toast.makeText(activity, message, Toast.LENGTH_LONG).show(); }
    private interface Work { void run() throws Exception; }
    private void run(Work work) {
        if (!busy.compareAndSet(false, true)) { toast("更新检查正在进行"); return; }
        new Thread(() -> {
            try { work.run(); }
            catch (Exception e) { activity.runOnUiThread(() -> toast("更新失败：网络、发布信息或安装包校验异常，请稍后重试")); }
            finally { busy.set(false); }
        }, "handycode-updater").start();
    }
    void check() {
        toast("正在检查 GitHub 更新…");
        run(() -> {
            byte[] bytes;
            try (InputStream in = open(UpdatePolicy.API, false)) { bytes = readLimited(in, 1024 * 1024); }
            JSONObject release = new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            String tag = release.optString("tag_name", ""), version = UpdatePolicy.version(tag);
            if (version == null || release.optBoolean("draft") || release.optBoolean("prerelease")) throw new IllegalStateException();
            String current = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
            if (!UpdatePolicy.newer(version, current)) { activity.runOnUiThread(() -> toast("当前已是最新版本")); return; }
            JSONArray assets = release.optJSONArray("assets"); JSONObject selected = null;
            String wanted = "HandyCode-" + version + "-release.apk";
            if (assets != null) for (int i=0; i<assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset != null && wanted.equals(asset.optString("name"))) { if (selected != null) throw new IllegalStateException(); selected = asset; }
            }
            if (selected == null) throw new IllegalStateException();
            String url = selected.optString("browser_download_url", ""), hash = selected.optString("digest", "");
            long size = selected.optLong("size", 0);
            if (!UpdatePolicy.assetUrl(url, tag, wanted) || !UpdatePolicy.digest(hash) || size <= 0 || size > MAX_APK_BYTES) throw new IllegalStateException();
            String expectedHash = hash.substring(7).toLowerCase(java.util.Locale.ROOT);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                new AlertDialog.Builder(activity).setTitle("发现新版本 " + version)
                    .setMessage("将从你的 GitHub Release 下载更新，并在校验后打开系统安装界面。")
                    .setNegativeButton("稍后", null).setPositiveButton("下载并安装", (d, w) -> download(url, expectedHash, size)).show();
            });
        });
    }
    private void download(String url, String expectedHash, long expectedSize) {
        toast("正在下载更新…");
        run(() -> {
            File target = UpdateFileProvider.apk(activity), temporary = new File(activity.getCacheDir(), "update-download.tmp.apk");
            // A previously verified update must not remain installable after a failed download.
            target.delete(); temporary.delete();
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); long count = 0;
            try (InputStream input = open(url, true); FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[32768]; int n;
                while ((n = input.read(buffer)) != -1) {
                    count += n; if (count > expectedSize || count > MAX_APK_BYTES) throw new IllegalStateException();
                    digest.update(buffer, 0, n); output.write(buffer, 0, n);
                }
                output.getFD().sync();
            } catch (Exception e) { temporary.delete(); throw e; }
            if (count != expectedSize || !hex(digest.digest()).equals(expectedHash) || !compatible(temporary)) {
                temporary.delete(); throw new IllegalStateException();
            }
            if (!temporary.renameTo(target)) { temporary.delete(); throw new IllegalStateException(); }
            activity.runOnUiThread(this::beginInstall);
        });
    }
    private void beginInstall() {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity).setTitle("允许安装更新")
                .setMessage("请在系统设置中允许 HandyCode 安装应用，然后返回完成更新。")
                .setNegativeButton("取消", null).setPositiveButton("打开设置", (d,w) -> {
                    awaitingInstallPermission = true;
                    try { activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName())), INSTALL_PERMISSION_REQUEST); }
                    catch (RuntimeException e) { awaitingInstallPermission = false; toast("无法打开系统安装授权设置"); }
                }).show();
            return;
        }
        install();
    }
    void permissionResult(int requestCode) {
        if (requestCode != INSTALL_PERMISSION_REQUEST || !awaitingInstallPermission) return;
        awaitingInstallPermission = false;
        if (activity.getPackageManager().canRequestPackageInstalls()) install();
        else toast("尚未授予安装权限，更新包保留在本机缓存中");
    }
    private void install() {
        File file = UpdateFileProvider.apk(activity);
        try {
            // Check again before exposing the file to the system installer.
            if (!file.isFile() || !compatible(file)) { toast("更新包已失效，请重新检查更新"); return; }
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(UpdateFileProvider.URI, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Exception e) { toast("无法打开系统安装界面，请稍后重试"); }
    }
    private boolean compatible(File file) throws Exception {
        PackageManager pm = activity.getPackageManager();
        int flag = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate = pm.getPackageArchiveInfo(file.getAbsolutePath(), flag);
        PackageInfo installed = pm.getPackageInfo(activity.getPackageName(), flag);
        if (candidate == null || !activity.getPackageName().equals(candidate.packageName)) return false;
        long next = Build.VERSION.SDK_INT >= 28 ? candidate.getLongVersionCode() : candidate.versionCode;
        long current = Build.VERSION.SDK_INT >= 28 ? installed.getLongVersionCode() : installed.versionCode;
        return next > current && Arrays.equals(signers(candidate), signers(installed));
    }
    private static String[] signers(PackageInfo info) throws Exception {
        Signature[] values = Build.VERSION.SDK_INT >= 28 && info.signingInfo != null
            ? info.signingInfo.getApkContentsSigners() : info.signatures;
        if (values == null || values.length == 0) throw new IllegalStateException();
        String[] result = new String[values.length];
        for (int i=0;i<values.length;i++) result[i] = hex(MessageDigest.getInstance("SHA-256").digest(values[i].toByteArray()));
        Arrays.sort(result); return result;
    }
    private static String hex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray(), out = new char[bytes.length * 2];
        for (int i=0;i<bytes.length;i++){out[i*2]=digits[(bytes[i]>>>4)&15];out[i*2+1]=digits[bytes[i]&15];}
        return new String(out);
    }
    private static byte[] readLimited(InputStream in, int limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int n;
        while ((n=in.read(b))!=-1) { if (out.size()+n>limit) throw new IllegalStateException(); out.write(b,0,n); }
        return out.toByteArray();
    }
    private static InputStream open(String value, boolean asset) throws Exception {
        for (int i=0;i<5;i++) {
            URL url = new URL(value);
            HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
            c.setInstanceFollowRedirects(false); c.setConnectTimeout(12000); c.setReadTimeout(20000);
            c.setRequestProperty("Accept", "application/vnd.github+json"); c.setRequestProperty("User-Agent", "HandyCode-Android");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = c.getHeaderField("Location"); c.disconnect();
                if (!asset || !UpdatePolicy.redirectUrl(location)) throw new IllegalStateException();
                value = location; continue;
            }
            if (code != 200) { c.disconnect(); throw new IllegalStateException(); }
            return c.getInputStream();
        }
        throw new IllegalStateException();
    }
}
