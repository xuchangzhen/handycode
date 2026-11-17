package com.fairycrane.handycode;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.fairycrane.handycode.logistics.PackageRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;

/** The parcel overview uses the app's existing palette, without touching its WebViews. */
final class PackageOverview {
    private static final int OVERVIEW = 0, SETTINGS = 1, DETAIL = 2;
    private final Context context;
    private final PackageRepository repository;
    private final boolean dark;
    private final Runnable refreshJd, toggleNotifications, toggleBackground, toggleStartup, checkUpdate, viewCainiao, openCainiao;
    private final PathInterpolator ease = new PathInterpolator(.2f, .7f, .2f, 1f);
    private Dialog overviewDialog;
    private LinearLayout panel, header, content;
    private ScrollView scroll;
    private int page = OVERVIEW, filter;
    private boolean closing, showInfo;
    private PackageRecord selected;
    private String previewKind = "", previewState = "";
    private JSONArray previewRows;

    PackageOverview(Context context, PackageRepository repository, boolean dark, Runnable refreshJd,
            Runnable toggleNotifications, Runnable toggleBackground, Runnable toggleStartup, Runnable checkUpdate,
            Runnable viewCainiao, Runnable openCainiao) {
        this.context = context; this.repository = repository; this.dark = dark;
        this.refreshJd = refreshJd; this.toggleNotifications = toggleNotifications;
        this.toggleBackground = toggleBackground; this.toggleStartup = toggleStartup; this.checkUpdate = checkUpdate;
        this.viewCainiao = viewCainiao; this.openCainiao = openCainiao;
    }
    private int dp(float value) { return (int)(value * context.getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, float size, int color, boolean bold) {
        TextView v = new TextView(context); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        v.setIncludeFontPadding(true); return v;
    }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke); return d;
    }
    private android.graphics.drawable.Drawable ripple(int color, int radius, int stroke) {
        return new RippleDrawable(ColorStateList.valueOf(dark ? 0x24ffffff : 0x18000000), shape(color, radius, stroke), shape(Color.WHITE, radius, 0));
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(context); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private int inner() { return dark ? AppTheme.elevated(true) : AppTheme.bg(false); }
    private LinearLayout card() {
        LinearLayout c = column(); c.setBackground(shape(inner(), 16, AppTheme.border(dark)));
        c.setPadding(dp(14), dp(12), dp(14), dp(12)); return c;
    }
    private void addSpace(LinearLayout target, int height) { target.addView(new View(context), new LinearLayout.LayoutParams(1, dp(height))); }
    private TextView action(String label) {
        TextView v = text(label, 13, AppTheme.accentText(dark), true); v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(40)); v.setPadding(dp(12), 0, dp(12), 0);
        v.setBackground(ripple(dark ? 0xff382a28 : 0xffffeee8, 13, 0)); return v;
    }

    void show() {
        overviewDialog = new Dialog(context) {
            @Override public void onBackPressed() { if (page != OVERVIEW) navigate(OVERVIEW); else close(null); }
            @Override public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) { close(null); return true; }
                return super.onTouchEvent(event);
            }
        };
        overviewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        overviewDialog.setCanceledOnTouchOutside(false);
        FrameLayout frame = new FrameLayout(context);
        frame.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel = column(); panel.setClipToOutline(true); panel.setElevation(dp(14));
        panel.setBackground(shape(AppTheme.surface(dark), 24, AppTheme.border(dark)));
        frame.addView(panel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        View handle = new View(context); handle.setBackground(shape(dark ? 0xff515d6c : 0xffd7dce4, 4, 0));
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(36), dp(4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL; handleLp.topMargin = dp(10); handleLp.bottomMargin = dp(3); panel.addView(handle, handleLp);
        header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(8), dp(12), dp(8)); panel.addView(header);
        scroll = new BoundedScrollView(context, (int)(context.getResources().getDisplayMetrics().heightPixels * .67f));
        scroll.setFillViewport(false); scroll.setClipToPadding(false); scroll.setVerticalScrollBarEnabled(false);
        content = column(); content.setPadding(dp(16), 0, dp(16), dp(20)); scroll.addView(content);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, -2));
        overviewDialog.setContentView(frame);
        Window window = overviewDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
            window.setDimAmount(.42f); window.setGravity(Gravity.BOTTOM);
        }
        render(false); overviewDialog.show();
        if (window != null) window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        if (ValueAnimator.areAnimatorsEnabled()) {
            panel.setAlpha(0f); panel.setTranslationY(dp(32));
            panel.animate().alpha(1f).translationY(0).setDuration(280).setInterpolator(ease).start();
        }
    }
    private void close(Runnable next) {
        if (closing || overviewDialog == null) return;
        closing = true;
        Runnable done = () -> { if (overviewDialog.isShowing()) overviewDialog.dismiss(); if (next != null) next.run(); };
        if (!ValueAnimator.areAnimatorsEnabled() || panel == null) { done.run(); return; }
        panel.animate().cancel(); panel.animate().alpha(0f).translationY(dp(24)).setDuration(180).setInterpolator(ease).withEndAction(done).start();
    }
    private void navigate(int target) {
        if (closing || page == target) return;
        page = target; render(true); scroll.scrollTo(0, 0);
    }
    private void render(boolean animate) {
        if (content == null) return;
        content.animate().cancel();
        renderHeader(); content.removeAllViews();
        if (page == SETTINGS) renderSettings(); else if (page == DETAIL && selected != null) renderDetail(); else renderOverview();
        if (animate && ValueAnimator.areAnimatorsEnabled()) {
            content.setAlpha(0f); content.setTranslationY(dp(8));
            content.animate().alpha(1f).translationY(0).setDuration(200).setInterpolator(ease).start();
        } else { content.setAlpha(1f); content.setTranslationY(0); }
    }
    private void renderHeader() {
        header.removeAllViews();
        if (page != OVERVIEW) {
            TextView back = text("‹", 30, AppTheme.ink(dark), false); back.setGravity(Gravity.CENTER);
            back.setContentDescription("返回包裹总览"); back.setBackground(ripple(Color.TRANSPARENT, 12, 0));
            back.setOnClickListener(v -> navigate(OVERVIEW)); header.addView(back, new LinearLayout.LayoutParams(dp(40), dp(44)));
        }
        LinearLayout titles = column();
        TextView title = text(page == SETTINGS ? "同步设置" : page == DETAIL ? "物流详情" : "全部包裹", 20, AppTheme.ink(dark), true);
        titles.addView(title);
        if (page == OVERVIEW) {
            TextView sub = text("数据以平台实际返回为准", 11, AppTheme.muted(dark), false); titles.addView(sub);
        }
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        if (page == OVERVIEW) {
            TextView settings = action("设置"); settings.setContentDescription("打开同步设置");
            settings.setOnClickListener(v -> navigate(SETTINGS)); header.addView(settings);
        }
        TextView dismiss = text("×", 26, AppTheme.muted(dark), false); dismiss.setGravity(Gravity.CENTER);
        dismiss.setContentDescription("关闭包裹总览"); dismiss.setBackground(ripple(Color.TRANSPARENT, 12, 0));
        dismiss.setOnClickListener(v -> close(null)); header.addView(dismiss, new LinearLayout.LayoutParams(dp(44), dp(44)));
    }
    private void renderOverview() {
        LinearLayout sync = card();
        LinearLayout heading = new LinearLayout(context); heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("同步状态", 15, AppTheme.ink(dark), true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView refresh = action("刷新京东"); refresh.setOnClickListener(v -> close(refreshJd)); heading.addView(refresh); sync.addView(heading);
        addSpace(sync, 5);
        statusLine(sync, "京东", repository.jdState(), repository.lastJdSuccess());
        statusLine(sync, "淘宝", "仅部分待取信息", repository.stationTime("淘宝"));
        statusLine(sync, "拼多多", "仅部分待取信息", repository.stationTime("拼多多"));
        statusLine(sync, "菜鸟", "独立同步未接入；网页可能仅显示部分包裹", 0);
        LinearLayout cainiaoActions = new LinearLayout(context);
        cainiaoActions.setGravity(Gravity.END);
        TextView web = action("查看菜鸟网页"); web.setOnClickListener(v -> close(viewCainiao)); cainiaoActions.addView(web);
        TextView app = action("打开菜鸟 App"); app.setOnClickListener(v -> close(openCainiao));
        LinearLayout.LayoutParams appLp = new LinearLayout.LayoutParams(-2, -2); appLp.leftMargin = dp(8); cainiaoActions.addView(app, appLp);
        sync.addView(cainiaoActions);
        TextView info = text(showInfo ? "收起同步说明  ∧" : "查看同步说明  ›", 12, AppTheme.accentText(dark), true);
        info.setPadding(0, dp(8), 0, dp(4)); info.setMinHeight(dp(36));
        info.setOnClickListener(v -> { showInfo = !showInfo; render(true); }); sync.addView(info);
        if (showInfo) {
            TextView note = text("菜鸟独立同步暂未接入。淘宝和拼多多仅能识别部分待取信息，其他状态暂未同步。拼多多小号可查询的范围以官方页面为准；切换京东账号前请按需清除旧缓存。", 12, AppTheme.muted(dark), false);
            note.setLineSpacing(dp(3), 1f); sync.addView(note);
        }
        content.addView(sync); addSpace(content, 16);
        LinearLayout filterRow = new LinearLayout(context); filterRow.setGravity(Gravity.CENTER_VERTICAL);
        String[] names = {"全部", "待取", "进行中", "已完成"};
        for (int i = 0; i < names.length; i++) {
            final int index = i; boolean active = filter == i;
            TextView tab = text(names[i], 13, active ? AppTheme.accentText(dark) : AppTheme.muted(dark), active);
            tab.setGravity(Gravity.CENTER); tab.setMinHeight(dp(40)); tab.setContentDescription(names[i] + (active ? "，已选中" : ""));
            tab.setBackground(ripple(active ? (dark ? 0xff382a28 : 0xffffeee8) : Color.TRANSPARENT, 12, 0));
            tab.setOnClickListener(v -> { if (filter != index) { filter = index; render(true); } });
            filterRow.addView(tab, new LinearLayout.LayoutParams(0, dp(40), 1));
        }
        content.addView(filterRow); addSpace(content, 10);
        int count = 0;
        for (PackageRecord record : repository.records()) {
            boolean done = record.status == PackageRecord.Status.PICKED_UP || record.status == PackageRecord.Status.CANCELLED || record.status == PackageRecord.Status.REJECTED;
            if (filter == 1 && record.status != PackageRecord.Status.PICKUP || filter == 2 && (done || record.status == PackageRecord.Status.PICKUP) || filter == 3 && !done) continue;
            count++;
            LinearLayout item = card(); item.setBackground(ripple(inner(), 16, AppTheme.border(dark)));
            LinearLayout top = new LinearLayout(context); top.setGravity(Gravity.CENTER_VERTICAL);
            top.addView(text(record.platform, 13, AppTheme.ink(dark), true), new LinearLayout.LayoutParams(0, -2, 1));
            top.addView(text(record.status.label, 12, AppTheme.accentText(dark), true)); item.addView(top);
            TextView name = text(value(record.name), 14, AppTheme.ink(dark), false); name.setPadding(0, dp(6), 0, 0); item.addView(name);
            long current = record.platform.equals("京东") ? repository.lastJdSuccess() : repository.stationTime(record.platform);
            if (record.updatedAt < current) { TextView stale = text("历史缓存 · 当前状态未确认", 11, AppTheme.muted(dark), false); stale.setPadding(0, dp(4), 0, 0); item.addView(stale); }
            item.setOnClickListener(v -> { selected = record; navigate(DETAIL); }); content.addView(item); addSpace(content, 8);
        }
        if (count == 0) {
            LinearLayout empty = card(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(20), dp(25), dp(20), dp(25));
            TextView title = text("暂未显示包裹", 15, AppTheme.ink(dark), true); title.setGravity(Gravity.CENTER); empty.addView(title);
            TextView hint = text("可能尚未同步，不代表平台没有包裹", 12, AppTheme.muted(dark), false);
            hint.setGravity(Gravity.CENTER); hint.setPadding(0, dp(6), 0, 0); empty.addView(hint); content.addView(empty);
        }
        if (!previewKind.isEmpty()) {
            addSpace(content, 12); LinearLayout preview = card(); preview.addView(text(previewKind + " · 页面信息", 13, AppTheme.ink(dark), true));
            TextView note = text(previewState, 12, AppTheme.muted(dark), false); note.setPadding(0, dp(5), 0, 0); preview.addView(note);
            if ((filter == 0 || filter == 1) && previewRows != null) for (int i = 0; i < previewRows.length(); i++) {
                JSONObject row = previewRows.optJSONObject(i); if (row == null) continue; String code = row.optString("code", ""); if (code.isEmpty()) continue;
                TextView entry = text("待取码  " + code + "    复制", 13, AppTheme.accentText(dark), true); entry.setPadding(0, dp(10), 0, 0); entry.setMinHeight(dp(40));
                entry.setOnClickListener(v -> copy("取件码", code)); preview.addView(entry);
            }
            content.addView(preview);
        }
    }
    private void statusLine(LinearLayout target, String name, String status, long time) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.TOP); row.setPadding(0, dp(5), 0, dp(5));
        TextView source = text(name, 12, AppTheme.ink(dark), true); row.addView(source, new LinearLayout.LayoutParams(dp(55), -2));
        String update = time == 0 ? "暂无记录" : "更新于 " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(time));
        LinearLayout details = column(); details.addView(text(status, 12, AppTheme.muted(dark), false)); details.addView(text(update, 11, AppTheme.muted(dark), false));
        row.addView(details, new LinearLayout.LayoutParams(0, -2, 1)); target.addView(row);
    }
    private void renderSettings() {
        TextView intro = text("同步与提醒只在获得相应权限后运行。", 12, AppTheme.muted(dark), false); intro.setPadding(dp(2), 0, 0, dp(10)); content.addView(intro);
        LinearLayout card = card();
        toggleRow(card, "到件提醒", "发现新的待取状态时提醒", PackageNotificationManager.enabled(context), toggleNotifications);
        divider(card);
        toggleRow(card, "京东后台同步", "约 6 小时一次，时间由系统安排\n最近结果：" + BackgroundJdSync.diagnostic(context), BackgroundJdSync.enabled(context), toggleBackground);
        divider(card);
        toggleRow(card, "启动时同步京东", "有网络时由系统执行", BackgroundJdSync.startupEnabled(context), toggleStartup);
        content.addView(card); addSpace(content, 12);
        LinearLayout actions = card();
        navigationRow(actions, "检查应用更新", "从 GitHub 获取新版本", false, () -> close(checkUpdate));
        divider(actions);
        navigationRow(actions, "清除物流缓存", "不会退出平台账号", true, this::confirmClear);
        content.addView(actions);
    }
    private void toggleRow(LinearLayout target, String title, String subtitle, boolean checked, Runnable run) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(8), 0, dp(8)); row.setMinimumHeight(dp(58));
        LinearLayout labels = column(); labels.addView(text(title, 14, AppTheme.ink(dark), true));
        TextView note = text(subtitle, 11, AppTheme.muted(dark), false); note.setPadding(0, dp(4), dp(8), 0); labels.addView(note);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        Switch sw = new Switch(context); sw.setContentDescription(title); sw.setChecked(checked);
        sw.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{AppTheme.ACCENT, dark ? 0xffd0d6df : 0xffffffff}));
        sw.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{dark ? 0xff8f4937 : 0xffffc5b0, dark ? 0xff596272 : 0xffb8c0cc}));
        sw.setOnCheckedChangeListener((CompoundButton button, boolean value) -> { run.run(); row.postDelayed(() -> { if (overviewDialog != null && overviewDialog.isShowing() && page == SETTINGS) render(false); }, 250); });
        row.addView(sw); row.setOnClickListener(v -> sw.performClick()); target.addView(row);
    }
    private void navigationRow(LinearLayout target, String title, String subtitle, boolean destructive, Runnable run) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); row.setMinimumHeight(dp(58)); row.setPadding(0, dp(7), 0, dp(7));
        LinearLayout labels = column(); labels.addView(text(title, 14, destructive ? AppTheme.accentText(dark) : AppTheme.ink(dark), true));
        TextView note = text(subtitle, 11, AppTheme.muted(dark), false); note.setPadding(0, dp(3), 0, 0); labels.addView(note); row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(text("›", 22, AppTheme.muted(dark), false)); row.setOnClickListener(v -> run.run()); target.addView(row);
    }
    private void divider(LinearLayout target) { View line = new View(context); line.setBackgroundColor(AppTheme.divider(dark)); target.addView(line, new LinearLayout.LayoutParams(-1, dp(1))); }
    private void renderDetail() {
        PackageRecord p = selected; LinearLayout card = card();
        detailLine(card, "状态", p.status.label, false);
        detailLine(card, "商品", value(p.name), false);
        detailLine(card, "订单号", p.orderRefs.isEmpty() ? "暂不可用" : android.text.TextUtils.join("、", p.orderRefs), false);
        detailLine(card, "快递公司", value(p.carrier), false);
        detailLine(card, "运单号", value(p.tracking), !p.tracking.isEmpty());
        detailLine(card, "取件码", value(p.pickupCode), !p.pickupCode.isEmpty());
        detailLine(card, "物流信息", value(p.detail), false);
        detailLine(card, "记录时间", DateFormat.getDateTimeInstance().format(new Date(p.updatedAt)), false);
        content.addView(card); addSpace(content, 12);
        LinearLayout timeline = card(); timeline.addView(text("物流轨迹", 15, AppTheme.ink(dark), true)); addSpace(timeline, 8);
        if (p.timeline.isEmpty()) timeline.addView(text("暂不可用", 13, AppTheme.muted(dark), false));
        else for (PackageRecord.Event event : p.timeline) {
            TextView stamp = text(value(event.time), 11, AppTheme.muted(dark), false); stamp.setPadding(0, dp(5), 0, dp(3)); timeline.addView(stamp);
            TextView message = text(event.text, 13, AppTheme.ink(dark), false); message.setLineSpacing(dp(2), 1f); timeline.addView(message); addSpace(timeline, 8);
        }
        content.addView(timeline);
    }
    private void detailLine(LinearLayout target, String name, String value, boolean copyable) {
        LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.TOP); row.setPadding(0, dp(7), 0, dp(7));
        row.addView(text(name, 12, AppTheme.muted(dark), false), new LinearLayout.LayoutParams(dp(72), -2));
        TextView detail = text(value, 13, AppTheme.ink(dark), false); detail.setTextIsSelectable(true); row.addView(detail, new LinearLayout.LayoutParams(0, -2, 1));
        if (copyable) { TextView copy = text("复制", 12, AppTheme.accentText(dark), true); copy.setPadding(dp(10), 0, 0, 0); copy.setOnClickListener(v -> copy(name, value)); row.addView(copy); }
        target.addView(row);
    }
    private void confirmClear() {
        Dialog confirm = new Dialog(context); confirm.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout box = column(); box.setPadding(dp(20), dp(18), dp(20), dp(16)); box.setBackground(shape(AppTheme.surface(dark), 22, AppTheme.border(dark)));
        box.addView(text("清除物流缓存？", 18, AppTheme.ink(dark), true)); addSpace(box, 8);
        TextView message = text("将清除本机物流缓存和历史，不会退出平台账号。", 13, AppTheme.muted(dark), false); message.setLineSpacing(dp(3), 1f); box.addView(message); addSpace(box, 18);
        LinearLayout actions = new LinearLayout(context); actions.setGravity(Gravity.END);
        TextView cancel = action("取消"); cancel.setTextColor(AppTheme.muted(dark)); cancel.setBackground(ripple(inner(), 12, 0)); cancel.setOnClickListener(v -> confirm.dismiss()); actions.addView(cancel);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.leftMargin = dp(10);
        TextView remove = action("清除"); remove.setOnClickListener(v -> { BackgroundJdSync.stopForCacheClear(context); repository.clear(); confirm.dismiss(); render(true); }); actions.addView(remove, lp); box.addView(actions);
        confirm.setContentView(box); Window w = confirm.getWindow(); if (w != null) { w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); w.setDimAmount(.25f); }
        confirm.show(); if (w != null) w.setLayout(Math.min(dp(340), context.getResources().getDisplayMetrics().widthPixels - dp(40)), -2);
    }
    private void copy(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(context, "已复制" + label, Toast.LENGTH_SHORT).show();
    }
    private static String value(String s) { return s == null || s.isEmpty() ? "暂不可用" : s; }
    void refreshSettings() { if (overviewDialog != null && overviewDialog.isShowing() && page == SETTINGS) render(false); }
    void setPickupPreview(String kind, JSONObject data) {
        if (overviewDialog == null || !overviewDialog.isShowing()) return;
        previewKind = kind;
        if (data == null || !kind.equals(data.optString("kind")) || !data.optBoolean("ready")) {
            previewState = "当前页面尚未就绪或读取失败"; previewRows = null;
        } else {
            JSONObject capture = data.optJSONObject("capture"); JSONArray shipments = capture == null ? null : capture.optJSONArray("rows");
            if (shipments != null && shipments.length() > 0) { previewRows = null; previewState = "已识别部分含运单号的待取信息，其他内容请在原页面查看"; }
            else { previewRows = data.optJSONArray("rows"); previewState = previewRows == null || previewRows.length() == 0 ? "页面没有可识别的取件码，不能据此判断没有包裹" : "当前页面待取码，仅临时展示，可能与其他平台重复"; }
        }
        if (page == OVERVIEW) render(false);
    }
    private static final class BoundedScrollView extends ScrollView {
        private final int maxHeight;
        BoundedScrollView(Context context, int maxHeight) { super(context); this.maxHeight = maxHeight; }
        @Override protected void onMeasure(int width, int height) {
            int mode = MeasureSpec.getMode(height), size = MeasureSpec.getSize(height);
            int limit = mode == MeasureSpec.UNSPECIFIED ? maxHeight : Math.min(size, maxHeight);
            super.onMeasure(width, MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST));
        }
    }
}
