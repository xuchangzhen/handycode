package com.fairycrane.handycode.tests;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.webkit.CookieManager;

/** Uses only a reserved synthetic origin to verify data survives a same-signature upgrade. */
public final class RenameProbe extends Instrumentation {
    private String mode;
    @Override public void onCreate(Bundle args) { super.onCreate(args); mode = args.getString("mode", ""); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context app = getTargetContext(); CookieManager cookies = CookieManager.getInstance();
            String origin = "https://rename-fixture.invalid/";
            if ("seed".equals(mode)) {
                if (!app.getSharedPreferences("rename-fixture", 0).edit().putString("fixture", "synthetic").commit()) throw new AssertionError();
                cookies.setCookie(origin, "synthetic_cookie=fixture; Path=/; Secure"); cookies.flush();
            } else if ("check".equals(mode)) {
                if (!"synthetic".equals(app.getSharedPreferences("rename-fixture", 0).getString("fixture", ""))) throw new AssertionError("preferences lost");
                String value = cookies.getCookie(origin);
                if (value == null || !value.contains("synthetic_cookie=fixture")) throw new AssertionError("cookie lost");
                String label = app.getApplicationInfo().loadLabel(app.getPackageManager()).toString();
                if (!"驿拾包裹".equals(label)) throw new AssertionError("application label mismatch");
                app.getSharedPreferences("rename-fixture", 0).edit().clear().commit();
                cookies.setCookie(origin, "synthetic_cookie=; Max-Age=0; Path=/; Secure"); cookies.flush();
            } else throw new IllegalArgumentException("mode");
            result.putString("stream", "PASS: rename fixture " + mode + "\n"); finish(-1, result);
        } catch (Throwable error) { result.putString("stream", "FAIL: " + error + "\n"); finish(0, result); }
    }
}
