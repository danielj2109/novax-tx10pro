#!/bin/bash
set -e
cd "$(dirname "$0")"
BT=${BUILD_TOOLS:?ruta a build-tools}
AJ=${ANDROID_JAR:?ruta a platforms/android-29/android.jar}
rm -rf build && mkdir -p build/gen build/obj build/res build/apk
$BT/aapt2 compile --dir res -o build/res/res.zip
$BT/aapt2 link -o build/base.apk -I $AJ --manifest AndroidManifest.xml -A assets \
  --java build/gen build/res/res.zip --min-sdk-version 21 --target-sdk-version 28 --version-code 32 --version-name 3.2
javac -source 8 -target 8 -encoding UTF-8 -nowarn -bootclasspath $AJ -classpath $AJ -d build/obj \
  $(find src build/gen -name '*.java') 2>&1 | grep -v "bootstrap classpath\|^warning\|^1 warning\|source value 8\|target value 8\|To suppress" || true
ls build/obj/com/novax/launcher/MainActivity.class >/dev/null
$BT/dx --dex --min-sdk-version=21 --output=build/apk/classes.dex build/obj
cp build/base.apk build/unsigned.apk
(cd build/apk && zip -q -u ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 build/unsigned.apk build/aligned.apk
# Your own signing key (never commit it). Set KEYSTORE / KEYSTORE_PASS to reuse one; otherwise a local one is created.
KS=${KEYSTORE:-novax-local.keystore}
KP=${KEYSTORE_PASS:-cambiame}
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$KP" -keypass "$KP" -alias novax \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=NOVA X" >/dev/null 2>&1
$BT/apksigner sign --ks "$KS" --ks-pass pass:"$KP" --key-pass pass:"$KP" --out build/novax.apk build/aligned.apk
$BT/apksigner verify build/novax.apk && ls -la build/novax.apk
