package com.fairycrane.handycode;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import com.fairycrane.handycode.logistics.PackageCollection;
import com.fairycrane.handycode.logistics.PackageRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Private, authenticated encryption; no credentials or raw platform responses are stored. */
final class PackageRepository {
    private static PackageRepository instance;
    static synchronized PackageRepository shared(Context context) {
        if (instance == null) instance = new PackageRepository(context.getApplicationContext());
        return instance;
    }
    private static final String ALIAS = "handycode.parcels.v1";
    private final AtomicFile file;
    private final Context appContext;
    private PackageCollection collection = new PackageCollection();
    private long lastJdSuccess;
    private final Map<String, Long> stationTimes = new HashMap<>();
    private final Map<String, String> lastStationCapture = new HashMap<>();
    private final Set<String> suppressedStationCaptures = new HashSet<>();
    private long cacheGeneration;
    private String jdState = "尚未同步；请打开京东页面";
    private boolean storageError, storageBlocked;

    PackageRepository(Context context) {
        appContext = context.getApplicationContext();
        file = new AtomicFile(new File(context.getFilesDir(), "parcels-v1.enc"));
        read();
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey key = (SecretKey) store.getKey(ALIAS, null);
        if (key != null) return key;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    private void read() {
        if (!file.getBaseFile().exists()) return;
        try (FileInputStream in = file.openRead()) {
            byte[] data = new byte[(int) file.getBaseFile().length()];
            if (data.length < 29 || data.length > 2_000_000) throw new IllegalStateException();
            int off = 0, n; while (off < data.length && (n = in.read(data, off, data.length - off)) > 0) off += n;
            if (off != data.length || data[0] != 1) throw new IllegalStateException();
            byte[] iv = new byte[12]; System.arraycopy(data, 1, iv, 0, 12);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            JSONObject root = new JSONObject(new String(cipher.doFinal(data, 13, data.length - 13), StandardCharsets.UTF_8));
            lastJdSuccess = root.optLong("jdUpdated");
            JSONObject times = root.optJSONObject("stationUpdated");
            if (times != null) for (String kind : new String[]{"淘宝", "拼多多"}) if (times.optLong(kind) > 0) stationTimes.put(kind, times.optLong(kind));
            JSONArray rows = root.optJSONArray("records");
            if (rows != null) for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                collection.update(new PackageRecord(row.optString("platform"), row.optString("order"), row.optString("tracking"),
                    row.optString("name"), row.optString("detail"), row.optString("code"),
                    PackageRecord.Status.valueOf(row.getString("status")), row.optLong("updated"), row.optString("carrier"), events(row.optJSONArray("timeline")), refs(row.optJSONArray("orderRefs"))));
            }
            if (lastJdSuccess > 0) jdState = "上次数据已恢复；本次尚未同步";
        } catch (Exception e) { collection = new PackageCollection(); lastJdSuccess = 0; storageError = true; storageBlocked = true; }
    }
    void acceptJd(JSONArray rows) {
        long now = System.currentTimeMillis();
        if (rows == null) { jdState = "解析失败，上次数据保留"; return; }
        List<PackageRecord> incoming = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            String id = row.optString("id", "");
            String detail = row.optString("detail", "");
            String code = row.optString("code", "");
            String status = row.optString("status", "");
            incoming.add(new PackageRecord("京东", id, row.optString("tracking", ""), row.optString("name", ""), detail, code,
                PackageRecord.infer(status + " " + detail, code), now, row.optString("carrier", ""), events(row.optJSONArray("timeline"))));
        }
        boolean newPickup = collection.hasNewPickup(incoming, lastJdSuccess > 0);
        for (PackageRecord record : incoming) collection.update(record);
        lastJdSuccess = now;
        jdState = "已读取当前订单页；范围可能不完整，历史状态不会自动消失";
        save();
        if (newPickup && !storageError) PackageNotificationManager.notifyArrival(appContext);
    }
    long generation() { return cacheGeneration; }
    void rememberStationCapture(String kind, JSONObject capture) {
        if (capture != null) lastStationCapture.put(kind, capture.optString("id", ""));
    }
    void acceptStation(String kind, JSONObject capture) {
        if ((!"淘宝".equals(kind) && !"拼多多".equals(kind)) || capture == null) return;
        String id = capture.optString("id", ""); JSONArray rows = capture.optJSONArray("rows");
        if (id.isEmpty() || rows == null || rows.length() == 0) return;
        String previous = lastStationCapture.get(kind);
        if (suppressedStationCaptures.contains(kind)) {
            if (previous == null || id.equals(previous)) { lastStationCapture.put(kind, id); return; }
            suppressedStationCaptures.remove(kind);
        }
        if (id.equals(previous)) return;
        long now = System.currentTimeMillis(); List<PackageRecord> incoming = new ArrayList<>();
        for (int i = 0; i < Math.min(rows.length(), 100); i++) {
            JSONObject row = rows.optJSONObject(i); if (row == null) continue;
            String tracking = row.optString("tracking", ""), code = row.optString("code", ""), status = row.optString("status", "");
            if (!status.matches(".*(待取件|待领取|等待取件|等待领取|驿站代签|代收点代签).*")
                    || status.matches(".*(已取件|已领取|取消|关闭|退款|拒收|无需取件).*")) continue;
            if (!tracking.matches("[A-Za-z0-9-]{8,100}") || !code.matches("[A-Za-z0-9-]{3,32}")) continue;
            String station = row.optString("station", "");
            incoming.add(new PackageRecord(kind, "", tracking, "", station.isEmpty() ? "" : "驿站：" + station,
                code, PackageRecord.Status.PICKUP, now, row.optString("carrier", ""), null));
        }
        lastStationCapture.put(kind, id);
        if (!incoming.isEmpty()) {
            boolean arrived = collection.hasNewPickup(incoming, stationTime(kind) > 0);
            for (PackageRecord record : incoming) collection.update(record);
            stationTimes.put(kind, now); save();
            if (arrived && !storageError) PackageNotificationManager.notifyArrival(appContext);
        }
    }
    long stationTime(String kind) { Long value = stationTimes.get(kind); return value == null ? 0 : value; }
    private static List<String> refs(JSONArray values) {
        List<String> result = new ArrayList<>();
        if (values != null) for (int i = 0; i < Math.min(values.length(), 200); i++) {
            String value = values.optString(i, ""); if (!value.isEmpty()) result.add(value);
        }
        return result;
    }
    private static List<PackageRecord.Event> events(JSONArray nodes) {
        List<PackageRecord.Event> result = new ArrayList<>();
        if (nodes == null) return result;
        for (int i = 0; i < Math.min(nodes.length(), 200); i++) {
            JSONObject node = nodes.optJSONObject(i);
            if (node != null && node.optLong("epoch") > 0 && !node.optString("text").isEmpty())
                result.add(new PackageRecord.Event(node.optString("time"), node.optLong("epoch"), node.optString("text")));
        }
        return result;
    }
    void jdFailure(String reason) { jdState = reason + "；上次数据保留"; }
    List<PackageRecord> records() { return collection.sorted(); }
    long lastJdSuccess() { return lastJdSuccess; }
    String jdState() { return jdState + (storageError ? "；本地加密缓存不可用" : ""); }
    void clear() {
        cacheGeneration++; suppressedStationCaptures.add("淘宝"); suppressedStationCaptures.add("拼多多");
        collection.clear(); stationTimes.clear(); lastJdSuccess = 0; jdState = "缓存已清除；请在京东页面点击刷新";
        file.delete(); storageError = file.getBaseFile().exists(); storageBlocked = storageError;
    }
    private void save() {
        if (storageBlocked) return; // Preserve unreadable ciphertext until the user explicitly clears it.
        FileOutputStream out = null;
        try {
            JSONObject root = new JSONObject(); root.put("jdUpdated", lastJdSuccess);
            JSONObject times = new JSONObject(); for (Map.Entry<String, Long> entry : stationTimes.entrySet()) times.put(entry.getKey(), entry.getValue());
            root.put("stationUpdated", times);
            JSONArray rows = new JSONArray();
            for (PackageRecord p : collection.sorted()) {
                JSONObject row = new JSONObject();
                row.put("platform", p.platform); row.put("order", p.orderId); row.put("tracking", p.tracking);
                row.put("name", p.name); row.put("detail", p.detail); row.put("code", p.pickupCode);
                row.put("status", p.status.name()); row.put("updated", p.updatedAt); row.put("carrier", p.carrier);
                JSONArray timeline = new JSONArray();
                for (PackageRecord.Event event : p.timeline) {
                    JSONObject node = new JSONObject(); node.put("time", event.time); node.put("epoch", event.epoch); node.put("text", event.text); timeline.put(node);
                }
                row.put("timeline", timeline); row.put("orderRefs", new JSONArray(p.orderRefs)); rows.put(row);
            }
            root.put("records", rows);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = cipher.getIV(); if (iv.length != 12) throw new IllegalStateException();
            byte[] ciphertext = cipher.doFinal(root.toString().getBytes(StandardCharsets.UTF_8));
            if (ciphertext.length + 13 > 2_000_000) throw new IllegalStateException();
            out = file.startWrite(); out.write(1); out.write(iv); out.write(ciphertext); file.finishWrite(out);
            storageError = false;
        } catch (Exception e) { if (out != null) file.failWrite(out); storageError = true; }
    }
}
