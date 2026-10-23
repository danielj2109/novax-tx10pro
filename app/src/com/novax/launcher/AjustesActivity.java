package com.novax.launcher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** NOVA X settings: the everyday options in the NOVA X style, with a door to the full Android settings. */
public class AjustesActivity extends Activity {

    static final String[][] MENU = {
            {"red", "Red", "WiFi e internet"},
            {"sonido", "Sonido", "Volumen y estilo"},
            {"pantalla", "Pantalla", "Imagen y reposo"},
            {"sistema", "Fecha e idioma", "Hora, formato e idioma"},
            {"espacio", "Almacenamiento", "Espacio y memoria"},
            {"apps", "Aplicaciones", "Instaladas en la caja"},
            {"botones", "Control remoto", "Botones de NOVA X"},
            {"info", "Información", "Sobre esta caja"},
    };

    Ui u;
    LinearLayout menu, panel;
    ScrollView scroll;
    TextView clock;
    final Handler ui = new Handler(Looper.getMainLooper());
    final List<View> menuItems = new ArrayList<>();
    View currentItem;
    String current = "";
    String parent = null;
    boolean inside = false;
    static AjustesActivity open;        // true while the cursor belongs to the right-hand panel          // set while a sub-page (WiFi key, app detail) is open

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        open = this;
        Ui.immersive(this);
        u = new Ui(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF10131F, 0xFF07080D, 0xFF050608}));

        TextView kicker = u.text("AJUSTES", true, 14, Ui.GOLD);
        kicker.setLetterSpacing(0.32f);
        FrameLayout.LayoutParams lk = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        lk.leftMargin = u.px(56); lk.topMargin = u.px(34);
        root.addView(kicker, lk);
        clock = u.text("", true, 18, Ui.WHITE);
        FrameLayout.LayoutParams lc = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        lc.rightMargin = u.px(56); lc.topMargin = u.px(30);
        root.addView(clock, lc);

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams lm = new FrameLayout.LayoutParams(u.px(330), -1, Gravity.TOP | Gravity.START);
        lm.leftMargin = u.px(44); lm.topMargin = u.px(80); lm.bottomMargin = u.px(30);
        root.addView(menu, lm);
        for (String[] m : MENU) menu.addView(menuItem(m[0], m[1], m[2]));

        View sep = new View(this);
        sep.setBackgroundColor(0x14FFFFFF);
        FrameLayout.LayoutParams ls = new FrameLayout.LayoutParams(Math.max(1, u.px(1)), -1, Gravity.TOP | Gravity.START);
        ls.leftMargin = u.px(396); ls.topMargin = u.px(86); ls.bottomMargin = u.px(40);
        root.addView(sep, ls);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 0, 0, u.px(40));
        scroll.addView(panel, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(u.px(800), -1, Gravity.TOP | Gravity.START);
        lp.leftMargin = u.px(428); lp.topMargin = u.px(80);
        root.addView(scroll, lp);

        TextView hint = u.text("▲▼ elegir   ·   ▶ entrar   ·   Atrás para salir", false, 14, Ui.MUTED);
        FrameLayout.LayoutParams lh = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        lh.rightMargin = u.px(56); lh.bottomMargin = u.px(22);
        root.addView(hint, lh);

        setContentView(root);
        ensureLocation(this);
        tick.run();
        route(getIntent());
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); if (i != null && i.getAction() != null && !Intent.ACTION_MAIN.equals(i.getAction())) route(i); }

    /** Pick the section from the intent another app sent ("open WiFi settings", "app details"…). */
    void route(Intent in) {
        String a = in == null || in.getAction() == null ? "" : in.getAction();
        String id = "red";
        if (a.contains("SOUND")) id = "sonido";
        else if (a.contains("DISPLAY") || a.contains("DREAM")) id = "pantalla";
        else if (a.contains("DATE") || a.contains("LOCALE") || a.contains("INPUT_METHOD")) id = "sistema";
        else if (a.contains("STORAGE") || a.contains("MEMORY")) id = "espacio";
        else if (a.contains("APPLICATION") || a.contains("APP_") || a.contains("MANAGE_")) id = "apps";
        else if (a.contains("DEVICE_INFO") || a.contains("DEVELOPMENT")) id = "info";
        for (int i = 0; i < MENU.length; i++) if (MENU[i][0].equals(id)) { menuItems.get(i).requestFocus(); break; }
        Uri d = in == null ? null : in.getData();
        if (a.equals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) && d != null && "package".equals(d.getScheme())) {
            String pkg = d.getSchemeSpecificPart();
            String label = pkg;
            try { label = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg, 0)).toString(); } catch (Exception ignored) {}
            appPage(pkg, label);
        }
    }

    final Runnable tick = new Runnable() {
        @Override public void run() {
            clock.setText(new SimpleDateFormat("h:mm a", new Locale("es", "CO")).format(new Date()));
            ui.postDelayed(this, 15000);
        }
    };

    @Override protected void onResume() { super.onResume(); Ui.immersive(this); if (!current.isEmpty() && parent == null) show(current); }
    @Override protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        if (open == this) open = null;
        if (scanRx != null) try { unregisterReceiver(scanRx); } catch (Exception ignored) {}
        super.onDestroy();
    }

    // ------------------------------------------------------------------ menu
    View menuItem(final String id, String label, String sub) {
        final LinearLayout it = new LinearLayout(this);
        it.setOrientation(LinearLayout.VERTICAL);
        it.setFocusable(true);
        it.setPadding(u.px(18), u.px(10), u.px(18), u.px(10));
        final TextView l = u.text(label, true, 19, Ui.WHITE);
        final TextView s = u.text(sub, false, 13, Ui.MUTED);
        it.addView(l);
        LinearLayout.LayoutParams lps = new LinearLayout.LayoutParams(-2, -2); lps.topMargin = u.px(3);
        it.addView(s, lps);
        it.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                if (has && inside) {               // the panel lost focus by itself; give it back
                    ui.post(new Runnable() { public void run() { View c = firstFocusable(panel); if (c != null && inside) c.requestFocus(); } });
                    paintMenu();
                    return;
                }
                if (has && currentItem != v) { currentItem = v; show(id); }
                paintMenu();
            }
        });
        it.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { enterPanel(); } });
        it.setTag(new TextView[]{l, s});
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = u.px(4);
        it.setLayoutParams(lp);
        menuItems.add(it);
        return it;
    }

    void paintMenu() {
        for (View v : menuItems) {
            TextView[] t = (TextView[]) v.getTag();
            boolean focused = v.hasFocus();
            boolean selected = v == currentItem;
            v.setBackground(u.box(focused ? Ui.WHITE : selected ? 0x1FE6C35C : 0x00000000, 14, selected && !focused ? 0x99E6C35C : 0, selected && !focused ? 1.5f : 0));
            t[0].setTextColor(focused ? 0xFF07080D : Ui.WHITE);
            t[1].setTextColor(focused ? 0xFF4A4E5C : Ui.MUTED);
        }
    }

    void enterPanel() {
        View c = firstFocusable(panel);
        if (c == null) return;          // nothing to select on this page: stay in the menu
        inside = true;
        c.requestFocus();
        scroll.smoothScrollTo(0, 0);
    }

    static boolean isIn(View v, View root) {
        while (v != null) {
            if (v == root) return true;
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return false;
    }

    View firstFocusable(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c.getVisibility() != View.VISIBLE) continue;
            if (c.isFocusable()) return c;
            if (c instanceof ViewGroup) { View x = firstFocusable((ViewGroup) c); if (x != null) return x; }
        }
        return null;
    }

    void toMenu() {
        inside = false;
        if (currentItem != null) currentItem.requestFocus();
    }

    boolean inPanel() {
        View f = getCurrentFocus();
        while (f != null) {
            if (f == panel) return true;
            f = f.getParent() instanceof View ? (View) f.getParent() : null;
        }
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if ((code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN) && e.getAction() == KeyEvent.ACTION_DOWN) {
            View f = getCurrentFocus();
            ViewGroup area = inside ? panel : menu;
            if (f == null || !isIn(f, area)) {         // focus got lost (list refreshed): put it back
                View c = firstFocusable(area);
                if (c != null) c.requestFocus();
                return true;
            }
            {
                View next = android.view.FocusFinder.getInstance().findNextFocus(area, f,
                        code == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN);
                if (next != null) {
                    next.requestFocus();
                    if (area == panel) {   // keep the focused row visible
                        android.graphics.Rect r = new android.graphics.Rect();
                        next.getDrawingRect(r);
                        panel.offsetDescendantRectToMyCoords(next, r);
                        int top = r.top - u.px(60), bottom = r.bottom + u.px(60);
                        if (top < scroll.getScrollY()) scroll.smoothScrollTo(0, Math.max(0, top));
                        else if (bottom > scroll.getScrollY() + scroll.getHeight()) scroll.smoothScrollTo(0, bottom - scroll.getHeight());
                    }
                }
                if (next == null && area == panel && code == KeyEvent.KEYCODE_DPAD_UP) scroll.smoothScrollTo(0, 0);
            }
            return true;   // never jump to the other column
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_DPAD_RIGHT && !inside) { enterPanel(); return true; }
        if (code == KeyEvent.KEYCODE_BACK && parent != null) { String pp = parent; parent = null; show(pp); enterPanel(); return true; }
        if (code == KeyEvent.KEYCODE_BACK && inside) { toMenu(); return true; }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT && inside && !(getCurrentFocus() instanceof android.widget.EditText) && !(getCurrentFocus() != null && "slider".equals(getCurrentFocus().getTag()))) {
            if (parent != null) { String pp = parent; parent = null; show(pp); enterPanel(); return true; }
            toMenu();
            return true;
        }
        return super.onKeyDown(code, e);
    }

    // ------------------------------------------------------------------ panel helpers
    void title(String t, String sub) {
        panel.addView(u.text(t, true, 30, Ui.WHITE));
        if (sub != null) {
            TextView s = u.text(sub, false, 16, Ui.MUTED);
            LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2); l.topMargin = u.px(6);
            panel.addView(s, l);
        }
        View gap = new View(this);
        panel.addView(gap, new LinearLayout.LayoutParams(1, u.px(20)));
    }

    void section(String t) {
        TextView k = u.text(t, true, 12, Ui.GOLD);
        k.setLetterSpacing(0.26f);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = u.px(18); l.bottomMargin = u.px(8);
        panel.addView(k, l);
    }

    /** A static info line: label on the left, value on the right. */
    void info(String label, String value) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(u.px(20), u.px(12), u.px(20), u.px(12));
        r.setBackground(u.box(0x0DFFFFFF, 12, 0, 0));
        TextView l = u.text(label, false, 17, Ui.SOFT);
        TextView v = u.text(value, true, 17, Ui.WHITE);
        v.setGravity(Gravity.END);
        v.setSingleLine(true); v.setEllipsize(TextUtils.TruncateAt.END);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(v, new LinearLayout.LayoutParams(0, -2, 1.3f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = u.px(6);
        panel.addView(r, lp);
    }

    /** A focusable action row. */
    View action(String label, String value, final Runnable act) {
        final LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setPadding(u.px(20), u.px(14), u.px(20), u.px(14));
        final TextView l = u.text(label, true, 18, Ui.WHITE);
        final TextView v = u.text(value == null ? "›" : value, false, 16, Ui.MUTED);
        v.setGravity(Gravity.END);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(v);
        r.setBackground(u.box(0x12FFFFFF, 12, 0, 0));
        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View x, boolean has) {
                x.setBackground(u.box(has ? Ui.WHITE : 0x12FFFFFF, 12, 0, 0));
                l.setTextColor(has ? 0xFF07080D : Ui.WHITE);
                v.setTextColor(has ? 0xFF4A4E5C : Ui.MUTED);
            }
        });
        r.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View x) { act.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = u.px(6);
        panel.addView(r, lp);
        return r;
    }

    void bar(float frac, int color) {
        FrameLayout track = new FrameLayout(this);
        track.setBackground(u.box(0x22FFFFFF, 4, 0, 0));
        View fill = new View(this);
        fill.setBackground(u.box(color, 4, 0, 0));
        track.addView(fill, new FrameLayout.LayoutParams(Math.round(u.px(800) * Math.max(0.01f, Math.min(1f, frac))), -1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, u.px(8)); lp.topMargin = u.px(4); lp.bottomMargin = u.px(12);
        panel.addView(track, lp);
    }

    void open(Intent i) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception ex) { android.util.Log.w("NOVAX", "open: " + ex); }
    }

    int global(String k, int def) {
        try { return Integer.parseInt(Settings.Global.getString(getContentResolver(), k)); } catch (Exception e) { return def; }
    }
    boolean putGlobal(String k, String v) {
        try {
            if (v == null) return Settings.Global.putString(getContentResolver(), k, null);
            return Settings.Global.putString(getContentResolver(), k, v);
        } catch (Exception e) { return false; }
    }

    static String mb(long bytes) {
        double g = bytes / 1073741824.0;
        return g >= 1 ? String.format(Locale.US, "%.1f GB", g) : String.format(Locale.US, "%d MB", bytes / 1048576);
    }

    // ------------------------------------------------------------------ pages
    void sub(String parentId) {   // start a sub-page inside the panel
        parent = parentId;
        panel.removeAllViews();
        scroll.scrollTo(0, 0);
    }

    void show(String id) {
        current = id;
        parent = null;
        panel.removeAllViews();
        scroll.scrollTo(0, 0);
        switch (id) {
            case "red": red(); break;
            case "sonido": sonido(); break;
            case "pantalla": pantalla(); break;
            case "sistema": sistema(); break;
            case "espacio": espacio(); break;
            case "apps": apps(); break;
            case "botones": botones(); break;
            case "info": info(); break;
            default: info(); break;
        }
        if (inside) ui.post(new Runnable() { public void run() { if (inside && !isIn(getCurrentFocus(), panel)) { View c = firstFocusable(panel); if (c != null) c.requestFocus(); } } });
    }

    void sonido() {
        final AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        final int s = AudioManager.STREAM_MUSIC;
        title("Sonido", "Usa ◀ ▶ sobre el volumen para subirlo o bajarlo.");
        section("VOLUMEN");
        final int max = am.getStreamMaxVolume(s);
        final LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setFocusable(true);
        r.setTag("slider");
        r.setPadding(u.px(20), u.px(14), u.px(20), u.px(16));
        final TextView l = u.text("", true, 18, Ui.WHITE);
        r.addView(l);
        final FrameLayout track = new FrameLayout(this);
        track.setBackground(u.box(0x33FFFFFF, 3, 0, 0));
        final View fill = new View(this);
        fill.setBackground(u.box(Ui.GOLD, 3, 0, 0));
        track.addView(fill, new FrameLayout.LayoutParams(0, -1));
        LinearLayout.LayoutParams lt = new LinearLayout.LayoutParams(-1, u.px(6)); lt.topMargin = u.px(12);
        r.addView(track, lt);
        final Runnable paint = new Runnable() {
            @Override public void run() {
                int v = am.getStreamVolume(s);
                l.setText("Volumen   " + v + " / " + max);
                FrameLayout.LayoutParams fl = (FrameLayout.LayoutParams) fill.getLayoutParams();
                fl.width = max > 0 ? Math.round((u.px(800) - u.px(40)) * v / (float) max) : 0;
                fill.setLayoutParams(fl);
            }
        };
        paint.run();
        r.setBackground(u.box(0x12FFFFFF, 12, 0, 0));
        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View x, boolean has) {
                x.setBackground(u.box(has ? 0x26FFFFFF : 0x12FFFFFF, 12, has ? Ui.GOLD : 0, has ? 1.5f : 0));
            }
        });
        r.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int code, KeyEvent e) {
                if (e.getAction() != KeyEvent.ACTION_DOWN) return code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT;
                if (code == KeyEvent.KEYCODE_DPAD_RIGHT) { am.adjustStreamVolume(s, AudioManager.ADJUST_RAISE, 0); paint.run(); return true; }
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) { am.adjustStreamVolume(s, AudioManager.ADJUST_LOWER, 0); paint.run(); return true; }
                return false;
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = u.px(6);
        panel.addView(r, lp);
        section("ESTILO");
        final boolean tv = global("novax_volumen_tv", 1) != 0;
        action("Volumen estilo televisor", tv ? "Activado" : "Desactivado", new Runnable() {
            public void run() {
                if (putGlobal("novax_volumen_tv", tv ? "0" : "1")) show("sonido");
            }
        });
    }

    void espacio() {
        long total, free;
        try {   // real numbers straight from the kernel (the box's framework reports a fake 128 GB)
            android.system.StructStatVfs v = android.system.Os.statvfs("/data");
            total = v.f_blocks * v.f_frsize; free = v.f_bavail * v.f_frsize;
        } catch (Exception e) {
            StatFs st = new StatFs(Environment.getDataDirectory().getPath());
            total = st.getTotalBytes(); free = st.getAvailableBytes();
        }
        long used = total - free;
        final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        final ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        title("Almacenamiento", "Cuánto espacio y memoria le queda a la caja.");
        section("ESPACIO");
        info("Usado", mb(used) + " de " + mb(total));
        bar(used / (float) total, free < 500L * 1048576 ? Ui.RED : Ui.GOLD);
        info("Libre", mb(free));
        section("MEMORIA");
        info("Memoria libre", mb(mi.availMem) + " de " + mb(mi.totalMem));
        bar((mi.totalMem - mi.availMem) / (float) mi.totalMem, Ui.GOLD);
        action("Liberar memoria ahora", "Cierra apps en segundo plano", new Runnable() {
            public void run() {
                long before = mi.availMem;
                for (ApplicationInfo ai : getPackageManager().getInstalledApplications(0)) {
                    if (!ai.packageName.equals(getPackageName())) {
                        try { am.killBackgroundProcesses(ai.packageName); } catch (Exception ignored) {}
                    }
                }
                ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(after);
                long gain = Math.max(0, after.availMem - before);
                show("espacio");
                section("LISTO");
                info("Memoria liberada", mb(gain));
            }
        });
    }

    void botones() {
        title("Control remoto", "Lo que hace cada botón con NOVA X.");
        section("BOTONES");
        info("Encendido", "Pregunta, despedida y apagado");
        info("Inicio (casita)", "Toque: inicio · Mantener: cambiar de app");
        info("Volumen", global("novax_volumen_tv", 1) != 0 ? "Estilo televisor" : "Estilo Android");
        info("Botón del mouse", "Ajustes / panel rápido");
        section("OPCIONES");
        final PackageManager pm = getPackageManager();
        String cur = Settings.Global.getString(getContentResolver(), "novax_netflix_app");
        String label = "Netflix (normal)";
        if (cur != null && !cur.isEmpty()) {
            try { label = pm.getApplicationLabel(pm.getApplicationInfo(cur, 0)).toString(); } catch (Exception e) { label = cur; }
        }
        action("Botón Netflix abre", label, new Runnable() { public void run() { elegirBoton(); } });
    }

    /** Pick which app the Netflix button opens. */
    void elegirBoton() {
        sub("botones");
        title("Botón Netflix", "Elige qué app abre ese botón del control.");
        action("Netflix (normal)", null, new Runnable() {
            public void run() { putGlobal("novax_netflix_app", null); show("botones"); enterPanel(); }
        });
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> all = new ArrayList<>(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        final List<String> seen = new ArrayList<>();
        Collections.sort(all, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) { return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()); }
        });
        for (final ResolveInfo r : all) {
            final String p = r.activityInfo.packageName;
            if (seen.contains(p) || p.equals(getPackageName())) continue;
            seen.add(p);
            View row = action(r.loadLabel(pm).toString(), null, new Runnable() {
                public void run() { putGlobal("novax_netflix_app", p); show("botones"); enterPanel(); }
            });
            addIcon(row, r.loadIcon(pm));
        }
        enterPanel();
    }

    void info() {
        String ver = "";
        try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception ignored) {}
        long up = SystemClock.elapsedRealtime() / 60000;
        title("Información", "NOVA X · tu caja, a tu manera.");
        section("ESTA CAJA");
        info("Sistema", "NOVA X " + ver);
        info("Android", Build.VERSION.SDK_INT == 29 ? "10 (la caja dice " + Build.VERSION.RELEASE + ")" : Build.VERSION.RELEASE);
        info("Modelo", "TX10 Pro · " + Build.HARDWARE);
        info("Encendida hace", up < 60 ? up + " min" : (up / 60) + " h " + (up % 60) + " min");
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wm != null && wm.getConnectionInfo() != null) {
            int ip = wm.getConnectionInfo().getIpAddress();
            if (ip != 0) info("Dirección IP", (ip & 255) + "." + (ip >> 8 & 255) + "." + (ip >> 16 & 255) + "." + (ip >> 24 & 255));
        }
        boolean adb = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        section("SISTEMA");
        final boolean adbOn = adb;
        action("Depuración por red (para cambios desde el PC)", adb ? "Activada" : "Desactivada", new Runnable() {
            public void run() {
                putGlobal(Settings.Global.ADB_ENABLED, adbOn ? "0" : "1");
                show("info"); enterPanel();
            }
        });
        action("Reiniciar la caja", null, new Runnable() {
            public void run() {
                try { Runtime.getRuntime().exec(new String[]{"setprop", "sys.powerctl", "reboot"}); } catch (Exception ignored) {}
            }
        });
        action("Apagar la caja", null, new Runnable() { public void run() { startActivity(new Intent(AjustesActivity.this, ApagarActivity.class)); } });
    }



    /** Android 10 hides WiFi from apps while location is off; keep it on (needs WRITE_SECURE_SETTINGS). */
    static void ensureLocation(android.content.Context c) {
        android.content.ContentResolver cr = c.getContentResolver();
        try {
            if (Settings.Secure.getInt(cr, "location_mode", 0) != 0) return;
        } catch (Exception ignored) {}
        try { Settings.Secure.putInt(cr, "location_mode", 3); } catch (Exception e) { android.util.Log.w("NOVAX", "location_mode: " + e); }
        try { Settings.Secure.putString(cr, "location_providers_allowed", "+network"); } catch (Exception ignored) {}
        try {
            android.location.LocationManager lm = (android.location.LocationManager) c.getSystemService(LOCATION_SERVICE);
            lm.getClass().getMethod("setLocationEnabledForUser", boolean.class, android.os.UserHandle.class)
                    .invoke(lm, true, android.os.Process.myUserHandle());
        } catch (Throwable t) { android.util.Log.w("NOVAX", "setLocationEnabled: " + t); }
    }

    // ================================================================== RED (WiFi inside NOVA X)
    WifiManager wifi() { return (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE); }

    static String ipStr(int ip) { return (ip & 255) + "." + (ip >> 8 & 255) + "." + (ip >> 16 & 255) + "." + (ip >> 24 & 255); }

    String currentSsid() {
        WifiInfo wi = wifi().getConnectionInfo();
        if (wi == null) return null;
        String s = wi.getSSID();
        if (s == null || s.contains("unknown")) {
            // Android 10 hides the name; find it from the saved network list instead
            try {
                int id = wi.getNetworkId();
                List<android.net.wifi.WifiConfiguration> cs = wifi().getConfiguredNetworks();
                if (cs != null) for (android.net.wifi.WifiConfiguration c : cs) if (c.networkId == id) s = c.SSID;
            } catch (Exception ignored) {}
        }
        if (s == null || s.contains("unknown")) return null;
        return s.replace("\"", "");
    }

    android.net.wifi.WifiConfiguration saved(String ssid) {
        try {
            List<android.net.wifi.WifiConfiguration> cs = wifi().getConfiguredNetworks();
            if (cs != null) for (android.net.wifi.WifiConfiguration c : cs)
                if (c.SSID != null && c.SSID.replace("\"", "").equals(ssid)) return c;
        } catch (Exception ignored) {}
        return null;
    }

    android.content.BroadcastReceiver scanRx;
    LinearLayout netList;
    boolean scannedOnce = false;

    void red() {
        final WifiManager wm = wifi();
        final WifiInfo wi = wm.getConnectionInfo();
        boolean on = wm.isWifiEnabled();
        boolean connected = wi != null && wi.getIpAddress() != 0;
        final String cur = currentSsid();
        title("Red", connected ? "Conectada a internet por WiFi." : on ? "WiFi prendido, sin conectar." : "El WiFi está apagado.");
        section("CONEXIÓN ACTUAL");
        info("Red WiFi", connected ? (cur != null ? cur : "Conectada") : "—");
        if (connected) {
            int lvl = WifiManager.calculateSignalLevel(wi.getRssi(), 5);
            String[] q = {"Muy débil", "Débil", "Buena", "Muy buena", "Excelente"};
            info("Señal", q[Math.max(0, Math.min(4, lvl))]);
            info("Dirección IP", ipStr(wi.getIpAddress()));
        }
        section("PROTECCIÓN");
        String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
        String spec = Settings.Global.getString(getContentResolver(), "private_dns_specifier");
        final boolean guard = "hostname".equals(mode) && spec != null && spec.contains("adguard");
        action("Bloqueo de anuncios y rastreo (AdGuard)", guard ? "Activado" : "Desactivado", new Runnable() {
            public void run() {
                boolean ok;
                if (guard) ok = putGlobal("private_dns_mode", "opportunistic");
                else ok = putGlobal("private_dns_specifier", "dns.adguard-dns.com") && putGlobal("private_dns_mode", "hostname");
                show("red");
                if (!ok) { section("AVISO"); info("No se pudo cambiar", "Falta permiso"); }
            }
        });
        section("WIFI");
        final boolean wasOn = on;
        action("WiFi", on ? "Prendido" : "Apagado", new Runnable() {
            public void run() {
                wm.setWifiEnabled(!wasOn);
                sub("red"); title("Red", wasOn ? "Apagando WiFi…" : "Prendiendo WiFi…");
                ui.postDelayed(new Runnable() { public void run() { show("red"); enterPanel(); } }, 3000);
            }
        });
        if (!on) return;
        section("REDES DISPONIBLES");
        netList = new LinearLayout(this);
        netList.setOrientation(LinearLayout.VERTICAL);
        panel.addView(netList, new LinearLayout.LayoutParams(-1, -2));
        fillNetworks(netList, cur);
        action("Buscar redes de nuevo", null, new Runnable() { public void run() { try { wm.startScan(); } catch (Exception ignored) {} } });
        if (scanRx == null) {
            scanRx = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent i) {
                    if (!"red".equals(current) || parent != null || netList == null) return;
                    boolean focusInList = false;
                    View f = getCurrentFocus();
                    while (f != null) { if (f == netList) { focusInList = true; break; } f = f.getParent() instanceof View ? (View) f.getParent() : null; }
                    netList.removeAllViews();
                    fillNetworks(netList, currentSsid());
                    if ((focusInList || inside) && !isIn(getCurrentFocus(), panel)) { View fc = netList.getChildCount() > 0 ? netList.getChildAt(0) : firstFocusable(panel); if (fc != null) fc.requestFocus(); }
                }
            };
            registerReceiver(scanRx, new android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
        if (!scannedOnce) { scannedOnce = true; try { wm.startScan(); } catch (Exception ignored) {} }
    }

    void fillNetworks(LinearLayout list, String cur) {
        List<android.net.wifi.ScanResult> rs;
        try { rs = wifi().getScanResults(); } catch (Exception e) { rs = new ArrayList<>(); }
        // strongest first, one row per name
        Collections.sort(rs, new Comparator<android.net.wifi.ScanResult>() {
            @Override public int compare(android.net.wifi.ScanResult a, android.net.wifi.ScanResult b) { return b.level - a.level; }
        });
        List<String> seen = new ArrayList<>();
        LinearLayout keep = panel;
        panel = list;
        for (final android.net.wifi.ScanResult r : rs) {
            final String name = r.SSID == null ? null : r.SSID.trim();
            if (name == null || name.isEmpty() || seen.contains(name)) continue;
            seen.add(name);
            final boolean secure = r.capabilities != null && (r.capabilities.contains("WPA") || r.capabilities.contains("WEP") || r.capabilities.contains("SAE"));
            int lvl = WifiManager.calculateSignalLevel(r.level, 4);
            String bars = "▂▄▆█".substring(0, Math.max(1, lvl + 1));
            boolean isCur = name.equals(cur);
            String right = (isCur ? "Conectada  " : saved(name) != null ? "Guardada  " : "") + (secure ? "🔒 " : "") + bars;
            action(name, right, new Runnable() { public void run() { wifiPage(name, secure); } });
            if (seen.size() >= 14) break;
        }
        if (seen.isEmpty()) {
            boolean locOff = false;
            try { locOff = Settings.Secure.getInt(getContentResolver(), "location_mode", 0) == 0; } catch (Exception ignored) {}
            info(locOff ? "Android bloqueó la búsqueda" : "Buscando redes…", locOff ? "ubicación apagada" : "");
        }
        panel = keep;
    }

    void wifiPage(final String ssid, final boolean secure) {
        sub("red");
        final WifiManager wm = wifi();
        final android.net.wifi.WifiConfiguration have = saved(ssid);
        boolean isCur = ssid.equals(currentSsid());
        title(ssid, isCur ? "Estás conectado a esta red." : have != null ? "Red guardada." : secure ? "Escribe la clave para conectarte." : "Red abierta, sin clave.");
        if (isCur) {
            section("OPCIONES");
            if (have != null) action("Olvidar esta red", null, new Runnable() {
                public void run() {
                    boolean ok = wm.removeNetwork(have.networkId);
                    if (ok) { try { wm.saveConfiguration(); } catch (Exception ignored) {} show("red"); }
                    else { section("AVISO"); info("Android no deja olvidarla desde aquí", "usa Ajustes técnicos"); }
                }
            });
            enterPanel();
            return;
        }
        if (have != null) {
            section("CONECTAR");
            action("Conectarme a " + ssid, null, new Runnable() { public void run() { connect(have.networkId, ssid); } });
        }
        if (secure) {
            section(have != null ? "O ESCRIBE UNA CLAVE NUEVA" : "CLAVE DEL WIFI");
            final android.widget.EditText pass = new android.widget.EditText(this);
            pass.setSingleLine(true);
            pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            pass.setTextColor(Ui.WHITE);
            pass.setHintTextColor(Ui.MUTED);
            pass.setHint("Clave");
            pass.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, u.px(20));
            pass.setPadding(u.px(20), u.px(14), u.px(20), u.px(14));
            pass.setBackground(u.box(0x12FFFFFF, 12, Ui.GOLD, 1.5f));
            pass.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = u.px(8);
            panel.addView(pass, lp);
            final Runnable go = new Runnable() {
                public void run() {
                    String k = pass.getText().toString();
                    if (k.length() < 8) { pass.setError("Mínimo 8 caracteres"); return; }
                    android.net.wifi.WifiConfiguration c = new android.net.wifi.WifiConfiguration();
                    c.SSID = "\"" + ssid + "\"";
                    c.preSharedKey = "\"" + k + "\"";
                    int id = wm.addNetwork(c);
                    if (id == -1 && have != null) {   // update existing
                        have.preSharedKey = c.preSharedKey;
                        id = wm.updateNetwork(have);
                    }
                    connect(id, ssid);
                }
            };
            pass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override public boolean onEditorAction(TextView v, int a, KeyEvent e) { go.run(); return true; }
            });
            action("Conectar", null, go);
            pass.requestFocus();
        } else if (have == null) {
            section("CONECTAR");
            action("Conectarme a " + ssid, null, new Runnable() {
                public void run() {
                    android.net.wifi.WifiConfiguration c = new android.net.wifi.WifiConfiguration();
                    c.SSID = "\"" + ssid + "\"";
                    c.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE);
                    connect(wm.addNetwork(c), ssid);
                }
            });
            enterPanel();
        } else enterPanel();
    }

    void connect(int id, final String ssid) {
        final WifiManager wm = wifi();
        sub("red");
        title(ssid, null);
        if (id == -1) {
            info("No se pudo guardar la red", "revisa la clave");
            enterPanel();
            return;
        }
        boolean ok = wm.enableNetwork(id, true);
        try { wm.saveConfiguration(); } catch (Exception ignored) {}
        wm.reconnect();
        info(ok ? "Conectando…" : "Android no aceptó la conexión", "");
        ui.postDelayed(new Runnable() {
            int tries = 0;
            public void run() {
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null && wi.getIpAddress() != 0 && ssid.equals(currentSsid())) { show("red"); return; }
                if (++tries > 10) { sub("red"); title(ssid, null); info("No se pudo conectar", "¿la clave está bien?"); enterPanel(); return; }
                ui.postDelayed(this, 1500);
            }
        }, 1500);
    }

    // ================================================================== PANTALLA
    void pantalla() {
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        title("Pantalla", "La imagen que la caja manda a tu televisor.");
        section("IMAGEN");
        info("Resolución", dm.widthPixels + " × " + dm.heightPixels + " · automática");
        info("Frecuencia", Math.round(getWindowManager().getDefaultDisplay().getRefreshRate()) + " Hz");
        section("REPOSO");
        int ss = 0;
        try { ss = Settings.Secure.getInt(getContentResolver(), "screensaver_enabled"); } catch (Exception ignored) {}
        final boolean saver = ss != 0;
        action("Protector de pantalla", saver ? "Activado" : "Desactivado", new Runnable() {
            public void run() {
                try { Settings.Secure.putInt(getContentResolver(), "screensaver_enabled", saver ? 0 : 1); } catch (Exception e) { }
                show("pantalla");
            }
        });
        int off = 0;
        try { off = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT); } catch (Exception ignored) {}
        final int[] opts = {Integer.MAX_VALUE, 3600000, 7200000, 14400000};
        final String[] names = {"Nunca", "1 hora", "2 horas", "4 horas"};
        int idx = 0;
        for (int i = 0; i < opts.length; i++) if (off == opts[i]) idx = i;
        final int next = (idx + 1) % opts.length;
        action("Apagar pantalla sin uso", names[idx], new Runnable() {
            public void run() {
                try { Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, opts[next]); }
                catch (Exception e) { }
                show("pantalla"); enterPanel();
            }
        });
        TextView n = u.text("Recomendado: Nunca. En estas cajas el reposo automático suele dejarlas pegadas.", false, 14, Ui.MUTED);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2); l.topMargin = u.px(6);
        panel.addView(n, l);
    }

    // ================================================================== FECHA E IDIOMA
    void sistema() {
        title("Fecha e idioma", new SimpleDateFormat("EEEE d 'de' MMMM, h:mm a", new Locale("es", "CO")).format(new Date()));
        section("HORA");
        int auto = 1;
        try { auto = Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME); } catch (Exception ignored) {}
        final boolean isAuto = auto != 0;
        action("Hora automática (internet)", isAuto ? "Activada" : "Desactivada", new Runnable() {
            public void run() {
                putGlobal(Settings.Global.AUTO_TIME, isAuto ? "0" : "1");
                putGlobal(Settings.Global.AUTO_TIME_ZONE, isAuto ? "0" : "1");
                show("sistema"); enterPanel();
            }
        });
        String f = null;
        try { f = Settings.System.getString(getContentResolver(), Settings.System.TIME_12_24); } catch (Exception ignored) {}
        final boolean h24 = "24".equals(f);
        action("Formato de hora", h24 ? "24 horas (18:30)" : "12 horas (6:30 p. m.)", new Runnable() {
            public void run() {
                try { Settings.System.putString(getContentResolver(), Settings.System.TIME_12_24, h24 ? "12" : "24"); } catch (Exception e) { }
                show("sistema"); enterPanel();
            }
        });
        info("Zona horaria", java.util.TimeZone.getDefault().getID().replace("_", " "));
        section("IDIOMA");
        Locale cur = getResources().getConfiguration().getLocales().get(0);
        final String[][] langs = {{"es", "CO", "Español"}, {"en", "US", "English"}};
        for (final String[] lg : langs) {
            boolean is = cur.getLanguage().equals(lg[0]);
            action(lg[2], is ? "En uso" : "", new Runnable() {
                public void run() {
                    boolean ok = setLanguage(new Locale(lg[0], lg[1]));
                    show("sistema");
                    if (!ok) { section("AVISO"); info("No se pudo cambiar el idioma", "falta permiso"); }
                }
            });
        }
    }

    boolean setLanguage(Locale loc) {
        try {
            Object am = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
            android.content.res.Configuration cfg = (android.content.res.Configuration) am.getClass().getMethod("getConfiguration").invoke(am);
            cfg.setLocales(new android.os.LocaleList(loc));
            try { android.content.res.Configuration.class.getField("userSetLocale").setBoolean(cfg, true); } catch (Exception ignored) {}
            am.getClass().getMethod("updatePersistentConfiguration", android.content.res.Configuration.class).invoke(am, cfg);
            return true;
        } catch (Throwable t) {
            android.util.Log.w("NOVAX", "idioma: " + t);
            return false;
        }
    }

    // ================================================================== APLICACIONES
    void apps() {
        title("Aplicaciones", "Elige una para abrirla, cerrarla o desinstalarla.");
        final PackageManager pm = getPackageManager();
        List<ResolveInfo> all = new ArrayList<>();
        all.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0));
        all.addAll(pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0));
        final List<String> seen = new ArrayList<>();
        final List<ResolveInfo> list = new ArrayList<>();
        for (ResolveInfo r : all) {
            String p = r.activityInfo.packageName;
            if (seen.contains(p) || p.equals(getPackageName())) continue;
            seen.add(p); list.add(r);
        }
        Collections.sort(list, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });
        section(list.size() + " APPS");
        for (final ResolveInfo r : list) {
            final String pkg = r.activityInfo.packageName;
            final String label = r.loadLabel(pm).toString();
            View row = action(label, "›", new Runnable() { public void run() { appPage(pkg, label); } });
            addIcon(row, r.loadIcon(pm));
        }
    }

    void addIcon(View row, Drawable d) {
        try {
            ImageView ic = new ImageView(this);
            ic.setImageDrawable(d);
            LinearLayout.LayoutParams li = new LinearLayout.LayoutParams(u.px(28), u.px(28)); li.rightMargin = u.px(14);
            ((LinearLayout) row).addView(ic, 0, li);
        } catch (Exception ignored) {}
    }

    void appPage(final String pkg, final String label) {
        sub("apps");
        final PackageManager pm = getPackageManager();
        String ver = "";
        boolean system = false;
        long installed = 0;
        try {
            android.content.pm.PackageInfo pi = pm.getPackageInfo(pkg, 0);
            ver = pi.versionName; installed = pi.firstInstallTime;
            system = (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (Exception ignored) {}
        title(label, (ver == null ? "" : "Versión " + ver) + (system ? " · viene con la caja" : ""));
        section("ESPACIO");
        try {
            android.app.usage.StorageStatsManager ssm = (android.app.usage.StorageStatsManager) getSystemService(STORAGE_STATS_SERVICE);
            android.app.usage.StorageStats st = ssm.queryStatsForPackage(android.os.storage.StorageManager.UUID_DEFAULT, pkg, android.os.Process.myUserHandle());
            info("App", mb(st.getAppBytes()));
            info("Datos", mb(st.getDataBytes() - st.getCacheBytes()));
            info("Caché", mb(st.getCacheBytes()));
            info("Total", mb(st.getAppBytes() + st.getDataBytes()));
        } catch (Throwable t) {
            info("Tamaño", "no disponible");
        }
        if (installed > 0) info("Instalada", new SimpleDateFormat("d MMM yyyy", new Locale("es", "CO")).format(new Date(installed)));
        section("ACCIONES");
        action("Abrir", null, new Runnable() {
            public void run() {
                Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
                if (i == null) i = pm.getLaunchIntentForPackage(pkg);
                if (i != null) open(i);
            }
        });
        action("Cerrar (si se quedó pegada)", null, new Runnable() {
            public void run() {
                ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).killBackgroundProcesses(pkg);
                appPage(pkg, label);
                section("LISTO");
                info("Cerrada", label);
            }
        });
        if (!system) action("Desinstalar", "pide confirmar", new Runnable() {
            public void run() {
                Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
                open(i);
            }
        });
        enterPanel();
    }
}
