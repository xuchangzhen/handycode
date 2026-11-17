package com.fairycrane.handycode.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Signed synthetic APK fixtures only; never installs or accesses user data. */
public final class UpdateProbe extends Instrumentation {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle(); Activity activity = null;
        try {
            Intent launch = getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); activity = startActivitySync(launch);
            Class<?> updater = getTargetContext().getClassLoader().loadClass("com.fairycrane.handycode.AppUpdater");
            Constructor<?> constructor = updater.getDeclaredConstructor(Activity.class); constructor.setAccessible(true);
            Object instance = constructor.newInstance(activity);
            Method compatible = updater.getDeclaredMethod("compatible", File.class); compatible.setAccessible(true);
            check((Boolean) compatible.invoke(instance, new File("/data/local/tmp/hc-good-update.apk")), "same signature and newer version");
            check(!(Boolean) compatible.invoke(instance, new File("/data/local/tmp/hc-wrong-update.apk")), "different signature rejected");
            check(!(Boolean) compatible.invoke(instance, new File("/data/local/tmp/hc-current.apk")), "current version rejected");
            File verified = new File(getTargetContext().getCacheDir(), "verified-update.apk");
            try (FileInputStream in = new FileInputStream("/data/local/tmp/hc-good-update.apk"); FileOutputStream out = new FileOutputStream(verified)) {
                byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n);
            }
            Uri uri = Uri.parse("content://com.fairycrane.handycode.updates/verified.apk");
            try (java.io.InputStream in = getTargetContext().getContentResolver().openInputStream(uri)) {
                check(in != null && in.read() == 'P' && in.read() == 'K', "provider returns APK bytes");
            }
            boolean denied = false;
            try { getTargetContext().getContentResolver().openFileDescriptor(uri, "w"); } catch (Exception expected) { denied = true; }
            check(denied, "provider denies write access"); verified.delete();
            result.putString("stream", "PASS: same signer upgrade, incompatible signer and version rejection, read-only update provider\n");
            finish(-1, result);
        } catch (Throwable e) { result.putString("stream", "FAIL: " + e + "; cause=" + e.getCause() + "\n"); finish(0, result); }
        finally { if (activity != null) activity.finish(); }
    }
}
