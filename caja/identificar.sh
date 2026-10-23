#!/system/bin/sh
# NOVA X - identifica la caja. Solo lee, no cambia nada.
# Imprime lineas CLAVE=valor y al final COMPATIBLE=si|no
P(){ getprop "$1"; }
HW=$(P ro.hardware); PLAT=$(P ro.board.platform); FW=$(P ro.product.firmware)
SDK=$(P ro.build.version.sdk); TYPE=$(P ro.build.type); KEYS=$(P ro.build.tags)
echo "MODELO=$(P ro.product.model)"
echo "MODELO_REAL=$HW / $PLAT"
echo "FIRMWARE=$FW"
echo "ANDROID_REAL_SDK=$SDK"
echo "ANDROID_DICE=$(P ro.build.version.release)"
echo "TIPO=$TYPE/$KEYS"
[ -x /system/xbin/su ] && echo "ROOT=si" || echo "ROOT=no"
[ -f /system/usr/keylayout/sunxi-ir-uinput.kl ] && echo "CONTROL_IR=si" || echo "CONTROL_IR=no"
OK=si
case "$HW$PLAT" in *sun50iw9*) ;; *) OK=no; echo "MOTIVO=no es un chip Allwinner H313 (sun50iw9)";; esac
case "$FW" in H313*) ;; *) OK=no; echo "MOTIVO=firmware distinto a H313-Q ($FW)";; esac
[ "$SDK" = "29" ] || { OK=no; echo "MOTIVO=no es Android 10 real (sdk $SDK)"; }
echo "COMPATIBLE=$OK"
