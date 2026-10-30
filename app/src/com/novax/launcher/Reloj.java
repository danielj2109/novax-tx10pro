package com.novax.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * Guardián del reloj. Esta caja no tiene pila de reloj: cuando se apaga del todo, al prender cree que es
 * septiembre de 2024. Con la fecha mala fallan las conexiones seguras (YouTube, Hydra…), y si el DNS privado
 * (AdGuard) está prendido ni siquiera puede preguntar la hora: se queda trabada.
 * Aquí: hora automática siempre activa; si la fecha está mal, se pausa AdGuard, se fuerza la sincronización
 * y, cuando la hora queda bien, AdGuard vuelve solo.
 */
public class Reloj {

    static final String TAG = "NOVAXRELOJ";
    static final long MINIMO = 1790812800000L;      // 1 oct 2026: cualquier fecha anterior está mal
    static final Handler ui = new Handler(Looper.getMainLooper());
    static boolean vigilando = false;

    static boolean mal() { return System.currentTimeMillis() < MINIMO; }

    static void put(ContentResolver cr, String k, String v) {
        try { Settings.Global.putString(cr, k, v); } catch (Exception e) { Log.w(TAG, k + ": " + e); }
    }

    static void vigilar(final Context c) {
        final ContentResolver cr = c.getContentResolver();
        put(cr, Settings.Global.AUTO_TIME, "1");
        put(cr, Settings.Global.AUTO_TIME_ZONE, "1");
        if (!mal()) { restaurarDns(cr); return; }
        if ("hostname".equals(Settings.Global.getString(cr, "private_dns_mode"))) {
            put(cr, "novax_dns_pausado", "1");
            put(cr, "private_dns_mode", "opportunistic");
            Log.i(TAG, "fecha mala: AdGuard en pausa");
        }
        if (vigilando) return;
        vigilando = true;
        ui.post(new Runnable() {
            int vueltas = 0;
            @Override public void run() {
                if (!mal()) {
                    vigilando = false;
                    Log.i(TAG, "hora corregida");
                    restaurarDns(cr);
                    return;
                }
                if (vueltas % 3 == 0) {          // cada 30 s: apagar y prender la hora automática fuerza a pedir la hora
                    put(cr, Settings.Global.AUTO_TIME, "0");
                    put(cr, Settings.Global.AUTO_TIME, "1");
                }
                if (++vueltas < 120) ui.postDelayed(this, 10000);   // hasta 20 minutos
                else vigilando = false;
            }
        });
    }

    /** Reloj de respaldo: guarda la hora (segundos) en el almacenamiento que el sistema puede leer al arrancar,
     *  /data/user_de/0/com.novax.launcher/files/hora.txt. El script /system/etc/novax_hora.sh la recupera tras un corte. */
    static void guardar(Context c) {
        if (mal()) return;                       // nunca guardar una fecha equivocada
        try {
            java.io.File dir = c.createDeviceProtectedStorageContext().getFilesDir();
            java.io.File tmp = new java.io.File(dir, "hora.tmp"), f = new java.io.File(dir, "hora.txt");
            java.io.FileOutputStream o = new java.io.FileOutputStream(tmp);
            o.write(String.valueOf(System.currentTimeMillis() / 1000).getBytes());
            o.getFD().sync(); o.close();
            tmp.renameTo(f);
            f.setReadable(true, false);
            dir.setExecutable(true, false);
            if (dir.getParentFile() != null) dir.getParentFile().setExecutable(true, false);
        } catch (Exception e) { Log.w(TAG, "guardar: " + e); }
    }

    static final Runnable cadaMinuto = new Runnable() {
        @Override public void run() {
            if (BotonesService.instance != null) guardar(BotonesService.instance);
            ui.postDelayed(this, 60000);
        }
    };

    static void restaurarDns(ContentResolver cr) {
        if ("1".equals(Settings.Global.getString(cr, "novax_dns_pausado"))) {
            put(cr, "private_dns_mode", "hostname");
            put(cr, "novax_dns_pausado", null);
            Log.i(TAG, "AdGuard de vuelta");
        }
    }
}
