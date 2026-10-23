#!/system/bin/sh
# NOVA X - busca malware. Solo lee.
# Salida: MALWARE=<paquete> para cada uno encontrado, SISTEMA=<carpeta> si el instalador vive en /system,
#         SOSPECHOSA=<paquete> para apps con poder de sistema fuera de la lista conocida.
L=/data/local/tmp/novax/lista_malware.txt
KNOWN=$(grep -v '^#' $L | tr '\n' ' ')
INSTALLED=$(pm list packages | cut -d: -f2)
for P in $KNOWN; do echo "$INSTALLED" | grep -qx "$P" && echo "MALWARE=$P"; done
# apps instaladas POR un paquete de la lista (descendientes)
for P in $(pm list packages -3 | cut -d: -f2); do
  I=$(dumpsys package $P | grep -m1 installerPackageName= | sed 's/.*=//')
  case " $KNOWN " in *" $I "*) case " $KNOWN " in *" $P "*) ;; *) echo "MALWARE=$P";; esac;; esac
done
# el instalador escondido en el sistema de fabrica
for D in /system/app/netserevice-release /system/app/netservice-release /system/priv-app/netserevice-release; do
  [ -d "$D" ] && echo "SISTEMA=$D"
done
# apps con identidad de sistema instaladas en /data que no conocemos
pm list packages -f -U | grep "uid:1000" | grep "/data/app/" | sed -E 's/.*=([^ ]+) uid:1000/\1/' | while read P; do
  case " $KNOWN org.cyanogenmod.appdrawer " in *" $P "*) ;; *) echo "SOSPECHOSA=$P";; esac
done
