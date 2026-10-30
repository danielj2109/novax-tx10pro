#!/system/bin/sh
# NOVA X - reloj de respaldo: la caja no tiene pila; tras un corte de luz arranca en 2024.
# NOVA X guarda la hora cada minuto; esto la recupera al arrancar (antes de tener internet).
F=/data/user_de/0/com.novax.launcher/files/hora.txt
L=/data/local/tmp/novax_hora.log
[ -f "$F" ] || { echo "$(date) sin hora guardada" >> $L; exit 0; }
T=$(tr -cd 0-9 < "$F")
[ -n "$T" ] || exit 0
NOW=$(date +%s)
if [ "$NOW" -lt "$T" ]; then
  date -u "$(date -u -d @$T +%m%d%H%M%Y.%S)" >/dev/null 2>&1
  echo "$(date) hora recuperada (estaba en $NOW, guardada $T)" >> $L
else
  echo "$(date) la hora ya estaba bien" >> $L
fi
