#!/system/bin/sh
# NOVA X - deja la caja como venia (menos el malware, que no se reinstala).
B=/data/local/tmp/novax_respaldo; T=/data/local/tmp/novax; K=/system/usr/keylayout/sunxi-ir-uinput.kl
rm -f /data/local/boot.mp4 && echo "ANIMACION=original"
if [ -f $B/sunxi-ir-uinput.kl ] && [ "$(wc -c < $B/sunxi-ir-uinput.kl)" = "$(wc -c < $K)" ]; then
  blockdev --setrw /dev/block/dm-0 2>/dev/null
  nsenter -t 1 -m -- mount -o remount,rw / && dd if=$B/sunxi-ir-uinput.kl of=$K conv=notrunc 2>/dev/null; sync
  nsenter -t 1 -m -- mount -o remount,ro / 2>/dev/null
  cmp -s $B/sunxi-ir-uinput.kl $K && echo "BOTONES=original"
fi
mkdir -p $T/bl
if [ -f $B/bootlogo.bmp ] && mount -t vfat -o rw /dev/block/by-name/bootloader $T/bl 2>/dev/null; then
  cat $B/bootlogo.bmp > $T/bl/bootlogo.bmp; sync; umount $T/bl; echo "LOGO=original"
fi
pm enable com.google.android.tvlauncher >/dev/null 2>&1 && echo "INICIO_GOOGLE=activado"
settings put global private_dns_mode opportunistic
settings delete global novax_key_ajustes
settings delete global novax_netflix_app
sync
