package com.fairycrane.handycode;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.fairycrane.handycode.logistics.PackageRecord;
import java.text.DateFormat;
import java.util.Date;

/** A small opt-in view over data already observed by the official JD page. */
final class PackageOverview {
    private final Context context;
    private final PackageRepository repository;
    private final boolean dark;
    private LinearLayout body;
    private int filter;
    PackageOverview(Context context, PackageRepository repository, boolean dark) {
        this.context = context; this.repository = repository; this.dark = dark;
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
                .setNegativeButton("取消", null).setPositiveButton("清除", (d, which) -> { repository.clear(); render(); }).show()));
        render(); dialog.show();
    }
    private void render() {
        body.removeAllViews();
        String time = repository.lastJdSuccess() == 0 ? "暂无" : DateFormat.getDateTimeInstance().format(new Date(repository.lastJdSuccess()));
        body.addView(label("京东：" + repository.jdState() + "\n上次读取：" + time
            + "\n淘宝/天猫、菜鸟、拼多多：总览同步暂未接入；请在原页面查看。拼多多小号能否查询其他状态尚未验证。切换京东账号前请按需清除旧缓存。", 13));
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
            String stale = record.platform.equals("京东") && record.updatedAt < repository.lastJdSuccess()
                ? " · 历史缓存，当前状态未确认" : "";
            TextView row = label(record.platform + " · " + record.status.label + stale + "\n" + value(record.name), 15);
            row.setOnClickListener(v -> detail(record)); body.addView(row);
        }
        if (count == 0) body.addView(label("当前缓存中没有符合筛选条件的记录；这不代表平台没有包裹。", 13));
    }
    private static String value(String s) { return s == null || s.isEmpty() ? "暂不可用" : s; }
    private void detail(PackageRecord p) {
        String content = "状态：" + p.status.label + "\n商品：" + value(p.name) + "\n订单号：" + value(p.orderId)
            + "\n快递公司：暂不可用\n运单号：" + value(p.tracking) + "\n取件码：" + value(p.pickupCode)
            + "\n物流信息：" + value(p.detail) + "\n记录时间：" + DateFormat.getDateTimeInstance().format(new Date(p.updatedAt))
            + "\n完整轨迹：暂不可用";
        AlertDialog.Builder dialog = new AlertDialog.Builder(context).setTitle("包裹详情").setMessage(content).setPositiveButton("关闭", null);
        if (!p.pickupCode.isEmpty()) dialog.setNeutralButton("复制取件码", (d, which) -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("取件码", p.pickupCode));
        });
        dialog.show();
    }
}
