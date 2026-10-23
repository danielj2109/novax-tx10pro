#!/system/bin/sh
# NOVA X - saca del sistema de fabrica la carpeta del instalador de malware (con copia de respaldo).
# Uso (como root): sh sacar_del_sistema.sh /system/app/netserevice-release
D="$1"; Q=/data/local/tmp/novax_cuarentena
case "$D" in /system/app/netserevice-release|/system/app/netservice-release|/system/priv-app/netserevice-release) ;; *) echo "RUTA NO PERMITIDA: $D"; exit 1;; esac
[ -d "$D" ] || { echo "YA NO ESTA: $D"; exit 0; }
mkdir -p $Q && cp -r "$D" $Q/ || { echo "NO SE PUDO RESPALDAR"; exit 1; }
B=$(basename "$D")
for f in "$D"/*.apk; do cmp -s "$f" "$Q/$B/$(basename "$f")" || { echo "RESPALDO NO COINCIDE"; exit 1; }; done
blockdev --setrw /dev/block/dm-0 2>/dev/null
nsenter -t 1 -m -- mount -o remount,rw / || { echo "NO SE PUDO ABRIR EL SISTEMA"; exit 1; }
rm -rf "$D"; sync
nsenter -t 1 -m -- mount -o remount,ro / 2>/dev/null
[ -d "$D" ] && echo "SIGUE: $D" || echo "SACADA: $D (respaldo en $Q/$B)"
