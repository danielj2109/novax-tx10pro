#!/system/bin/sh
# NOVA X - partes que necesitan root pero NO tocan el sistema: la animacion de arranque va en /data.
T=/data/local/tmp/novax
if [ -f $T/arranque.mp4 ]; then
  cp $T/arranque.mp4 /data/local/boot.mp4 && chmod 644 /data/local/boot.mp4 && echo "ANIMACION=ok"
fi
