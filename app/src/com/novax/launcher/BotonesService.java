package com.novax.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

/**
 * Global remote-control buttons for NOVA X.
 * Every key is logged as "NOVAXKEY" so new buttons can be identified.
 * Mapping is read from Settings.Global (set from the PC, no rebuild needed):
 *   novax_netflix_app  -> package the Netflix button opens instead (chosen in Ajustes)
 *   novax_key_ajustes  -> keycode that opens Settings
 */
public class BotonesService extends AccessibilityService {

    static final String TAG = "NOVAXKEY";
    static BotonesService instance;
    android.content.BroadcastReceiver wakeRx;

    /** Put the box into standby (screen off). Power button wakes it back to NOVA X. */
    static void standby() {
        // 1) real power-off, like unplugging (init shuts the box down cleanly)
        new Thread(new Runnable() { public void run() {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"setprop", "sys.powerctl", "shutdown"});
                Log.i(TAG, "apagado completo pedido, salida=" + p.waitFor());
            } catch (Exception ex) { Log.w(TAG, "apagado completo: " + ex); }
        } }).start();
        // 2) if the box is still alive a few seconds later, fall back to standby
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { public void run() { lockScreen(); } }, 6000);
    }

    static void lockScreen() {
        BotonesService b = instance;
        Log.w(TAG, "apagado completo no funciono, uso reposo");
        if (b != null) {
            try {
                android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) b.getSystemService(DEVICE_POLICY_SERVICE);
                if (dpm.isAdminActive(new android.content.ComponentName(b, Apagado.class))) { dpm.lockNow(); return; }
            } catch (Exception ex) { Log.w(TAG, "lockNow: " + ex); }
            if (android.os.Build.VERSION.SDK_INT >= 28) b.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        }
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        AjustesActivity.ensureLocation(this);
        Reloj.vigilar(this);
        Reloj.ui.removeCallbacks(Reloj.cadaMinuto);
        Reloj.ui.postDelayed(Reloj.cadaMinuto, 20000);
        registerReceiver(new android.content.BroadcastReceiver() {
            @Override public void onReceive(android.content.Context c, Intent in) { Reloj.vigilar(c); }
        }, new android.content.IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        if (wakeRx == null) {
            wakeRx = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent in) {
                    if (setting("novax_intro_encender") == 0) return;
                    try {
                        startActivity(new Intent(BotonesService.this, EncenderActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
                    } catch (Exception ex) { Log.w(TAG, "intro: " + ex); }
                }
            };
            registerReceiver(wakeRx, new android.content.IntentFilter(Intent.ACTION_SCREEN_ON));
        }
        AccessibilityServiceInfo i = getServiceInfo();
        if (i != null) {
            i.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(i);
        }
        Log.i(TAG, "servicio listo");
    }

    int setting(String name) {
        try { return Integer.parseInt(Settings.Global.getString(getContentResolver(), name)); }
        catch (Exception e) { return -1; }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        Log.i(TAG, "code=" + code + " name=" + KeyEvent.keyCodeToString(code) + " scan=" + e.getScanCode()
                + " action=" + (e.getAction() == KeyEvent.ACTION_DOWN ? "DOWN" : "UP") + " repeat=" + e.getRepeatCount());
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN
                || code == KeyEvent.KEYCODE_VOLUME_MUTE || code == KeyEvent.KEYCODE_MUTE) {
            if (setting("novax_volumen_tv") == 0) return false;   // escape hatch: system volume UI
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (e.getRepeatCount() == 0) {
                    volume(code);
                    if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        heldCode = code;
                        ui.removeCallbacks(repeat);
                        ui.postDelayed(repeat, 380);   // hold: start repeating after a short pause
                    }
                }
            } else {
                heldCode = -1;
                ui.removeCallbacks(repeat);
            }
            return true;
        }
        int apagar = setting("novax_key_apagar");
        if (apagar == -1) apagar = KeyEvent.KEYCODE_STEM_1;   // remote power button, remapped (wake key)
        if (code == apagar || code == KeyEvent.KEYCODE_F12) {
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) {
                if (ApagarActivity.open != null) ApagarActivity.open.startShutdown();  // second press = apagar
                else {
                    try { startActivity(new Intent(this, ApagarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
                    catch (Exception ex) { Log.w(TAG, "apagar: " + ex); standby(); }
                }
            }
            return true;
        }
        // HOME: short press = NOVA X home, hold = recent apps bar
        if (code == KeyEvent.KEYCODE_HOME) {
            if (e.getAction() == KeyEvent.ACTION_DOWN) {
                if (e.getRepeatCount() == 0) { homeLong = false; ui.removeCallbacks(homeHold); ui.postDelayed(homeHold, 550); }
            } else {
                ui.removeCallbacks(homeHold);
                if (!homeLong) goHome();
            }
            return true;
        }
        // Netflix button: opens the app chosen in Ajustes > Control remoto (if any)
        if (code == KeyEvent.KEYCODE_PROG_RED) {
            String app = Settings.Global.getString(getContentResolver(), "novax_netflix_app");
            if (app == null || app.isEmpty()) return false;          // normal behaviour
            if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) openApp(app);
            return true;
        }
        int ajustes = setting("novax_key_ajustes");
        if (code != ajustes) return false;
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) openSettings();
        return true; // consume both DOWN and UP so the original action does not run
    }

    void openApp(String pkg) {
        PackageManager pm = getPackageManager();
        Intent i = null;
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat).setPackage(pkg);
            List<ResolveInfo> r = pm.queryIntentActivities(q, 0);
            if (!r.isEmpty()) {
                i = q.setClassName(pkg, r.get(0).activityInfo.name);
                break;
            }
        }
        if (i == null) { Log.w(TAG, "app no instalada: " + pkg); return; }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try { startActivity(i); } catch (Exception ex) { Log.w(TAG, "no abre " + pkg + ": " + ex); }
    }

    boolean homeLong = false;
    final Runnable homeHold = new Runnable() {
        @Override public void run() {
            homeLong = true;
            if (CambiarActivity.open != null) { CambiarActivity.open.finish(); return; }
            try { startActivity(new Intent(BotonesService.this, CambiarActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION)); }
            catch (Exception ex) { Log.w(TAG, "cambiar: " + ex); }
        }
    };

    void goHome() {
        if (CambiarActivity.open != null) CambiarActivity.open.finish();
        if (PanelActivity.open != null) PanelActivity.open.finish();
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setClass(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Exception ex) { performGlobalAction(GLOBAL_ACTION_HOME); }
    }

    /** Mouse button: close what is open, else full settings on the NOVA X home, quick panel inside apps. */
    void openSettings() {
        try {
            if (PanelActivity.open != null) { PanelActivity.open.finish(); return; }
            if (AjustesActivity.open != null && getPackageName().equals(fgPkg)) { AjustesActivity.open.finish(); return; }
            boolean inApp = lastApp != null && !getPackageName().equals(fgPkg) ? true : false;
            if (fgPkg != null && !getPackageName().equals(fgPkg) && !"com.android.systemui".equals(fgPkg)) inApp = true;
            if (getPackageName().equals(fgPkg)) inApp = false;
            Class<?> target = inApp ? PanelActivity.class : AjustesActivity.class;
            startActivity(new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ex) { Log.w(TAG, "no abre ajustes: " + ex); }
    }

    // ------------------------------------------------------------ which app is on screen
    static String lastApp;     // last real app (not NOVA X) the user had open
    String fgPkg;

    void track(AccessibilityEvent ev) {
        if (ev.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence p = ev.getPackageName(), c = ev.getClassName();
        if (p == null || c == null) return;
        try {   // only real activities count (ignore dialogs, keyboards, toasts)
            getPackageManager().getActivityInfo(new android.content.ComponentName(p.toString(), c.toString()), 0);
        } catch (Exception notActivity) { return; }
        fgPkg = p.toString();
        if (!fgPkg.equals(getPackageName()) && !fgPkg.equals("com.android.systemui")) lastApp = fgPkg;
    }

    // ------------------------------------------------------------ TV-style volume
    final Handler ui = new Handler(Looper.getMainLooper());
    int heldCode = -1;
    final Runnable repeat = new Runnable() {
        @Override public void run() {
            if (heldCode == -1) return;
            volume(heldCode);
            ui.postDelayed(this, 90);   // fast steps while the button stays pressed
        }
    };
    LinearLayout osd;
    TextView osdNum;
    View osdBar, osdTrack;
    final Runnable hide = new Runnable() {
        @Override public void run() {
            if (osd != null) osd.animate().alpha(0f).setDuration(350).start();
        }
    };

    int px(float v) {
        float k = getResources().getDisplayMetrics().widthPixels / 1280f;
        return Math.round(v * k);
    }

    void volume(int code) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int s = AudioManager.STREAM_MUSIC;
        if (code == KeyEvent.KEYCODE_VOLUME_UP) am.adjustStreamVolume(s, AudioManager.ADJUST_RAISE, 0);
        else if (code == KeyEvent.KEYCODE_VOLUME_DOWN) am.adjustStreamVolume(s, AudioManager.ADJUST_LOWER, 0);
        else am.adjustStreamVolume(s, AudioManager.ADJUST_TOGGLE_MUTE, 0);
        int v = am.getStreamVolume(s), max = am.getStreamMaxVolume(s);
        boolean muted = false;
        try { muted = am.isStreamMute(s); } catch (Throwable ignored) {}
        showOsd(muted ? 0 : v, max, muted);
    }

    void showOsd(int v, int max, boolean muted) {
        if (osd == null) {
            osd = new LinearLayout(this);
            osd.setOrientation(LinearLayout.VERTICAL);
            osd.setGravity(Gravity.CENTER_HORIZONTAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xE60A0C14); bg.setCornerRadius(px(18)); bg.setStroke(Math.max(1, px(1)), 0x55E6C35C);
            osd.setBackground(bg);
            osd.setPadding(px(26), px(14), px(26), px(16));
            TextView k = new TextView(this);
            k.setText("VOLUMEN");
            k.setTextColor(0xFFE6C35C);
            k.setLetterSpacing(0.3f);
            k.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(12));
            osd.addView(k);
            osdNum = new TextView(this);
            osdNum.setTextColor(0xFFF4F5F8);
            osdNum.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(56));
            osdNum.setIncludeFontPadding(false);
            try { osdNum.setTypeface(android.graphics.Typeface.createFromAsset(getAssets(), "fonts/medium.otf")); } catch (Exception ignored) {}
            LinearLayout.LayoutParams ln = new LinearLayout.LayoutParams(-2, -2);
            ln.topMargin = px(6); ln.bottomMargin = px(10);
            osd.addView(osdNum, ln);
            android.widget.FrameLayout track = new android.widget.FrameLayout(this);
            GradientDrawable tb = new GradientDrawable(); tb.setColor(0x33FFFFFF); tb.setCornerRadius(px(2));
            track.setBackground(tb);
            osdBar = new View(this);
            GradientDrawable bb = new GradientDrawable(); bb.setColor(0xFFE6C35C); bb.setCornerRadius(px(2));
            osdBar.setBackground(bb);
            track.addView(osdBar, new android.widget.FrameLayout.LayoutParams(0, -1));
            osd.addView(track, new LinearLayout.LayoutParams(px(110), px(4)));
            osdTrack = track;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.END;
            lp.x = px(48); lp.y = px(40);
            try { ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(osd, lp); }
            catch (Exception ex) { Log.w(TAG, "overlay: " + ex); osd = null; return; }
        }
        osdNum.setText(muted ? "Silencio" : String.valueOf(v));
        osdNum.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(muted ? 30 : 56));
        android.widget.FrameLayout.LayoutParams bl = (android.widget.FrameLayout.LayoutParams) osdBar.getLayoutParams();
        bl.width = max > 0 ? Math.round(px(110) * (v / (float) max)) : 0;
        osdBar.setLayoutParams(bl);
        osd.animate().cancel();
        osd.setAlpha(1f);
        ui.removeCallbacks(hide);
        ui.postDelayed(hide, 2200);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { try { track(event); } catch (Exception ignored) {} }
    @Override public void onInterrupt() { }
}
