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
    private boolean keepFixture, keepSchedule, statusOnly;
    @Override public void onCreate(Bundle args) { super.onCreate(args); keepFixture = "true".equals(args.getString("keepFixture")); keepSchedule = "true".equals(args.getString("keepSchedule")); statusOnly = "true".equals(args.getString("statusOnly")); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Class<?> background = getTargetContext().getClassLoader().loadClass("com.fairycrane.handycode.BackgroundJdSync");
            if (statusOnly) {
                Method diag = method(background, "diagnostic", android.content.Context.class);
                result.putString("stream", "Background diagnostic: " + diag.invoke(null, getTargetContext()) + "\n");
                finish(-1, result); return;
            }
            Method backgroundEnabled = method(background, "enabled", android.content.Context.class);
            Method schedule = method(background, "setEnabled", android.content.Context.class, boolean.class);
            schedule.invoke(null, getTargetContext(), false);
            check(!(Boolean)backgroundEnabled.invoke(null, getTargetContext()), "background defaults off after disable");
            check((Boolean)schedule.invoke(null, getTargetContext(), true), "background scheduling succeeds");
            android.app.job.JobScheduler scheduler = (android.app.job.JobScheduler) getTargetContext().getSystemService(android.content.Context.JOB_SCHEDULER_SERVICE);
            android.app.job.JobInfo job = scheduler.getPendingJob(19082);
            check(job != null && job.isPeriodic() && job.getIntervalMillis() >= 6L * 60 * 60 * 1000, "conservative periodic schedule");
            schedule.invoke(null, getTargetContext(), false);
            check(scheduler.getPendingJob(19082) == null, "background can be turned off");
            Method startupEnabled = method(background, "startupEnabled", android.content.Context.class);
            Method startupSet = method(background, "setStartupEnabled", android.content.Context.class, boolean.class);
            Method startupSchedule = method(background, "scheduleStartupOnce", android.content.Context.class);
            startupSet.invoke(null, getTargetContext(), true);
            check((Boolean) startupEnabled.invoke(null, getTargetContext()), "startup sync enabled");
            startupSchedule.invoke(null, getTargetContext());
            check(scheduler.getPendingJob(19083) != null && !scheduler.getPendingJob(19083).isPeriodic(), "startup sync scheduled once");
            startupSet.invoke(null, getTargetContext(), false);
            check(scheduler.getPendingJob(19083) == null, "startup sync can be turned off");
            Class<?> type = getTargetContext().getClassLoader().loadClass("com.fairycrane.handycode.PackageRepository");
            Constructor<?> constructor = type.getDeclaredConstructor(android.content.Context.class); constructor.setAccessible(true);
            Method clear = method(type, "clear"), accept = method(type, "acceptJd", JSONArray.class);
            Method records = method(type, "records"), failure = method(type, "jdFailure", String.class);
            Method state = method(type, "jdState"), last = method(type, "lastJdSuccess");
            Object repo = constructor.newInstance(getTargetContext()); clear.invoke(repo);
            JSONArray fixture = new JSONArray().put(new JSONObject().put("id", "fictional-order-001")
                .put("name", "fictional-goods-001").put("status", "运输中").put("detail", "运输中").put("code", "")
                .put("tracking", "fictional-way-001").put("carrier", "fictional-carrier")
                .put("timeline", new JSONArray().put(new JSONObject().put("time", "fictional-time").put("epoch", 2000L).put("text", "fictional-event"))));
            accept.invoke(repo, fixture);
            check(((List<?>) records.invoke(repo)).size() == 1, "fixture accepted");
            byte[] bytes = Files.readAllBytes(new java.io.File(getTargetContext().getFilesDir(), "parcels-v1.enc").toPath());
            check(!new String(bytes, StandardCharsets.ISO_8859_1).contains("fictional-order-001"), "plaintext in file");
            Object reopened = constructor.newInstance(getTargetContext());
            List<?> reopenedRows = (List<?>) records.invoke(reopened);
            check(reopenedRows.size() == 1 && ((Long) last.invoke(reopened)) > 0, "encrypted data survives reopen");
            Object record = reopenedRows.get(0);
            check("fictional-way-001".equals(record.getClass().getField("tracking").get(record)), "tracking survives reopen");
            check(((List<?>) record.getClass().getField("timeline").get(record)).size() == 1, "timeline survives reopen");
            failure.invoke(reopened, "fixture network failure");
            check(((List<?>) records.invoke(reopened)).size() == 1 && ((String) state.invoke(reopened)).contains("上次数据保留"), "failure retains cached data");
            Class<?> notifications = getTargetContext().getClassLoader().loadClass("com.fairycrane.handycode.PackageNotificationManager");
            Method enable = method(notifications, "setEnabled", android.content.Context.class, boolean.class);
            enable.invoke(null, getTargetContext(), true);
            JSONArray arrived = new JSONArray().put(new JSONObject().put("id", "fictional-order-001")
                .put("name", "fictional-goods-001").put("tracking", "fictional-way-001")
                .put("status", "待取件").put("code", "FAKE-123"));
            accept.invoke(reopened, arrived);
            android.app.NotificationManager manager = (android.app.NotificationManager) getTargetContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            android.service.notification.StatusBarNotification[] posted = manager.getActiveNotifications();
            for (int i = 0; posted.length == 0 && i < 20; i++) { Thread.sleep(100); posted = manager.getActiveNotifications(); }
            check(posted.length == 1, "arrival notification posted");
            String notice = String.valueOf(posted[0].getNotification().extras.getCharSequence(android.app.Notification.EXTRA_TEXT));
            check(!notice.contains("FAKE-123") && !notice.contains("fictional-order-001"), "notification contains no sensitive fields");
            manager.cancel(1001); enable.invoke(null, getTargetContext(), false);
            CookieManager.getInstance().setCookie("https://fixture.invalid", "fixture_token=synthetic; Secure; Path=/");
            CookieManager.getInstance().flush();
            clear.invoke(reopened);
            check(((List<?>) records.invoke(reopened)).isEmpty(), "clear memory");
            check(!new java.io.File(getTargetContext().getFilesDir(), "parcels-v1.enc").exists(), "clear file");
            check(CookieManager.getInstance().getCookie("https://fixture.invalid").contains("fixture_token=synthetic"), "clear leaves web session");
            CookieManager.getInstance().setCookie("https://fixture.invalid", "fixture_token=; Max-Age=0; Secure; Path=/");
            Method station = method(type, "acceptStation", String.class, JSONObject.class);
            Method stationTime = method(type, "stationTime", String.class);
            JSONObject capture = new JSONObject().put("id", "synthetic-document:1").put("rows", new JSONArray()
                .put(new JSONObject().put("tracking", "SYNTHETIC-WAY-01").put("code", "FAKE-5").put("status", "待取件").put("carrier", "虚构物流"))
                .put(new JSONObject().put("tracking", "SYNTHETIC-WAY-02").put("code", "FAKE-5").put("status", "待取件").put("carrier", "虚构物流")));
            Method remember = method(type, "rememberStationCapture", String.class, JSONObject.class);
            remember.invoke(reopened, "拼多多", new JSONObject().put("id", "synthetic-document:0"));
            station.invoke(reopened, "拼多多", capture);
            check(((List<?>) records.invoke(reopened)).size() == 2 && (Long)stationTime.invoke(reopened, "拼多多") > 0, "station stable identities imported");
            Object stationReopened = constructor.newInstance(getTargetContext());
            check(((List<?>)records.invoke(stationReopened)).size() == 2, "station data encrypted and restored");
            clear.invoke(reopened);
            station.invoke(reopened, "拼多多", capture);
            check(((List<?>)records.invoke(reopened)).isEmpty(), "old snapshot does not repopulate cleared cache");
            capture.put("id", "synthetic-document:2"); station.invoke(reopened, "拼多多", capture);
            check(((List<?>)records.invoke(reopened)).size() == 2, "new snapshot can repopulate cache");
            clear.invoke(reopened);
            if (keepFixture) accept.invoke(reopened, fixture);
            if (keepSchedule) schedule.invoke(null, getTargetContext(), true);
            result.putString("stream", "PASS: encrypted repository, reopen, failure retention, clear, optional schedule, generic arrival notification, and synthetic cookie isolation\n");
            finish(-1, result);
        } catch (Throwable e) {
            result.putString("stream", "FAIL: " + e.toString() + "; cause=" + String.valueOf(e.getCause()) + "\n");
            finish(0, result);
        }
    }
}
