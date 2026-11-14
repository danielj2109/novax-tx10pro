package com.novax.launcher;

import android.content.Context;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * WiFi helpers that need more than a normal app may do.
 * The same class runs two ways:
 *  - inside NOVA X: root(...) starts it again as root with app_process, and probar() tests the internet;
 *  - as root (main): talks straight to Android's WiFi service to pin, forget or list networks.
 */
public class Red {

    // ------------------------------------------------------------------ inside the app

    /** Blocking (call off the UI thread). Runs this class as root and returns what it printed. */
    public static String root(Context c, String... args) {
        StringBuilder cmd = new StringBuilder("CLASSPATH=" + c.getApplicationInfo().sourceDir + " app_process /system/bin " + Red.class.getName());
        for (String a : args) cmd.append(' ').append(a);
        cmd.append(" 2>&1; iw dev wlan0 set power_save off 2>/dev/null");
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "0", "sh", "-c", cmd.toString()});
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) out.append(line).append('\n');
            r.close();
            p.waitFor();
        } catch (Exception e) { out.append("ERROR ").append(e); }
        return out.toString();
    }

    /** Turns off WiFi power saving (it adds big delays on this box's SSV chip). Android turns it back on after every reconnect. */
    public static void sinAhorro() {
        new Thread(new Runnable() { public void run() {
            try { Runtime.getRuntime().exec(new String[]{"su", "0", "sh", "-c", "iw dev wlan0 set power_save off"}).waitFor(); } catch (Exception ignored) {}
        } }).start();
    }

    /** Blocking. Milliseconds to reach the internet, or -1 if there is no internet, -2 if names (DNS) fail. */
    public static int probar() {
        int best = -1;
        for (int i = 0; i < 3; i++) {
            long t = System.currentTimeMillis();
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("8.8.8.8", 53), 4000);
                int ms = (int) (System.currentTimeMillis() - t);
                if (best < 0 || ms < best) best = ms;
            } catch (Exception ignored) {}
        }
        if (best < 0) return -1;
        try { InetAddress.getByName("www.google.com"); } catch (Exception e) { return -2; }
        return best;
    }

    /** Human text for a probar() result. */
    public static String texto(int ms) {
        if (ms == -1) return "No hay internet";
        if (ms == -2) return "Conecta, pero no abre páginas";
        return (ms < 150 ? "Funciona ✓" : ms < 500 ? "Funciona, lento" : "Funciona, muy lento") + " · " + ms + " ms";
    }

    // ------------------------------------------------------------------ as root

    static Object wifi;
    static final String[] PKGS = {"root", "android", "com.android.shell"};

    /** Calls an IWifiManager method by name: ints/booleans come from vals in order, Strings get a package name. */
    static Object call(String name, Object... vals) throws Exception {
        Exception last = null;
        for (Method m : wifi.getClass().getMethods()) {
            if (!m.getName().equals(name)) continue;
            for (String pkg : PKGS) {
                Class<?>[] p = m.getParameterTypes();
                Object[] a = new Object[p.length];
                int v = 0;
                boolean fits = true;
                for (int i = 0; i < p.length; i++) {
                    if (p[i] == String.class) a[i] = pkg;
                    else if (p[i] == int.class || p[i] == boolean.class) {
                        if (v >= vals.length) { fits = false; break; }
                        a[i] = vals[v++];
                    } else a[i] = null;
                }
                if (!fits || v != vals.length) break;
                try { return m.invoke(wifi, a); }
                catch (java.lang.reflect.InvocationTargetException e) { last = (Exception) e.getCause(); }
            }
        }
        if (last != null) throw last;
        throw new NoSuchMethodException(name);
    }

    @SuppressWarnings("unchecked")
    static List<Object> redes() throws Exception {
        // on this box getPrivilegedConfiguredNetworks returns null, the normal one works as root
        for (String n : new String[]{"getConfiguredNetworks", "getPrivilegedConfiguredNetworks"}) {
            try {
                Object r = call(n);
                if (r == null) continue;
                List<Object> l = (List<Object>) r.getClass().getMethod("getList").invoke(r);
                if (l != null && !l.isEmpty()) return l;
            } catch (Exception ignored) {}
        }
        return new ArrayList<>();
    }

    static String ssid(Object c) throws Exception { String s = (String) c.getClass().getField("SSID").get(c); return s == null ? "" : s.replace("\"", ""); }
    static int id(Object c) throws Exception { return c.getClass().getField("networkId").getInt(c); }

    /**
     * fijar ID   : connect to that saved network and stop Android from switching to the others on its own
     * libre      : let Android choose again (enable every saved network)
     * olvidar ID : forget that saved network
     * lista      : print the saved networks
     */
    public static void main(String[] a) {
        try {
            IBinder b = (IBinder) Class.forName("android.os.ServiceManager").getMethod("getService", String.class).invoke(null, "wifi");
            wifi = Class.forName("android.net.wifi.IWifiManager$Stub").getMethod("asInterface", IBinder.class).invoke(null, b);
            String op = a.length > 0 ? a[0] : "lista";
            int target = a.length > 1 ? Integer.parseInt(a[1]) : -1;
            if (op.equals("fijar")) {
                System.out.println("enable " + target + " " + call("enableNetwork", target, true));
                for (Object c : redes()) if (id(c) != target) System.out.println("disable " + ssid(c) + " " + call("disableNetwork", id(c)));
                try { call("reconnect"); } catch (Exception ignored) {}
                System.out.println("OK");
            } else if (op.equals("libre")) {
                for (Object c : redes()) System.out.println("enable " + ssid(c) + " " + call("enableNetwork", id(c), false));
                System.out.println("OK");
            } else if (op.equals("olvidar")) {
                System.out.println("remove " + target + " " + call("removeNetwork", target));
                System.out.println("OK");
            } else {
                for (Object c : redes()) System.out.println("guardada " + id(c) + " " + ssid(c));
                System.out.println("OK");
            }
        } catch (Throwable t) {
            System.out.println("ERROR " + t);
        }
    }
}
