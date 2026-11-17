package com.fairycrane.handycode;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Optional, generic local notifications only after an observed transition. */
final class PackageNotificationManager {
    private static final String PREF = "parcel_notifications", KEY = "enabled", CHANNEL = "parcel_arrivals";
    static boolean enabled(Context context) { return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY, false); }
    static void setEnabled(Context context, boolean value) { context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply(); }
    static void notifyArrival(Context context) {
        if (!enabled(context)) return;
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null || !manager.areNotificationsEnabled()) return;
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "包裹到件提醒", NotificationManager.IMPORTANCE_DEFAULT));
        Intent intent = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("有包裹待取")
            .setContentText("官方页面显示有新的待取状态，请打开应用查看。")
            .setContentIntent(pending).setAutoCancel(true).build();
        try { manager.notify(1001, notification); } catch (SecurityException ignored) { /* Permission may be revoked. */ }
    }
}
