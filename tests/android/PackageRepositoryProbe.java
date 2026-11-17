package com.fairycrane.handycode.tests;

import android.app.Instrumentation;
import android.os.Bundle;
import android.webkit.CookieManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/** Synthetic data only; tests the production repository inside the target sandbox. */
public final class PackageRepositoryProbe extends Instrumentation {
    private void check(boolean v, String what) { if (!v) throw new AssertionError(what); }
    private Method method(Class<?> c, String name, Class<?>... args) throws Exception {
        Method m = c.getDeclaredMethod(name, args); m.setAccessible(true); return m;
    }
    private boolean keepFixture;
    @Override public void onCreate(Bundle args) { super.onCreate(args); keepFixture = "true".equals(args.getString("keepFixture")); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Class<?> type = getTargetContext().getClassLoader().loadClass("com.fairycrane.handycode.PackageRepository");
            Constructor<?> constructor = type.getDeclaredConstructor(android.content.Context.class); constructor.setAccessible(true);
            Method clear = method(type, "clear"), accept = method(type, "acceptJd", JSONArray.class);
            Method records = method(type, "records"), failure = method(type, "jdFailure", String.class);
            Method state = method(type, "jdState"), last = method(type, "lastJdSuccess");
            Object repo = constructor.newInstance(getTargetContext()); clear.invoke(repo);
            JSONArray fixture = new JSONArray().put(new JSONObject().put("id", "fictional-order-001")
                .put("name", "fictional-goods-001").put("status", "运输中").put("detail", "运输中").put("code", ""));
            accept.invoke(repo, fixture);
            check(((List<?>) records.invoke(repo)).size() == 1, "fixture accepted");
            byte[] bytes = Files.readAllBytes(new java.io.File(getTargetContext().getFilesDir(), "parcels-v1.enc").toPath());
            check(!new String(bytes, StandardCharsets.ISO_8859_1).contains("fictional-order-001"), "plaintext in file");
            Object reopened = constructor.newInstance(getTargetContext());
            check(((List<?>) records.invoke(reopened)).size() == 1 && ((Long) last.invoke(reopened)) > 0, "encrypted data survives reopen");
            failure.invoke(reopened, "fixture network failure");
            check(((List<?>) records.invoke(reopened)).size() == 1 && ((String) state.invoke(reopened)).contains("上次数据保留"), "failure retains cached data");
            CookieManager.getInstance().setCookie("https://fixture.invalid", "fixture_token=synthetic; Secure; Path=/");
            CookieManager.getInstance().flush();
            clear.invoke(reopened);
            check(((List<?>) records.invoke(reopened)).isEmpty(), "clear memory");
            check(!new java.io.File(getTargetContext().getFilesDir(), "parcels-v1.enc").exists(), "clear file");
            check(CookieManager.getInstance().getCookie("https://fixture.invalid").contains("fixture_token=synthetic"), "clear leaves web session");
            CookieManager.getInstance().setCookie("https://fixture.invalid", "fixture_token=; Max-Age=0; Secure; Path=/");
            if (keepFixture) accept.invoke(reopened, fixture);
            result.putString("stream", "PASS: encrypted repository, reopen, failure retention, clear, and synthetic cookie isolation\n");
            finish(-1, result);
        } catch (Throwable e) {
            result.putString("stream", "FAIL: " + e.toString() + "\n");
            finish(0, result);
        }
    }
}
