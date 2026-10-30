package com.novax.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    static class App {
        String pkg, label, kind, tag;
        Intent launch;
        Drawable banner, icon;
        int rank;
    }

    static final String[][] KNOWN = {
        {"com.google.android.youtube.tv", "YouTube", "VIDEO", "Videos, música y transmisiones en vivo."},
        {"com.spotify.tv.android", "Spotify", "MÚSICA", "Tu música en la pantalla grande."},
        {"com.wbd.stream", "Max", "STREAMING", "Cine, series y deportes."},
        {"com.amazon.amazonvideo.livingroom", "Prime Video", "STREAMING", "Películas y series."},
        {"com.univision.prendetv", "ViX", "STREAMING", "Novelas, series y fútbol."},
        {"com.netflix.ninja", "Netflix", "STREAMING", "Series y películas."},
        {"com.caracol.streaming", "Caracol", "TV EN VIVO", "Canal Caracol."},
        {"com.rcntv.rcntv", "RCN", "TV EN VIVO", "Canal RCN."},
    };
    static final String[] HIDDEN = {
        "com.novax.launcher", "com.google.android.tvlauncher", "org.cyanogenmod.appdrawer",
        "com.rk_itvui.allapp", "com.softwinner.agingdragonbox", "com.softwinner.dragonbox",
        "com.netflix.mediaclient"
    };

    Ui u;
    FrameLayout root;
    ImageView wash0, wash1, hero;
    int washTop = 0;
    LinearLayout row, card;
    HorizontalScrollView hs;
    TextView tKind, tName, tTag, tClock, cKick, cHome, cMid, cAway, cWhen;
    View cardRing;
    final List<App> apps = new ArrayList<>();
    final Map<String, Bitmap> washCache = new HashMap<>();
    final Handler ui = new Handler(Looper.getMainLooper());
    String current = null;
    View lastTile = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.immersive(this);
        u = new Ui(this);
        buildUi();
        loadApps();
        buildRow();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_CHANGED);
        f.addDataScheme("package");
        registerReceiver(pkgReceiver, f);
        ui.post(clockTick);
        ui.post(infoTick);
    }

    final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { loadApps(); buildRow(); }
    };

    final Runnable clockTick = new Runnable() {
        @Override public void run() {
            Date d = new Date();
            String h = new SimpleDateFormat("h:mm", Locale.US).format(d);
            int hr = Integer.parseInt(new SimpleDateFormat("H", Locale.US).format(d));
            tClock.setText(Reloj.mal() ? "Ajustando la hora…" : h + (hr < 12 ? " a. m." : " p. m."));
            ui.postDelayed(this, 15000);
        }
    };

    // the card at the bottom right: today's date and the connection, OK opens Ajustes
    final Runnable infoTick = new Runnable() {
        @Override public void run() {
            updateCard();
            ui.postDelayed(this, 30000);
        }
    };


    @Override
    protected void onDestroy() {
        try { unregisterReceiver(pkgReceiver); } catch (Exception ignored) {}
        ui.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onBackPressed() { }

    @Override
    protected void onResume() {
        super.onResume();
        Ui.immersive(this);
        if (lastTile != null) lastTile.requestFocus();
        else if (row.getChildCount() > 0) row.getChildAt(0).requestFocus();
    }

    // ------------------------------------------------------------------ UI
    void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        // z-order (bottom to top) is fixed: wash0, wash1, hero, shades, content
        wash0 = new ImageView(this); wash1 = new ImageView(this);
        for (ImageView iv : new ImageView[]{wash0, wash1}) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setAlpha(0f);
            root.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        }
        hero = new ImageView(this);
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lhero = new FrameLayout.LayoutParams(u.px(520), u.px(292), Gravity.TOP | Gravity.END);
        lhero.topMargin = u.px(206); lhero.rightMargin = u.px(64);
        root.addView(hero, lhero);
        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xB3050608, 0x33050608, 0x40050608, 0xF0050608}));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        View side = new View(this);
        side.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xA6050608, 0x00050608}));
        root.addView(side, new FrameLayout.LayoutParams(u.px(760), -1));

        tClock = u.text("", true, 22, Ui.WHITE);
        FrameLayout.LayoutParams lpc = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        lpc.topMargin = u.px(28); lpc.rightMargin = u.px(56);
        root.addView(tClock, lpc);

        hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipChildren(false); hs.setClipToPadding(false);
        hs.setPadding(u.px(56), u.px(18), u.px(56), u.px(18));
        row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setClipChildren(false); row.setClipToPadding(false);
        hs.addView(row);
        FrameLayout.LayoutParams lph = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        lph.topMargin = u.px(58);
        root.addView(hs, lph);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        tKind = u.text("", true, 15, Ui.GOLD); tKind.setLetterSpacing(0.3f);
        tName = u.text("", true, 80, Ui.WHITE);
        tName.setSingleLine(true); tName.setEllipsize(TextUtils.TruncateAt.END);
        tTag = u.text("", false, 21, Ui.SOFT);
        info.addView(tKind);
        LinearLayout.LayoutParams lpn = new LinearLayout.LayoutParams(-2, -2);
        lpn.topMargin = u.px(12); lpn.bottomMargin = u.px(14);
        info.addView(tName, lpn);
        info.addView(tTag);
        TextView hint = u.text("OK para abrir", true, 17, 0xFF07080D);
        hint.setBackground(u.box(Ui.WHITE, 10, 0, 0));
        hint.setPadding(u.px(30), u.px(12), u.px(30), u.px(12));
        LinearLayout.LayoutParams lpb = new LinearLayout.LayoutParams(-2, -2);
        lpb.topMargin = u.px(24);
        info.addView(hint, lpb);
        FrameLayout.LayoutParams lpi = new FrameLayout.LayoutParams(u.px(700), -2, Gravity.BOTTOM | Gravity.START);
        lpi.leftMargin = u.px(56); lpi.bottomMargin = u.px(60);
        root.addView(info, lpi);

        buildCard();
        FrameLayout.LayoutParams lpm = new FrameLayout.LayoutParams(u.px(400), -2, Gravity.BOTTOM | Gravity.END);
        lpm.rightMargin = u.px(56); lpm.bottomMargin = u.px(60);
        root.addView(card, lpm);
        setContentView(root);
    }

    void buildCard() {
        card = new LinearLayout(this);
        card.setId(View.generateViewId());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setFocusable(true);
        card.setBackground(u.box(0xC00A0C14, 16, 0x8CE6C35C, 1));
        card.setPadding(u.px(20), u.px(16), u.px(20), u.px(16));
        cKick = u.text("HOY", true, 12, Ui.GOLD); cKick.setLetterSpacing(0.26f);
        card.addView(cKick);
        LinearLayout teams = new LinearLayout(this);
        teams.setOrientation(LinearLayout.HORIZONTAL);
        teams.setGravity(Gravity.CENTER_VERTICAL);
        cHome = u.text("Cargando…", true, 23, Ui.WHITE);
        cMid = u.text("", true, 15, Ui.MUTED);
        cAway = u.text("", true, 23, Ui.WHITE);
        teams.addView(cHome); teams.addView(cMid); teams.addView(cAway);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = u.px(8); lp.bottomMargin = u.px(8);
        card.addView(teams, lp);
        cWhen = u.text("", true, 15, 0xFFC9CCD6);
        card.addView(cWhen);
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                v.setBackground(u.box(has ? 0xE6141826 : 0xC00A0C14, 16, has ? Ui.WHITE : 0x8CE6C35C, has ? 3 : 1));
                v.animate().scaleX(has ? 1.05f : 1f).scaleY(has ? 1.05f : 1f).setDuration(180).start();
            }
        });
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AjustesActivity.class));
            }
        });
        card.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int code, KeyEvent e) {
                if (e.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_DPAD_UP && lastTile != null) {
                    lastTile.requestFocus(); return true;
                }
                return false;
            }
        });
    }

    void updateCard() {
        Date d = new Date();
        String day = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "CO")).format(d);
        cKick.setText("HOY");
        cHome.setText(day.substring(0, 1).toUpperCase() + day.substring(1));
        cHome.setTextSize(0, u.px(21));
        cMid.setText(""); cAway.setText("");
        String net = "Sin internet";
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.isConnected()) net = ni.getType() == android.net.ConnectivityManager.TYPE_ETHERNET ? "Conectado por cable" : "Conectado por WiFi";
        } catch (Exception ignored) {}
        cWhen.setText(net + "   ·   Ajustes →");
    }

    // ---------------------------------------------------------------- apps
    void loadApps() {
        apps.clear();
        PackageManager pm = getPackageManager();
        Map<String, App> byPkg = new HashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            List<ResolveInfo> ris = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0);
            for (ResolveInfo ri : ris) {
                String p = ri.activityInfo.packageName;
                if (byPkg.containsKey(p) || isHidden(p)) continue;
                App a = new App();
                a.pkg = p;
                a.label = String.valueOf(ri.loadLabel(pm));
                a.launch = new Intent(Intent.ACTION_MAIN).addCategory(cat)
                        .setComponent(new ComponentName(p, ri.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    a.banner = ri.activityInfo.loadBanner(pm);
                    if (a.banner == null) a.banner = pm.getApplicationBanner(p);
                } catch (Exception ignored) {}
                a.icon = ri.loadIcon(pm);
                a.kind = "APP"; a.tag = ""; a.rank = 1000;
                for (int i = 0; i < KNOWN.length; i++) {
                    if (KNOWN[i][0].equals(p)) { a.label = KNOWN[i][1]; a.kind = KNOWN[i][2]; a.tag = KNOWN[i][3]; a.rank = i; }
                }
                byPkg.put(p, a);
            }
        }
        apps.addAll(byPkg.values());
        Collections.sort(apps, new Comparator<App>() {
            @Override public int compare(App x, App y) {
                return x.rank != y.rank ? x.rank - y.rank : x.label.compareToIgnoreCase(y.label);
            }
        });
        App s = new App();
        s.pkg = "settings"; s.label = "Ajustes"; s.kind = "SISTEMA"; s.tag = "Red, pantalla, sonido y más."; s.rank = 9999;
        s.launch = new Intent(this, AjustesActivity.class);
        apps.add(s);
        App off = new App();
        off.pkg = "apagar"; off.label = "Apagar"; off.kind = "SISTEMA"; off.tag = "Despedida y reposo."; off.rank = 10000;
        off.launch = new Intent(this, ApagarActivity.class);
        apps.add(off);
    }

    boolean isHidden(String p) {
        for (String h : HIDDEN) if (h.equals(p)) return true;
        return false;
    }

    Bitmap render(Drawable d, int w, int h, int bg) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(bg);
        if (d != null) {
            int iw = d.getIntrinsicWidth() > 0 ? d.getIntrinsicWidth() : w;
            int ih = d.getIntrinsicHeight() > 0 ? d.getIntrinsicHeight() : h;
            float s = Math.max(w / (float) iw, h / (float) ih);
            int dw = Math.round(iw * s), dh = Math.round(ih * s);
            d.setBounds((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2);
            d.draw(c);
        }
        return b;
    }

    /** Big blurred colour wash taken from the app's own banner/icon. */
    Bitmap wash(App a) {
        Bitmap b = washCache.get(a.pkg);
        if (b != null) return b;
        Drawable d = a.banner != null ? a.banner : a.icon;
        if (d == null) return null;
        Bitmap tiny = render(d.getConstantState() != null ? d.getConstantState().newDrawable().mutate() : d, 16, 9, 0xFF101320);
        Bitmap mid = Bitmap.createScaledBitmap(tiny, 64, 36, true);
        b = Bitmap.createScaledBitmap(mid, 640, 360, true);
        washCache.put(a.pkg, b);
        return b;
    }

    void buildRow() {
        int keep = -1;
        if (lastTile != null) keep = row.indexOfChild(lastTile);
        row.removeAllViews();
        lastTile = null;
        int w = u.px(176), h = u.px(99), gap = u.px(20);
        for (int i = 0; i < apps.size(); i++) {
            final App a = apps.get(i);
            FrameLayout tile = new FrameLayout(this);
            tile.setFocusable(true);
            tile.setNextFocusDownId(card.getId());
            tile.setClipToOutline(true);
            if (a.banner != null) {
                tile.setBackground(u.box(0xFF151824, 12, 0, 0));
                ImageView img = new ImageView(this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setImageDrawable(a.banner);
                tile.addView(img, new FrameLayout.LayoutParams(-1, -1));
            } else {
                // phone-style apps: a NOVA X card (same look as the match card)
                tile.setBackground(u.box(0xF00B0D15, 12, 0x40FFFFFF, 1));
                if (a.icon != null) {
                    ImageView ic = new ImageView(this);
                    ic.setImageDrawable(a.icon);
                    FrameLayout.LayoutParams li = new FrameLayout.LayoutParams(u.px(38), u.px(38), Gravity.TOP | Gravity.START);
                    li.leftMargin = u.px(14); li.topMargin = u.px(13);
                    tile.addView(ic, li);
                } else if (a.pkg.equals("settings") || a.pkg.equals("apagar")) {
                    TextView g = u.text(a.pkg.equals("apagar") ? "◯" : "⚙", false, 34, Ui.GOLD);
                    FrameLayout.LayoutParams li = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
                    li.leftMargin = u.px(14); li.topMargin = u.px(8);
                    tile.addView(g, li);
                }
                TextView kind = u.text(a.kind, true, 10, Ui.GOLD);
                kind.setLetterSpacing(0.2f);
                FrameLayout.LayoutParams lk = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
                lk.rightMargin = u.px(12); lk.topMargin = u.px(14);
                tile.addView(kind, lk);
                TextView name = u.text(a.label, true, 15, Ui.WHITE);
                name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END);
                FrameLayout.LayoutParams ln = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM | Gravity.START);
                ln.leftMargin = u.px(14); ln.rightMargin = u.px(10); ln.bottomMargin = u.px(12);
                tile.addView(name, ln);
            }
            final View ring = new View(this);
            ring.setBackground(u.box(0x00000000, 12, Ui.WHITE, 3));
            ring.setAlpha(0f);
            tile.addView(ring, new FrameLayout.LayoutParams(-1, -1));
            tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override public void onFocusChange(View v, boolean has) {
                    v.animate().scaleX(has ? 1.14f : 1f).scaleY(has ? 1.14f : 1f).translationZ(has ? u.px(12) : 0)
                            .setDuration(200).setInterpolator(new DecelerateInterpolator()).start();
                    ring.animate().alpha(has ? 1f : 0f).setDuration(160).start();
                    if (has) { lastTile = v; select(a); }
                }
            });
            tile.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { open(a); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, h);
            if (i > 0) lp.leftMargin = gap;
            row.addView(tile, lp);
        }
        current = null;
        int f = keep >= 0 && keep < row.getChildCount() ? keep : 0;
        if (row.getChildCount() == 0) return;
        if (card.hasFocus()) lastTile = row.getChildAt(f);
        else row.getChildAt(f).requestFocus();
    }

    void open(App a) {
        try { startActivity(a.launch); } catch (Exception e) { tTag.setText("No se pudo abrir " + a.label + "."); }
    }

    void select(App a) {
        tKind.setText(a.kind);
        tName.setText(a.label);
        tTag.setText(a.tag);
        if (a.pkg.equals(current)) return;
        current = a.pkg;
        // colour wash: fade the hidden layer in on top of the visible one (fixed z-order, no reordering)
        // wash1 always sits above wash0: copy the current look down, then fade the new one in on top
        Bitmap wb = wash(a);
        wash1.animate().cancel();
        wash0.setImageDrawable(wash1.getDrawable());
        wash0.setAlpha(wash1.getDrawable() != null ? 0.9f : 0f);
        wash1.setImageBitmap(wb);
        wash1.setAlpha(0f);
        wash1.animate().alpha(0.9f).setDuration(600).start();
        // the app's own artwork, large and crisp (or its recommendation)
        hero.animate().cancel();
        hero.setAlpha(0f); hero.setTranslationX(u.px(30));
        hero.setPadding(0, 0, 0, 0);
        hero.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.setTag(null);
        Drawable art = a.banner;
        if (art == null && a.icon != null) {
            art = a.icon;
            hero.setPadding(u.px(170), u.px(60), u.px(170), u.px(60));
        }
        hero.setImageDrawable(art);
        if (art != null) hero.animate().alpha(1f).translationX(0).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
    }
}
