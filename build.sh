#!/data/user/0/com.agy/files/usr/bin/bash
set -e

WORKSPACE_DIR="/data/user/0/com.agy/files/Siska"
cd "$WORKSPACE_DIR"

if [ "$1" = "clean" ]; then
  echo "=== Cleaning build artifacts ==="
  rm -rf build BOOTV.apk BOOTV.apk.idsig
  echo "Clean complete."
  exit 0
fi

START_TIME=$(date +%s%N 2>/dev/null || date +%s)

export ANDROID_JAR="/data/user/0/com.agy/files/usr/share/android-sdk/platforms/android-34/android.jar"
export PATH="/data/user/0/com.agy/files/usr/lib/jvm/java-17-openjdk/bin:/data/user/0/com.agy/files/usr/bin:$PATH"

mkdir -p build/compiled_res build/gen build/obj build/apk

# 1. Resource compilation (incremental)
if [ ! -f build/resources.zip ] || [ -n "$(find res -newer build/resources.zip 2>/dev/null)" ]; then
  echo "=== [1/5] Compiling resources with aapt2 ==="
  aapt2 compile --dir res -o build/resources.zip
else
  echo "=== [1/5] Resources up-to-date (cached) ==="
fi

if [ -d res_extra ]; then
  if [ ! -f build/resources_extra.zip ] || [ -n "$(find res_extra -newer build/resources_extra.zip 2>/dev/null)" ]; then
    echo "=== [1/5b] Compiling extra library resources with aapt2 ==="
    aapt2 compile --dir res_extra -o build/resources_extra.zip
  fi
fi

# 2. Resource linking & R.java generation
echo "=== [2/5] Linking resources with aapt2 link ==="
EXTRA_RES_ARG=""
if [ -f build/resources_extra.zip ]; then
  EXTRA_RES_ARG="build/resources_extra.zip"
fi

aapt2 link build/resources.zip $EXTRA_RES_ARG \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  --java build/gen \
  --custom-package tv.bootv.app \
  --extra-packages com.devbrackets.android.exomedia:androidx.media3.exoplayer:androidx.media3.common \
  -o build/unaligned.apk \
  --auto-add-overlay

# 3. Java Compilation
echo "=== [3/5] Compiling Java code ==="
CP="$ANDROID_JAR"
if [ -d libs ] && [ -n "$(ls libs/*.jar 2>/dev/null)" ]; then
  CP="$ANDROID_JAR:libs/*"
fi

javac -source 8 -target 8 -proc:none \
  -bootclasspath "$ANDROID_JAR" \
  -cp "$CP" \
  -d build/obj \
  $(find build/gen src -name "*.java")

# 4. Dexing bytecode with d8
echo "=== [4/5] Dexing bytecode with d8 ==="
LIB_JARS=()
if [ -d libs ] && [ -n "$(ls libs/*.jar 2>/dev/null)" ]; then
  LIB_JARS=(libs/*.jar)
fi

d8 --min-api 21 --lib "$ANDROID_JAR" \
  --output build/apk \
  $(find build/obj -name "*.class") "${LIB_JARS[@]}"

(cd build/apk && zip -u -q ../unaligned.apk classes.dex)

# 5. ZipAlign & V2/V3 Signature
echo "=== [5/5] Aligning and signing APK ==="
zipalign -f -p 4 build/unaligned.apk build/aligned.apk

if [ ! -f debug.keystore ]; then
  keytool -genkey -v -keystore debug.keystore \
    -alias bootv -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android \
    -dname "CN=BOOTV, OU=App, O=BOOTLOOP, L=City, S=State, C=US"
fi

apksigner sign --ks debug.keystore \
  --ks-key-alias siska \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out BOOTV.apk \
  build/aligned.apk

# Verify signature
apksigner verify BOOTV.apk

# Direct deployment to Download folder
DOWNLOAD_DIR="/storage/emulated/0/Download"
if [ -d "$DOWNLOAD_DIR" ]; then
  cp -f BOOTV.apk "$DOWNLOAD_DIR/BOOTV.apk"
  echo "=== Deployed to: $DOWNLOAD_DIR/BOOTV.apk ==="
fi

if grep -q "tv.bootv.app.beta" AndroidManifest.xml; then
  cp -f BOOTV.apk BOOTV_beta.apk
  if [ -d "$DOWNLOAD_DIR" ]; then
    cp -f BOOTV.apk "$DOWNLOAD_DIR/BOOTV_beta.apk"
    echo "=== Deployed to: $DOWNLOAD_DIR/BOOTV_beta.apk ==="
  fi
fi

echo "=== APK Built Successfully: $WORKSPACE_DIR/BOOTV.apk ==="
ls -lh "$WORKSPACE_DIR/BOOTV.apk"
