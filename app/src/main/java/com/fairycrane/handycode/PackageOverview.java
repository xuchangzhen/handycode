package com.fairycrane.handycode;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fairycrane.handycode.logistics.PackageRecord;
import java.text.DateFormat;
import java.util.Date;

/** A small opt-in view over data already observed by the official JD page. */
final class PackageOverview {
    private final Context context;
    private final PackageRepository repository;
    private final boolean dark;
    private final Runnable refreshJd, toggleNotifications, toggleBackground, toggleStartup, checkUpdate;
    private AlertDialog overviewDialog;
    private LinearLayout body;
    private int filter;
    private String previewKind = "", previewState = "";
    private JSONArray previewRows;
    PackageOverview(Context context, PackageRepository repository, boolean dark, Runnable refreshJd, Runnable toggleNotifications, Runnable toggleBackground, Runnable toggleStartup, Runnable checkUpdate) {
        this.context = context; this.repository = repository; this.dark = dark; this.refreshJd = refreshJd; this.toggleNotifications = toggleNotifications; this.toggleBackground = toggleBackground; this.toggleStartup = toggleStartup; this.checkUpdate = checkUpdate;
    }
    private TextView label(String text, int size) {
        TextView v = new TextView(context); v.setText(text); v.setTextSize(size);
        v.setTextColor(AppTheme.ink(dark)); v.setPadding(12, 12, 12, 12); return v;
    }
    void show() {
        ScrollView scroll = new ScrollView(context); body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL); body.setBackgroundColor(AppTheme.surface(dark));
        scroll.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("全部包裹（试用）").setView(scroll)
            .setPositiveButton("关闭", null).setNeutralButton("清除物流缓存", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(x ->
            new AlertDialog.Builder(context).setMessage("清除本机物流缓存和历史，不会退出平台账号。")
                .setNegativeButton("取消", null).setPositiveButton("清除", (d, which) -> { BackgroundJdSync.stopForCacheClear(context); repository.clear(); render(); }).show()));
        overviewDialog = dialog; render(); dialog.show();
    }
    void setPickupPreview(String kind, JSONObject data) {
        if (overviewDialog == null || !overviewDialog.isShowing()) return;
        previewKind = kind;
        if (data == null || !kind.equals(data.optString("kind")) || !data.optBoolean("ready")) {
            previewState = "当前页面尚未就绪或读取失败"; previewRows = null;
        } else {
            JSONObject capture = data.optJSONObject("capture");
            JSONArray shipments = capture == null ? null : capture.optJSONArray("rows");
            if (shipments != null && shipments.length() > 0) {
                previewRows = null;
                previewState = "已识别部分含运单号的待取信息，其他内容请在原页面查看";
                render(); return;
            }
            previewRows = data.optJSONArray("rows");
            previewState = previewRows == null || previewRows.length() == 0 ? "页面没有可识别的取件码，不能据此判断没有包裹" : "当前页面待取码（仅临时展示，可能与其他平台重复）";
        }
        render();
    }
    private void render() {
        body.removeAllViews();
        String time = repository.lastJdSuccess() == 0 ? "暂无" : DateFormat.getDateTimeInstance().format(new Date(repository.lastJdSuccess()));
        String tbTime = repository.stationTime("淘宝") == 0 ? "暂无" : DateFormat.getDateTimeInstance().format(new Date(repository.stationTime("淘宝")));
        String pddTime = repository.stationTime("拼多多") == 0 ? "暂无" : DateFormat.getDateTimeInstance().format(new Date(repository.stationTime("拼多多")));
        body.addView(label("京东：" + repository.jdState() + "\n上次读取：" + time
            + "\n淘宝/拼多多：仅部分待取信息可识别；其他状态暂未同步。淘宝上次识别：" + tbTime + "；拼多多：" + pddTime + "。菜鸟独立同步暂未接入。拼多多小号能否查询其他状态尚未验证。切换京东账号前请按需清除旧缓存。", 13));
        TextView refresh = label("刷新京东订单", 14);
        refresh.setTextColor(AppTheme.accentText(dark));
        refresh.setOnClickListener(v -> { overviewDialog.dismiss(); refreshJd.run(); });
        body.addView(refresh);
        TextView notifications = label("到件提醒：" + (PackageNotificationManager.enabled(context) ? "已开启" : "已关闭") + "（点击切换）", 13);
        notifications.setTextColor(AppTheme.accentText(dark));
        notifications.setOnClickListener(v -> { toggleNotifications.run(); render(); });
        body.addView(notifications);
        TextView background = label("京东后台同步：" + (BackgroundJdSync.enabled(context) ? "已开启" : "已关闭") + "（约 6 小时，系统安排）\n最近结果：" + BackgroundJdSync.diagnostic(context), 13);
        background.setTextColor(AppTheme.accentText(dark));
        background.setOnClickListener(v -> { toggleBackground.run(); render(); });
        body.addView(background);
        TextView startup = label("启动时同步京东：" + (BackgroundJdSync.startupEnabled(context) ? "已开启" : "已关闭") + "（有网络时由系统执行）", 13);
        startup.setTextColor(AppTheme.accentText(dark));
        startup.setOnClickListener(v -> { toggleStartup.run(); render(); });
        body.addView(startup);
        TextView update = label("检查应用更新", 13);
        update.setTextColor(AppTheme.accentText(dark));
        update.setOnClickListener(v -> { overviewDialog.dismiss(); checkUpdate.run(); });
        body.addView(update);
        LinearLayout tabs = new LinearLayout(context);
        String[] names = {"全部", "待取", "进行中", "已完成"};
        for (int i = 0; i < names.length; i++) {
            final int index = i; TextView tab = label((filter == i ? "• " : "") + names[i], 13);
            tab.setOnClickListener(v -> { filter = index; render(); }); tabs.addView(tab, new LinearLayout.LayoutParams(0, -2, 1));
        }
        body.addView(tabs);
        int count = 0;
        for (PackageRecord record : repository.records()) {
            boolean done = record.status == PackageRecord.Status.PICKED_UP || record.status == PackageRecord.Status.CANCELLED || record.status == PackageRecord.Status.REJECTED;
            if (filter == 1 && record.status != PackageRecord.Status.PICKUP || filter == 2 && (done || record.status == PackageRecord.Status.PICKUP)
                || filter == 3 && !done) continue;
            count++;
            long currentTime = record.platform.equals("京东") ? repository.lastJdSuccess() : repository.stationTime(record.platform);
            String stale = record.updatedAt < currentTime ? " · 历史缓存，当前状态未确认" : "";
            TextView row = label(record.platform + " · " + record.status.label + stale + "\n" + value(record.name), 15);
            row.setOnClickListener(v -> detail(record)); body.addView(row);
        }
        if (count == 0) body.addView(label("当前缓存中没有符合筛选条件的记录；这不代表平台没有包裹。", 13));
        if (!previewKind.isEmpty()) {
            body.addView(label(previewKind + "：" + previewState, 13));
            if ((filter == 0 || filter == 1) && previewRows != null) for (int i = 0; i < previewRows.length(); i++) {
                JSONObject row = previewRows.optJSONObject(i); if (row == null) continue;
                String code = row.optString("code", ""); if (code.isEmpty()) continue;
                TextView entry = label("待取码：" + code, 14);
                entry.setTextIsSelectable(true);
                entry.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("取件码", code));
                });
                body.addView(entry);
            }
        }
    }
    private static String value(String s) { return s == null || s.isEmpty() ? "暂不可用" : s; }
    private void detail(PackageRecord p) {
        String content = "状态：" + p.status.label + "\n商品：" + value(p.name) + "\n订单号：" + (p.orderRefs.isEmpty() ? "暂不可用" : android.text.TextUtils.join("、", p.orderRefs))
            + "\n快递公司：" + value(p.carrier) + "\n运单号：" + value(p.tracking) + "\n取件码：" + value(p.pickupCode)
            + "\n物流信息：" + value(p.detail) + "\n记录时间：" + DateFormat.getDateTimeInstance().format(new Date(p.updatedAt))
            + "\n物流轨迹：";
        if (p.timeline.isEmpty()) content += "暂不可用";
        else for (PackageRecord.Event event : p.timeline) content += "\n" + value(event.time) + "  " + event.text;
        AlertDialog.Builder dialog = new AlertDialog.Builder(context).setTitle("包裹详情").setMessage(content).setPositiveButton("关闭", null);
        if (!p.tracking.isEmpty()) dialog.setNegativeButton("复制运单号", (d, which) -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("运单号", p.tracking));
        });
        if (!p.pickupCode.isEmpty()) dialog.setNeutralButton("复制取件码", (d, which) -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("取件码", p.pickupCode));
        });
        dialog.show();
    }
}
