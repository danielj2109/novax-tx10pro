package com.novax.launcher;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class Ui {
    static final int GOLD = 0xFFE6C35C;
    static final int WHITE = 0xFFF4F5F8;
    static final int SOFT = 0xFFDFE1E8;
    static final int MUTED = 0xFFA9ADBB;
    static final int BG = 0xFF050608;
    static final int RED = 0xFFFF6B5E;

    static Typeface med, light;
    final Activity a;
    final float k;

    Ui(Activity a) {
        this.a = a;
        k = a.getResources().getDisplayMetrics().widthPixels / 1280f;
        if (med == null) {
            try {
                med = Typeface.createFromAsset(a.getAssets(), "fonts/medium.otf");
                light = Typeface.createFromAsset(a.getAssets(), "fonts/light.otf");
            } catch (Exception e) {
                med = Typeface.create("sans-serif-medium", Typeface.NORMAL);
                light = Typeface.create("sans-serif-light", Typeface.NORMAL);
            }
        }
    }

    int px(float v) { return Math.round(v * k); }

    TextView text(String s, boolean medium, float size, int color) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTypeface(medium ? med : light);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    GradientDrawable box(int fill, float radius, int strokeColor, float stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(radius));
        if (stroke > 0) g.setStroke(Math.max(1, px(stroke)), strokeColor);
        return g;
    }

    static void immersive(Activity a) {
        Window w = a.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
