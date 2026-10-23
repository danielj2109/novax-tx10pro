package com.novax.launcher;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

/** NOVA X shutdown: a TV-style question, then the ship sequence, then standby. */
public class ApagarActivity extends Activity {

    Ui u;
    FrameLayout root;
    LinearLayout dialog;
    VideoView video;
    final Handler ui = new Handler(Looper.getMainLooper());
    boolean playing = false;
    static ApagarActivity open;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        open = this;
        Ui.immersive(this);
        u = new Ui(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(0xB8030406);

        dialog = new LinearLayout(this);
        dialog.setOrientation(LinearLayout.VERTICAL);
        dialog.setBackground(u.box(0xFF10121A, 24, 0x1AFFFFFF, 1));
        dialog.setPadding(u.px(34), u.px(30), u.px(34), u.px(26));
        final boolean auto = getIntent() != null && getIntent().getBooleanExtra("auto", false);
        TextView title = u.text(auto ? "La caja se va a apagar" : "¿Qué quieres hacer?", true, 32, Ui.WHITE);
        dialog.addView(title);
        final TextView sub = u.text(auto ? "Se acabó el temporizador. Se apaga en 60 s." : "Tus apps quedarán tal como están.", false, 17, 0xFFB6BAC6);
        LinearLayout.LayoutParams ls = new LinearLayout.LayoutParams(-2, -2);
        ls.topMargin = u.px(8); ls.bottomMargin = u.px(22);
        dialog.addView(sub, ls);
        if (auto) {
            dialog.addView(option("Seguir viendo", "cancela el apagado", new Runnable() { public void run() { finish(); } }));
            dialog.addView(option("Apagar ya", "con el viaje de despedida", new Runnable() { public void run() { startShutdown(); } }));
            ui.postDelayed(new Runnable() {
                int left = 60;
                public void run() {
                    if (playing || isFinishing()) return;
                    if (--left <= 0) { startShutdown(); return; }
                    sub.setText("Se acabó el temporizador. Se apaga en " + left + " s.");
                    ui.postDelayed(this, 1000);
                }
            }, 1000);
        } else {
            dialog.addView(option("Apagar", "con el viaje de despedida", new Runnable() { public void run() { startShutdown(); } }));
            dialog.addView(option("Cancelar", "seguir viendo", new Runnable() { public void run() { finish(); } }));
        }
        FrameLayout.LayoutParams ld = new FrameLayout.LayoutParams(u.px(560), -2, Gravity.CENTER);
        root.addView(dialog, ld);
        setContentView(root);
        dialog.setAlpha(0f); dialog.setScaleX(0.94f); dialog.setScaleY(0.94f);
        dialog.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320).start();
        play(R.raw.pregunta);
        if (dialog.getChildCount() > 2) dialog.getChildAt(2).requestFocus();
    }

    View option(String label, String hint, final Runnable act) {
        final LinearLayout o = new LinearLayout(this);
        o.setOrientation(LinearLayout.HORIZONTAL);
        o.setGravity(Gravity.CENTER_VERTICAL);
        o.setFocusable(true);
        o.setPadding(u.px(20), u.px(16), u.px(20), u.px(16));
        final TextView l = u.text(label, true, 19, Ui.WHITE);
        final TextView h = u.text(hint, false, 15, 0xFF8D92A3);
        h.setGravity(Gravity.END);
        h.setSingleLine(true); h.setEllipsize(TextUtils.TruncateAt.END);
        o.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        o.addView(h);
        o.setBackground(u.box(0x12FFFFFF, 14, 0, 0));
        o.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                v.setBackground(u.box(has ? Ui.WHITE : 0x12FFFFFF, 14, 0, 0));
                l.setTextColor(has ? 0xFF07080D : Ui.WHITE);
                h.setTextColor(has ? 0xFF4A4E5C : 0xFF8D92A3);
            }
        });
        o.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { act.run(); } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = u.px(10);
        o.setLayoutParams(lp);
        return o;
    }

    void play(int res) {
        try {
            MediaPlayer mp = MediaPlayer.create(this, res);
            if (mp == null) return;
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer m) { m.release(); }
            });
            mp.start();
        } catch (Exception ignored) {}
    }

    void startShutdown() {
        if (playing) return;
        playing = true;
        root.removeAllViews();
        root.setBackgroundColor(0xFF000000);
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.apagado));
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override public void onCompletion(MediaPlayer mp) { sleepNow(); }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override public boolean onError(MediaPlayer mp, int w, int e) { sleepNow(); return true; }
        });
        video.start();
        ui.postDelayed(new Runnable() { public void run() { sleepNow(); } }, 7000); // safety net
    }

    boolean slept = false;
    void sleepNow() {
        if (slept) return;
        slept = true;
        BotonesService.standby();
        ui.postDelayed(new Runnable() { public void run() { finish(); } }, 800);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (playing) return true;   // the farewell can't be interrupted
        return super.onKeyDown(code, e);
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        if (open == this) open = null;
        super.onDestroy();
    }
}
