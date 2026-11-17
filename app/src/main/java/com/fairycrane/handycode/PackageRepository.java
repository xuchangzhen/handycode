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
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Private, authenticated encryption; no credentials or raw platform responses are stored. */
final class PackageRepository {
    private static final String ALIAS = "handycode.parcels.v1";
    private final AtomicFile file;
    private PackageCollection collection = new PackageCollection();
    private long lastJdSuccess;
    private String jdState = "尚未同步；请打开京东页面";
    private boolean storageError, storageBlocked;

    PackageRepository(Context context) {
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
            JSONArray rows = root.optJSONArray("records");
            if (rows != null) for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                collection.update(new PackageRecord(row.optString("platform"), row.optString("order"), row.optString("tracking"),
                    row.optString("name"), row.optString("detail"), row.optString("code"),
                    PackageRecord.Status.valueOf(row.getString("status")), row.optLong("updated")));
            }
            if (lastJdSuccess > 0) jdState = "上次数据已恢复；本次尚未同步";
        } catch (Exception e) { collection = new PackageCollection(); lastJdSuccess = 0; storageError = true; storageBlocked = true; }
    }
    void acceptJd(JSONArray rows) {
        long now = System.currentTimeMillis();
        if (rows == null) { jdState = "解析失败，上次数据保留"; return; }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            String id = row.optString("id", "");
            String detail = row.optString("detail", "");
            String code = row.optString("code", "");
            String status = row.optString("status", "");
            collection.update(new PackageRecord("京东", id, "", row.optString("name", ""), detail, code,
                PackageRecord.infer(status + " " + detail, code), now));
        }
        lastJdSuccess = now;
        jdState = "已读取当前订单页；范围可能不完整，历史状态不会自动消失";
        save();
    }
    void jdFailure(String reason) { jdState = reason + "；上次数据保留"; }
    List<PackageRecord> records() { return collection.sorted(); }
    long lastJdSuccess() { return lastJdSuccess; }
    String jdState() { return jdState + (storageError ? "；本地加密缓存不可用" : ""); }
    void clear() {
        collection.clear(); lastJdSuccess = 0; jdState = "缓存已清除；请在京东页面点击刷新";
        file.delete(); storageError = file.getBaseFile().exists(); storageBlocked = storageError;
    }
    private void save() {
        if (storageBlocked) return; // Preserve unreadable ciphertext until the user explicitly clears it.
        FileOutputStream out = null;
        try {
            JSONObject root = new JSONObject(); root.put("jdUpdated", lastJdSuccess);
            JSONArray rows = new JSONArray();
            for (PackageRecord p : collection.sorted()) {
                JSONObject row = new JSONObject();
                row.put("platform", p.platform); row.put("order", p.orderId); row.put("tracking", p.tracking);
                row.put("name", p.name); row.put("detail", p.detail); row.put("code", p.pickupCode);
                row.put("status", p.status.name()); row.put("updated", p.updatedAt); rows.put(row);
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
