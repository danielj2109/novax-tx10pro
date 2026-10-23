#!/system/bin/sh
# NOVA X - permisos y ajustes despues de instalar la app. Corre como usuario "shell" (adb), no necesita root.
N=com.novax.launcher
for p in android.permission.WRITE_SECURE_SETTINGS android.permission.CHANGE_CONFIGURATION \
         android.permission.ACCESS_FINE_LOCATION android.permission.ACCESS_COARSE_LOCATION; do
  pm grant $N $p 2>&1 | grep -v "^$" | sed "s/^/  $p: /"
done
appops set $N WRITE_SETTINGS allow
appops set $N GET_USAGE_STATS allow
# Android 10 esconde las redes WiFi a las apps si la "ubicacion" esta apagada
settings put secure location_mode 3
# botones y volumen del control
settings put secure enabled_accessibility_services $N/$N.BotonesService
settings put secure accessibility_enabled 1
# pantalla de inicio: NOVA X (el inicio de Google queda desactivado, se puede restaurar)
cmd package set-home-activity $N/.MainActivity
pm disable-user --user 0 com.google.android.tvlauncher >/dev/null 2>&1
# permiso para apagar la pantalla
dpm set-active-admin $N/.Apagado >/dev/null 2>&1
# bloqueo de anuncios y rastreo (AdGuard DNS)
settings put global private_dns_specifier dns.adguard-dns.com
settings put global private_dns_mode hostname
# sin reposo automatico (en estas cajas suele dejarlas pegadas)
settings put system screen_off_timeout 2147483647
settings put secure screensaver_enabled 0
settings put secure screensaver_activate_on_sleep 0
settings put global stay_on_while_plugged_in 7
echo "AJUSTES=ok"
echo "VERSION=$(dumpsys package $N | grep -m1 versionName | sed 's/.*=//')"
