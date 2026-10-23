package com.novax.launcher;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Quick side panel shown over any app (mouse button): the few things you need without leaving. */
public class PanelActivity extends Activity {

    static PanelActivity open;
    Ui u;
    LinearLayout box, sheet;
    android.widget.ScrollView scroll;
    String mode = "main";          // "main" or "redes"
    android.content.BroadcastReceiver scanRx;
    final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        open = this;
        Ui.immersive(this);
        u = new Ui(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x66000000);
        root.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { finish(); } });

        sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xF20B0D15, 0xFA07080D}));
        sheet.setPadding(u.px(28), u.px(30), u.px(28), u.px(24));
        scroll = new android.widget.ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setFillViewport(true);
        scroll.addView(box, new FrameLayout.LayoutParams(-1, -2));
        sheet.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams lb = new FrameLayout.LayoutParams(u.px(400), -1, Gravity.END);
        root.addView(sheet, lb);
        View edge = new View(this);
        edge.setBackgroundColor(0x33E6C35C);
        FrameLayout.LayoutParams le = new FrameLayout.LayoutParams(Math.max(1, u.px(1)), -1, Gravity.END);
        le.rightMargin = u.px(400);
        root.addView(edge, le);
        setContentView(root);
        build();
        sheet.setTranslationX(u.px(60)); sheet.setAlpha(0f);
        sheet.animate().translationX(0).alpha(1f).setDuration(220).start();
    }

    @Override protected void onDestroy() {
        if (open == this) open = null;
        ui.removeCallbacksAndMessages(null);
        mode = "main";
        if (scanRx != null) try { unregisterReceiver(scanRx); } catch (Exception ignored) {}
        super.onDestroy();
    }

    void build() {
        mode = "main";
        box.removeAllViews();
        TextView k = u.text("NOVA X", true, 13, Ui.GOLD);
        k.setLetterSpacing(0.32f);
        box.addView(k);
        TextView clock = u.text(new SimpleDateFormat("h:mm a", new Locale("es", "CO")).format(new Date()), true, 34, Ui.WHITE);
        LinearLayout.LayoutParams lc = new LinearLayout.LayoutParams(-2, -2); lc.topMargin = u.px(6); lc.bottomMargin = u.px(18);
        box.addView(clock, lc);

        // ---- WiFi
        final WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final boolean on = wm.isWifiEnabled();
        WifiInfo wi = wm.getConnectionInfo();
        boolean connected = wi != null && wi.getIpAddress() != 0;
        String name = null;
        if (connected) {
            name = wi.getSSID();
            if (name == null || name.contains("unknown")) {
                try {
                    for (android.net.wifi.WifiConfiguration c : wm.getConfiguredNetworks())
                        if (c.networkId == wi.getNetworkId()) name = c.SSID;
                } catch (Exception ignored) {}
            }
            if (name != null) name = name.replace("\"", "");
            if (name == null || name.contains("unknown")) name = "Conectado";
        }
        section("INTERNET");
        row(on ? (connected ? name : "WiFi sin conexión") : "WiFi apagado",
                on ? (connected ? "Conectado ✓" : "Elegir red ›") : "Toca para prender",
                connected ? 0xFF7BD88F : on ? Ui.RED : Ui.MUTED, new Runnable() {
                    public void run() {
                        if (!on) { wm.setWifiEnabled(true); status("Prendiendo WiFi…"); ui.postDelayed(new Runnable() { public void run() { build(); focusFirst(); } }, 3500); }
                        else redes();
                    }
                });
        if (on) row("Cambiar de red", "›", 0, new Runnable() { public void run() { redes(); } });
        if (on) row("Reconectar WiFi", "si se quedó pegado", 0, new Runnable() {
            public void run() {
                wm.disconnect(); wm.reconnect();
                ui.postDelayed(new Runnable() { public void run() { build(); focusFirst(); } }, 4000);
            }
        });

        // ---- Volume
        section("SONIDO");
        final AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        final int s = AudioManager.STREAM_MUSIC;
        final int max = am.getStreamMaxVolume(s);
        final LinearLayout vol = new LinearLayout(this);
        vol.setOrientation(LinearLayout.VERTICAL);
        vol.setFocusable(true);
        vol.setPadding(u.px(18), u.px(12), u.px(18), u.px(14));
        final TextView vl = u.text("", true, 17, Ui.WHITE);
        vol.addView(vl);
        final FrameLayout track = new FrameLayout(this);
        track.setBackground(u.box(0x33FFFFFF, 3, 0, 0));
        final View fill = new View(this);
        fill.setBackground(u.box(Ui.GOLD, 3, 0, 0));
        track.addView(fill, new FrameLayout.LayoutParams(0, -1));
        LinearLayout.LayoutParams lt = new LinearLayout.LayoutParams(-1, u.px(6)); lt.topMargin = u.px(10);
        vol.addView(track, lt);
        final Runnable paint = new Runnable() {
            public void run() {
                int v = am.getStreamVolume(s);
                vl.setText("Volumen  " + v + "   ◀ ▶");
                FrameLayout.LayoutParams fl = (FrameLayout.LayoutParams) fill.getLayoutParams();
                fl.width = max > 0 ? Math.round((u.px(400) - u.px(56) - u.px(36)) * v / (float) max) : 0;
                fill.setLayoutParams(fl);
            }
        };
        paint.run();
        vol.setBackground(u.box(0x12FFFFFF, 12, 0, 0));
        vol.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View x, boolean has) { x.setBackground(u.box(has ? 0x26FFFFFF : 0x12FFFFFF, 12, has ? Ui.GOLD : 0, has ? 1.5f : 0)); }
        });
        vol.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View v, int code, KeyEvent e) {
                if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                if (e.getAction() == KeyEvent.ACTION_DOWN) {
                    am.adjustStreamVolume(s, code == KeyEvent.KEYCODE_DPAD_RIGHT ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                    paint.run();
                }
                return true;
            }
        });
        LinearLayout.LayoutParams lv = new LinearLayout.LayoutParams(-1, -2); lv.bottomMargin = u.px(6);
        box.addView(vol, lv);

        // ---- Sleep timer (full power-off, never standby)
        section("APAGADO AUTOMÁTICO");
        final int left = Temporizador.restante(this);
        row("Apagar en", left > 0 ? "quedan " + Temporizador.texto(left) : "No", left > 0 ? Ui.GOLD : 0, new Runnable() {
            public void run() {
                int next = Temporizador.siguiente(PanelActivity.this);
                Temporizador.poner(PanelActivity.this, next);
                build();
                // keep the cursor on the timer row so it can be pressed again
                for (int i = 0; i < box.getChildCount(); i++) {
                    View v = box.getChildAt(i);
                    if (v.isFocusable() && v instanceof LinearLayout && ((LinearLayout) v).getChildCount() > 0
                            && ((LinearLayout) v).getChildAt(((LinearLayout) v).getChildCount() - 2) instanceof TextView
                            && "Apagar en".contentEquals(((TextView) ((LinearLayout) v).getChildAt(((LinearLayout) v).getChildCount() - 2)).getText())) { v.requestFocus(); break; }
                }
            }
        });

        // ---- Protection
        String mode = Settings.Global.getString(getContentResolver(), "private_dns_mode");
        String spec = Settings.Global.getString(getContentResolver(), "private_dns_specifier");
        final boolean guard = "hostname".equals(mode) && spec != null && spec.contains("adguard");
        section("PROTECCIÓN");
        row("Bloqueo de anuncios", guard ? "Activado" : "Desactivado", 0, new Runnable() {
            public void run() {
                try {
                    if (guard) Settings.Global.putString(getContentResolver(), "private_dns_mode", "opportunistic");
                    else {
                        Settings.Global.putString(getContentResolver(), "private_dns_specifier", "dns.adguard-dns.com");
                        Settings.Global.putString(getContentResolver(), "private_dns_mode", "hostname");
                    }
                } catch (Exception ignored) {}
                build(); focusFirst();
            }
        });

        // ---- More
        View sp = new View(this);
        box.addView(sp, new LinearLayout.LayoutParams(1, u.px(14)));
        row("Todos los ajustes", null, Ui.GOLD, new Runnable() { public void run() { openFull(null); } });
        row("Ir al inicio", null, 0, new Runnable() {
            public void run() {
                startActivity(new Intent(PanelActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
        row("Apagar", null, 0, new Runnable() { public void run() { startActivity(new Intent(PanelActivity.this, ApagarActivity.class)); finish(); } });
        TextView h = u.text("Botón del mouse o ATRÁS para cerrar", false, 13, Ui.MUTED);
        LinearLayout.LayoutParams lh = new LinearLayout.LayoutParams(-2, -2); lh.topMargin = u.px(10);
        box.addView(h, lh);
        focusFirst();
    }

    // ================================================================ WiFi inside the panel
    void status(String t) {
        box.removeAllViews();
        TextView k = u.text("NOVA X", true, 13, Ui.GOLD); k.setLetterSpacing(0.32f); box.addView(k);
        TextView m = u.text(t, true, 22, Ui.WHITE);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2); l.topMargin = u.px(30);
        box.addView(m, l);
    }

    String currentSsid(WifiManager wm) {
        WifiInfo wi = wm.getConnectionInfo();
        if (wi == null || wi.getIpAddress() == 0) return null;
        String n = wi.getSSID();
        if (n == null || n.contains("unknown")) {
            try { for (android.net.wifi.WifiConfiguration c : wm.getConfiguredNetworks()) if (c.networkId == wi.getNetworkId()) n = c.SSID; } catch (Exception ignored) {}
        }
        return n == null || n.contains("unknown") ? null : n.replace("\"", "");
    }

    android.net.wifi.WifiConfiguration saved(WifiManager wm, String ssid) {
        try { for (android.net.wifi.WifiConfiguration c : wm.getConfiguredNetworks()) if (c.SSID != null && c.SSID.replace("\"", "").equals(ssid)) return c; } catch (Exception ignored) {}
        return null;
    }

    void redes() {
        mode = "redes";
        final WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        AjustesActivity.ensureLocation(this);
        box.removeAllViews();
        TextView k = u.text("REDES WIFI", true, 13, Ui.GOLD); k.setLetterSpacing(0.32f); box.addView(k);
        TextView h = u.text("Elige una red · ATRÁS para volver", false, 13, Ui.MUTED);
        LinearLayout.LayoutParams lh = new LinearLayout.LayoutParams(-2, -2); lh.topMargin = u.px(6); lh.bottomMargin = u.px(14);
        box.addView(h, lh);
        String cur = currentSsid(wm);
        java.util.List<android.net.wifi.ScanResult> rs;
        try { rs = wm.getScanResults(); } catch (Exception e) { rs = new java.util.ArrayList<>(); }
        java.util.Collections.sort(rs, new java.util.Comparator<android.net.wifi.ScanResult>() {
            @Override public int compare(android.net.wifi.ScanResult a, android.net.wifi.ScanResult b) { return b.level - a.level; }
        });
        java.util.List<String> seen = new java.util.ArrayList<>();
        for (final android.net.wifi.ScanResult r : rs) {
            final String n = r.SSID == null ? "" : r.SSID.trim();
            if (n.isEmpty() || seen.contains(n)) continue;
            seen.add(n);
            final boolean secure = r.capabilities != null && (r.capabilities.contains("WPA") || r.capabilities.contains("WEP") || r.capabilities.contains("SAE"));
            int lvl = WifiManager.calculateSignalLevel(r.level, 4);
            String bars = "▂▄▆█".substring(0, Math.max(1, lvl + 1));
            boolean isCur = n.equals(cur);
            String right = (isCur ? "✓ " : saved(wm, n) != null ? "guardada " : "") + (secure ? "🔒" : "") + bars;
            row(n, right, isCur ? 0xFF7BD88F : 0, new Runnable() { public void run() { red(n, secure); } });
            if (seen.size() >= 12) break;
        }
        if (seen.isEmpty()) {
            TextView t = u.text("Buscando redes…", false, 16, Ui.SOFT);
            box.addView(t);
        }
        row("Buscar otra vez", null, 0, new Runnable() { public void run() { try { wm.startScan(); } catch (Exception ignored) {} } });
        if (scanRx == null) {
            scanRx = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent i) { if ("redes".equals(mode)) { redes(); } }
            };
            registerReceiver(scanRx, new android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            try { wm.startScan(); } catch (Exception ignored) {}
        }
        focusFirst();
    }

    void red(final String ssid, final boolean secure) {
        mode = "red";
        final WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        final android.net.wifi.WifiConfiguration have = saved(wm, ssid);
        box.removeAllViews();
        TextView k = u.text("RED WIFI", true, 13, Ui.GOLD); k.setLetterSpacing(0.32f); box.addView(k);
        TextView t = u.text(ssid, true, 24, Ui.WHITE);
        LinearLayout.LayoutParams lt = new LinearLayout.LayoutParams(-2, -2); lt.topMargin = u.px(6); lt.bottomMargin = u.px(16);
        box.addView(t, lt);
        if (ssid.equals(currentSsid(wm))) {
            TextView ok = u.text("Ya estás conectado a esta red.", false, 16, Ui.SOFT);
            box.addView(ok);
            View g = new View(this); box.addView(g, new LinearLayout.LayoutParams(1, u.px(14)));
            if (have != null) row("Olvidar esta red", null, 0, new Runnable() {
                public void run() { if (wm.removeNetwork(have.networkId)) { try { wm.saveConfiguration(); } catch (Exception ignored) {} } redes(); }
            });
            row("Volver", null, 0, new Runnable() { public void run() { redes(); } });
            focusFirst();
            return;
        }
        if (have != null) row("Conectar", "red guardada", Ui.GOLD, new Runnable() { public void run() { connect(have.networkId, ssid); } });
        if (secure) {
            TextView l = u.text(have != null ? "O escribe una clave nueva:" : "Clave del WiFi:", false, 14, Ui.MUTED);
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-2, -2); ll.topMargin = u.px(8); ll.bottomMargin = u.px(6);
            box.addView(l, ll);
            final android.widget.EditText pass = new android.widget.EditText(this);
            pass.setSingleLine(true);
            pass.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            pass.setTextColor(Ui.WHITE);
            pass.setHintTextColor(Ui.MUTED);
            pass.setHint("Clave");
            pass.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, u.px(18));
            pass.setPadding(u.px(16), u.px(12), u.px(16), u.px(12));
            pass.setBackground(u.box(0x12FFFFFF, 12, Ui.GOLD, 1.5f));
            pass.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = u.px(8);
            box.addView(pass, lp);
            final Runnable go = new Runnable() {
                public void run() {
                    String key = pass.getText().toString();
                    if (key.length() < 8) { pass.setError("Mínimo 8 caracteres"); return; }
                    android.net.wifi.WifiConfiguration c = new android.net.wifi.WifiConfiguration();
                    c.SSID = "\"" + ssid + "\"";
                    c.preSharedKey = "\"" + key + "\"";
                    int id = wm.addNetwork(c);
                    if (id == -1 && have != null) { have.preSharedKey = c.preSharedKey; id = wm.updateNetwork(have); }
                    connect(id, ssid);
                }
            };
            pass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override public boolean onEditorAction(TextView v, int a, KeyEvent e) { go.run(); return true; }
            });
            row("Conectar con esta clave", null, have == null ? Ui.GOLD : 0, go);
            row("Volver", null, 0, new Runnable() { public void run() { redes(); } });
            if (have == null) { pass.requestFocus(); return; }
        } else if (have == null) {
            row("Conectar", "red abierta", Ui.GOLD, new Runnable() {
                public void run() {
                    android.net.wifi.WifiConfiguration c = new android.net.wifi.WifiConfiguration();
                    c.SSID = "\"" + ssid + "\"";
                    c.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE);
                    connect(wm.addNetwork(c), ssid);
                }
            });
            row("Volver", null, 0, new Runnable() { public void run() { redes(); } });
        } else row("Volver", null, 0, new Runnable() { public void run() { redes(); } });
        focusFirst();
    }

    void connect(int id, final String ssid) {
        final WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mode = "conectando";
        if (id == -1) { status("No se pudo guardar la red. Revisa la clave."); ui.postDelayed(new Runnable() { public void run() { red(ssid, true); } }, 2500); return; }
        wm.enableNetwork(id, true);
        try { wm.saveConfiguration(); } catch (Exception ignored) {}
        wm.reconnect();
        status("Conectando a " + ssid + "…");
        ui.postDelayed(new Runnable() {
            int tries = 0;
            public void run() {
                if (ssid.equals(currentSsid(wm))) { status("Conectado a " + ssid + " ✓"); ui.postDelayed(new Runnable() { public void run() { build(); } }, 1500); return; }
                if (++tries > 12) { status("No se pudo conectar. ¿La clave está bien?"); ui.postDelayed(new Runnable() { public void run() { red(ssid, true); } }, 2500); return; }
                ui.postDelayed(this, 1500);
            }
        }, 1500);
    }

    void focusFirst() {
        for (int i = 0; i < box.getChildCount(); i++) if (box.getChildAt(i).isFocusable()) { box.getChildAt(i).requestFocus(); return; }
    }

    void openFull(String section) {
        Intent i = new Intent(this, AjustesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if ("red".equals(section)) i.setAction(Settings.ACTION_WIFI_SETTINGS);
        startActivity(i);
        finish();
    }

    void section(String t) {
        TextView k = u.text(t, true, 11, Ui.GOLD);
        k.setLetterSpacing(0.26f);
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = u.px(12); l.bottomMargin = u.px(6);
        box.addView(k, l);
    }

    void row(String label, String value, int dot, final Runnable act) {
        final LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setFocusable(true);
        r.setPadding(u.px(18), u.px(12), u.px(18), u.px(12));
        if (dot != 0) {
            View d = new View(this);
            d.setBackground(u.box(dot, 5, 0, 0));
            LinearLayout.LayoutParams ld = new LinearLayout.LayoutParams(u.px(9), u.px(9)); ld.rightMargin = u.px(12);
            r.addView(d, ld);
        }
        final TextView l = u.text(label, true, 17, Ui.WHITE);
        l.setSingleLine(true); l.setEllipsize(TextUtils.TruncateAt.END);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        final TextView v = u.text(value == null ? "›" : value, false, 14, Ui.MUTED);
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
        box.addView(r, lp);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int c = e.getKeyCode();
        if ((c == KeyEvent.KEYCODE_DPAD_UP || c == KeyEvent.KEYCODE_DPAD_DOWN) && e.getAction() == KeyEvent.ACTION_DOWN) {
            View f = getCurrentFocus();
            if (f != null) {
                View n = android.view.FocusFinder.getInstance().findNextFocus(box, f, c == KeyEvent.KEYCODE_DPAD_UP ? View.FOCUS_UP : View.FOCUS_DOWN);
                if (n != null) n.requestFocus();
            }
            return true;
        }
        if (c == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP && !"main".equals(mode)) {
            if ("redes".equals(mode)) { build(); } else if (!"conectando".equals(mode)) { redes(); }
            return true;
        }
        if (c == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_DOWN && !"main".equals(mode)) return true;
        if (c == KeyEvent.KEYCODE_DPAD_LEFT && e.getAction() == KeyEvent.ACTION_DOWN && "main".equals(mode)
                && !(getCurrentFocus() != null && isVolume(getCurrentFocus()))) {
            finish(); return true;
        }
        return super.dispatchKeyEvent(e);
    }

    boolean isVolume(View v) { return !(v instanceof android.widget.EditText) && v instanceof LinearLayout && ((LinearLayout) v).getOrientation() == LinearLayout.VERTICAL; }

    @Override public void finish() { super.finish(); overridePendingTransition(0, android.R.anim.fade_out); }
}
