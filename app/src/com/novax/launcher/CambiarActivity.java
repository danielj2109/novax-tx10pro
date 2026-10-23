package com.novax.launcher;

import android.app.Activity;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Hold HOME: a bar at the bottom with your recent apps, to jump between them without going home. */
public class CambiarActivity extends Activity {

    static CambiarActivity open;
    Ui u;
    LinearLayout row;
    TextView name;
    final List<String> pkgs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        open = this;
        Ui.immersive(this);
        u = new Ui(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0xF0050608, 0xB0050608, 0x00050608}));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams lb = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lb.bottomMargin = u.px(34);
        root.addView(bar, lb);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.BOTTOM);
        head.setPadding(u.px(56), 0, u.px(56), u.px(12));
        TextView k = u.text("CAMBIAR DE APP", true, 12, Ui.GOLD);
        k.setLetterSpacing(0.3f);
        head.addView(k);
        name = u.text("", true, 18, Ui.WHITE);
        name.setGravity(Gravity.END);
        head.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(head);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        hs.setPadding(u.px(48), u.px(10), u.px(48), u.px(10));
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(row);
        bar.addView(hs, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);

        build();
        bar.setTranslationY(u.px(40)); bar.setAlpha(0f);
        bar.animate().translationY(0).alpha(1f).setDuration(200).start();
    }

    @Override protected void onDestroy() { if (open == this) open = null; super.onDestroy(); }

    void build() {
        final PackageManager pm = getPackageManager();
        String current = BotonesService.lastApp;
        // recent apps, most recent first
        List<String> order = new ArrayList<>();
        try {
            UsageStatsManager um = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            List<UsageStats> st = um.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 3L * 86400000L, now);
            if (st != null) {
                Collections.sort(st, new Comparator<UsageStats>() {
                    @Override public int compare(UsageStats a, UsageStats b) { return Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()); }
                });
                for (UsageStats s : st) if (s.getLastTimeUsed() > 0 && !order.contains(s.getPackageName())) order.add(s.getPackageName());
            }
        } catch (Exception ignored) {}
        if (current != null) { order.remove(current); order.add(0, current); }

        addCard(null, "Inicio", null, pm);
        for (String p : order) {
            if (pkgs.size() >= 9) break;
            if (p.equals(getPackageName()) || p.startsWith("com.android.tv.settings") || p.equals("com.android.systemui")) continue;
            if (launch(pm, p) == null) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(p, 0);
                addCard(p, pm.getApplicationLabel(ai).toString(), ai, pm);
            } catch (Exception ignored) {}
        }
        // start on the previous app (like alt-tab): card 2 if we came from an app, else card 1
        int start = (current != null && pkgs.size() > 2 && current.equals(pkgs.get(1))) ? 2 : Math.min(1, row.getChildCount() - 1);
        row.getChildAt(Math.max(0, start)).requestFocus();
    }

    static Intent launch(PackageManager pm, String p) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(p);
        if (i == null) i = pm.getLaunchIntentForPackage(p);
        return i;
    }

    void addCard(final String pkg, final String label, ApplicationInfo ai, final PackageManager pm) {
        pkgs.add(pkg);
        final FrameLayout card = new FrameLayout(this);
        card.setFocusable(true);
        card.setClipToOutline(true);
        int w = u.px(200), h = u.px(112);
        Drawable banner = null, icon = null;
        if (ai != null) {
            try { banner = pm.getApplicationBanner(ai); } catch (Exception ignored) {}
            try { icon = pm.getApplicationIcon(ai); } catch (Exception ignored) {}
        }
        if (banner != null) {
            card.setBackground(u.box(0xFF151824, 12, 0, 0));
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageDrawable(banner);
            card.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } else {
            card.setBackground(u.box(0xF00B0D15, 12, 0x40FFFFFF, 1));
            if (icon != null) {
                ImageView iv = new ImageView(this);
                iv.setImageDrawable(icon);
                FrameLayout.LayoutParams li = new FrameLayout.LayoutParams(u.px(40), u.px(40), Gravity.TOP | Gravity.START);
                li.leftMargin = u.px(14); li.topMargin = u.px(14);
                card.addView(iv, li);
            } else {
                TextView g = u.text("⌂", true, 40, Ui.GOLD);
                FrameLayout.LayoutParams li = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
                li.leftMargin = u.px(14); li.topMargin = u.px(6);
                card.addView(g, li);
            }
            TextView t = u.text(label, true, 15, Ui.WHITE);
            t.setSingleLine(true); t.setEllipsize(TextUtils.TruncateAt.END);
            FrameLayout.LayoutParams lt = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
            lt.leftMargin = u.px(14); lt.rightMargin = u.px(10); lt.bottomMargin = u.px(12);
            card.addView(t, lt);
        }
        if (pkg != null && pkg.equals(BotonesService.lastApp)) {
            TextView now = u.text("AHORA", true, 10, 0xFF07080D);
            now.setLetterSpacing(0.2f);
            now.setPadding(u.px(7), u.px(3), u.px(7), u.px(3));
            now.setBackground(u.box(Ui.GOLD, 5, 0, 0));
            FrameLayout.LayoutParams ln = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            ln.topMargin = u.px(8); ln.rightMargin = u.px(8);
            card.addView(now, ln);
        }
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                v.animate().scaleX(has ? 1.08f : 1f).scaleY(has ? 1.08f : 1f).setDuration(140).start();
                v.setForeground(has ? u.box(0x00000000, 12, Ui.WHITE, 3) : null);
                if (has) name.setText(label);
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    if (pkg == null) {
                        startActivity(new Intent(CambiarActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    } else {
                        Intent i = launch(pm, pkg);
                        if (i != null) startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                    }
                } catch (Exception ignored) {}
                finish();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
        lp.rightMargin = u.px(22);
        row.addView(card, lp);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int c = e.getKeyCode();
        if ((c == KeyEvent.KEYCODE_DPAD_UP || c == KeyEvent.KEYCODE_DPAD_DOWN)) return true;   // stay on the bar
        return super.dispatchKeyEvent(e);
    }

    @Override public void finish() { super.finish(); overridePendingTransition(0, android.R.anim.fade_out); }
}
