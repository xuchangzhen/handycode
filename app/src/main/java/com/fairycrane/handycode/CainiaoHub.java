package com.fairycrane.handycode;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.webkit.ValueCallback;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The parcel page is the default; the existing identity page remains available. */
final class CainiaoHub extends LinearLayout {
    private final CainiaoPage identity;
    private final CainiaoPackageView packages;
    private final LinearLayout controls;
    private final TextView parcelsTab, identityTab, refresh, app, note;
    private boolean showingIdentity, active, dark;
    CainiaoHub(Context context, CainiaoPage identity, Runnable openApp) {
        super(context); this.identity = identity; this.packages = new CainiaoPackageView(context); setOrientation(VERTICAL);
        controls = new LinearLayout(context); controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(10), dp(8), dp(10), dp(4));
        parcelsTab = button("包裹"); identityTab = button("身份码");
        parcelsTab.setContentDescription("查看菜鸟包裹"); identityTab.setContentDescription("查看菜鸟身份码");
        parcelsTab.setOnClickListener(v -> switchPage(false)); identityTab.setOnClickListener(v -> switchPage(true));
        controls.addView(parcelsTab, new LayoutParams(0, dp(40), 1));
        controls.addView(identityTab, new LayoutParams(0, dp(40), 1));
        refresh = button("刷新"); refresh.setOnClickListener(v -> packages.refresh()); controls.addView(refresh);
        app = button("菜鸟 App"); app.setOnClickListener(v -> openApp.run()); controls.addView(app);
        addView(controls);
        note = new TextView(context); note.setTextSize(11); note.setText("官方网页显示的包裹可能不完整");
        note.setPadding(dp(16), dp(3), dp(16), dp(5)); addView(note);
        FrameLayout body = new FrameLayout(context);
        body.addView(packages, new FrameLayout.LayoutParams(-1, -1));
        body.addView(identity, new FrameLayout.LayoutParams(-1, -1)); identity.setVisibility(GONE);
        addView(body, new LayoutParams(-1, 0, 1));
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView button(String title) {
        TextView t = new TextView(getContext()); t.setText(title); t.setTextSize(13); t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(40)); t.setPadding(dp(9), 0, dp(9), 0); return t;
    }
    private void styleTab(TextView view, boolean selected) {
        view.setTextColor(selected ? AppTheme.accentText(dark) : AppTheme.muted(dark));
        view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); view.setSelected(selected);
        GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(dp(12));
        shape.setColor(selected ? (dark ? 0xff382a28 : 0xffffeee8) : Color.TRANSPARENT); view.setBackground(shape);
    }
    private void updateControls() {
        controls.setBackgroundColor(AppTheme.bg(dark)); note.setTextColor(AppTheme.muted(dark));
        styleTab(parcelsTab, !showingIdentity); styleTab(identityTab, showingIdentity);
        refresh.setTextColor(AppTheme.accentText(dark)); app.setTextColor(AppTheme.accentText(dark));
        refresh.setVisibility(showingIdentity ? GONE : VISIBLE); note.setVisibility(showingIdentity ? GONE : VISIBLE);
    }
    private void switchPage(boolean value) {
        if (showingIdentity == value) return;
        showingIdentity = value; updateControls();
        View incoming = value ? identity : packages, outgoing = value ? packages : identity;
        outgoing.animate().cancel(); outgoing.setVisibility(GONE); incoming.setVisibility(VISIBLE);
        if (ValueAnimator.areAnimatorsEnabled()) { incoming.setAlpha(0f); incoming.animate().alpha(1f).setDuration(180).start(); }
        else incoming.setAlpha(1f);
        if (active) { if (value) { packages.pausePage(); identity.showPage(); } else { identity.pausePage(); packages.showPage(); } }
    }
    void setDark(boolean value) { dark = value; setBackgroundColor(AppTheme.bg(dark)); packages.setDark(dark); identity.setDark(dark); updateControls(); }
    void showPage() { active = true; if (showingIdentity) identity.showPage(); else packages.showPage(); }
    void pausePage() { active = false; identity.pausePage(); packages.pausePage(); }
    void resetPackages() { packages.reset(); }
    void destroyPage() { pausePage(); packages.reset(); identity.resetSessionView(); }
    boolean goBack() {
        if (showingIdentity) { if (identity.goBack()) return true; switchPage(false); return true; }
        return packages.goBack();
    }
    void checkVisibleScanCode(ValueCallback<String> callback) {
        if (!active || !showingIdentity) { callback.onReceiveValue("false"); return; }
        identity.checkVisibleScanCode(result -> callback.onReceiveValue(active && showingIdentity ? result : "false"));
    }
    void scrollFocusedInput() { if (showingIdentity) identity.scrollFocusedInput(); }
}
