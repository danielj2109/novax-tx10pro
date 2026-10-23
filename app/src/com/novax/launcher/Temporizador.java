package com.novax.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Sleep timer: after N minutes the box shows a 60 s warning and then powers off completely. */
public class Temporizador extends BroadcastReceiver {

    static final int[] OPCIONES = {0, 30, 60, 90, 120};

    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("novax", Context.MODE_PRIVATE); }

    static PendingIntent pi(Context c) {
        return PendingIntent.getBroadcast(c, 77, new Intent(c, Temporizador.class), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Minutes left, or 0 if no timer is set. */
    static int restante(Context c) {
        long at = prefs(c).getLong("apagar_en", 0);
        if (at <= 0) return 0;
        long left = at - System.currentTimeMillis();
        if (left <= 0) return 0;
        return (int) Math.max(1, (left + 59999) / 60000);
    }

    static int elegido(Context c) { return prefs(c).getInt("apagar_min", 0); }

    static void poner(Context c, int minutes) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi(c));
        if (minutes <= 0) {
            prefs(c).edit().putLong("apagar_en", 0).putInt("apagar_min", 0).apply();
            return;
        }
        long at = System.currentTimeMillis() + minutes * 60000L;
        prefs(c).edit().putLong("apagar_en", at).putInt("apagar_min", minutes).apply();
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c));
    }

    /** Next option in the cycle: No -> 30 -> 60 -> 90 -> 120 -> No */
    static int siguiente(Context c) {
        int cur = restante(c) > 0 ? elegido(c) : 0;
        for (int i = 0; i < OPCIONES.length; i++) if (OPCIONES[i] == cur) return OPCIONES[(i + 1) % OPCIONES.length];
        return OPCIONES[1];
    }

    static String texto(int min) {
        if (min <= 0) return "No";
        if (min < 60) return min + " min";
        int h = min / 60, m = min % 60;
        return h + (h == 1 ? " hora" : " horas") + (m > 0 ? " " + m + " min" : "");
    }

    @Override
    public void onReceive(Context c, Intent in) {
        prefs(c).edit().putLong("apagar_en", 0).putInt("apagar_min", 0).apply();
        Intent a = new Intent(c, ApagarActivity.class).putExtra("auto", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (BotonesService.instance != null) BotonesService.instance.startActivity(a);
            else c.startActivity(a);
        } catch (Exception e) {
            BotonesService.standby();   // could not show the warning: power off anyway
        }
    }
}
