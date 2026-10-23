package com.novax.launcher;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.VideoView;

/** Plays the NOVA X intro every time the box wakes up, then lands on the NOVA X home. */
public class EncenderActivity extends Activity {

    final Handler ui = new Handler(Looper.getMainLooper());
    boolean done = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.immersive(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        VideoView v = new VideoView(this);
        root.addView(v, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        setContentView(root);
        v.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.encender));
        v.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override public void onCompletion(MediaPlayer mp) { home(); }
        });
        v.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override public boolean onError(MediaPlayer mp, int w, int e) { home(); return true; }
        });
        v.start();
        ui.postDelayed(new Runnable() { public void run() { home(); } }, 6000);   // safety net
    }

    void home() {
        if (done) return;
        done = true;
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setClass(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        } catch (Exception ignored) {}
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) { home(); return true; }
        return true;
    }

    @Override protected void onDestroy() { ui.removeCallbacksAndMessages(null); super.onDestroy(); }
}
