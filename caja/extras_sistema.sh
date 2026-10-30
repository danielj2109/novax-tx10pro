#!/system/bin/sh
# NOVA X - EXTRAS (avanzado): logo de encendido, botones de encendido/mouse y reloj de respaldo.
# Hace respaldo de todo en /data/local/tmp/novax_respaldo antes de cambiar.
# Los cambios del control se escriben "en el mismo lugar" con el mismo tamano,
# para no fallar aunque el sistema este lleno (eso dejo un control sin funcionar en las pruebas).
T=/data/local/tmp/novax; B=/data/local/tmp/novax_respaldo; mkdir -p $B
K=/system/usr/keylayout/sunxi-ir-uinput.kl

# ---------- 1. logo de encendido (particion de arranque, archivo FAT) ----------
mkdir -p $T/bl
if mount -t vfat -o rw /dev/block/by-name/bootloader $T/bl 2>/dev/null; then
  if [ -f $T/bl/bootlogo.bmp ] && [ -f $T/bootlogo.bmp ]; then
    [ -f $B/bootlogo.bmp ] || cp $T/bl/bootlogo.bmp $B/bootlogo.bmp
    if [ "$(wc -c < $T/bootlogo.bmp)" = "$(wc -c < $T/bl/bootlogo.bmp)" ]; then
      cat $T/bootlogo.bmp > $T/bl/bootlogo.bmp; sync
      cmp -s $T/bootlogo.bmp $T/bl/bootlogo.bmp && echo "LOGO=ok" || echo "LOGO=error"
    else echo "LOGO=omitido (el logo de esta caja tiene otro tamano)"; fi
  fi
  umount $T/bl
else echo "LOGO=omitido (no se encontro la particion)"; fi

# ---------- 2. botones del control (encendido -> NOVA X, mouse -> Ajustes) ----------
if [ -f $K ]; then
  [ -f $B/sunxi-ir-uinput.kl ] || cp $K $B/sunxi-ir-uinput.kl
  # mismo largo exacto: "key  116  POWER" (15) -> "key 116  STEM_1" (15); "key  232  MOUSE" -> "key 232  STEM_2"
  sed -e 's/^key  116  POWER$/key 116  STEM_1/' -e 's/^key  232  MOUSE$/key 232  STEM_2/' $K > $T/kl_nuevo.kl
  if [ "$(wc -c < $T/kl_nuevo.kl)" != "$(wc -c < $K)" ]; then
    echo "BOTONES=omitido (el archivo del control de esta caja es distinto)"
  elif cmp -s $T/kl_nuevo.kl $K; then
    echo "BOTONES=ya estaban"
  elif ! grep -q "^key 116  STEM_1$" $T/kl_nuevo.kl; then
    echo "BOTONES=omitido (no encontre el boton de encendido esperado)"
  else
    blockdev --setrw /dev/block/dm-0 2>/dev/null
    if nsenter -t 1 -m -- mount -o remount,rw /; then
      dd if=$T/kl_nuevo.kl of=$K conv=notrunc 2>/dev/null; sync
      nsenter -t 1 -m -- mount -o remount,ro / 2>/dev/null
    fi
    if cmp -s $T/kl_nuevo.kl $K; then echo "BOTONES=ok"
    else cat $B/sunxi-ir-uinput.kl > $K 2>/dev/null; echo "BOTONES=error (restaurado)"; fi
  fi
  grep -q "^key 232  STEM_2$" $K && settings put global novax_key_ajustes 266
fi

# ---------- 3. reloj de respaldo (la caja no tiene pila: tras un corte arranca en 2024) ----------
LIBRE=$(( $(stat -f -c %a /) * $(stat -f -c %S /) / 1024 ))
if [ "$LIBRE" -lt 256 ]; then
  echo "RELOJ=omitido (el sistema esta lleno: ${LIBRE} KB libres)"
elif cmp -s $T/novax_hora.sh /system/etc/novax_hora.sh && cmp -s $T/novax_hora.rc /system/etc/init/novax_hora.rc; then
  echo "RELOJ=ya estaba"
else
  blockdev --setrw /dev/block/dm-0 2>/dev/null
  if nsenter -t 1 -m -- mount -o remount,rw /; then
    cat $T/novax_hora.sh > /system/etc/novax_hora.sh && chmod 755 /system/etc/novax_hora.sh
    cat $T/novax_hora.rc > /system/etc/init/novax_hora.rc && chmod 644 /system/etc/init/novax_hora.rc
    chcon u:object_r:system_file:s0 /system/etc/novax_hora.sh /system/etc/init/novax_hora.rc 2>/dev/null
    sync; nsenter -t 1 -m -- mount -o remount,ro / 2>/dev/null
  fi
  if cmp -s $T/novax_hora.sh /system/etc/novax_hora.sh && cmp -s $T/novax_hora.rc /system/etc/init/novax_hora.rc; then echo "RELOJ=ok"
  else rm -f /system/etc/init/novax_hora.rc 2>/dev/null; echo "RELOJ=error"; fi
fi
sync
