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
export JAVA_TOOL_OPTIONS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xmx1024m"

mkdir -p build/compiled_res build/gen build/obj build/apk

# 1. Resource compilation (incremental + no-crunch for speed)
if [ ! -f build/resources.zip ] || [ -n "$(find res -newer build/resources.zip 2>/dev/null)" ]; then
  echo "=== [1/5] Compiling resources with aapt2 ==="
  aapt2 compile --dir res -o build/resources.zip
else
  echo "=== [1/5] Resources up-to-date (cached) ==="
fi

# 2. Resource linking & R.java generation
echo "=== [2/5] Linking resources with aapt2 link ==="
aapt2 link build/resources.zip \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  --java build/gen \
  --custom-package tv.bootv.app \
  -o build/unaligned.apk \
  --auto-add-overlay

# 3. Fast Java Compilation (Client C1 compiler + disabled annotation processing)
echo "=== [3/5] Compiling Java code (accelerated) ==="
javac -J-XX:+TieredCompilation -J-XX:TieredStopAtLevel=1 \
  -source 8 -target 8 -proc:none \
  -bootclasspath "$ANDROID_JAR" \
  -cp "$ANDROID_JAR" \
  -d build/obj \
  $(find build/gen src -name "*.java")

# 4. Multi-threaded Dexing with d8
echo "=== [4/5] Dexing bytecode with d8 (8 threads) ==="
d8 --min-api 21 --release --thread-count 8 --lib "$ANDROID_JAR" \
  --output build/apk \
  $(find build/obj -name "*.class")

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

# Direct deployment to Download folder
DOWNLOAD_DIR="/storage/emulated/0/Download"
if [ -d "$DOWNLOAD_DIR" ]; then
  (rm -f "$DOWNLOAD_DIR/BOOTV.apk" 2>/dev/null || true)
  cp -f BOOTV.apk "$DOWNLOAD_DIR/BOOTV.apk" 2>/dev/null || true
  echo "=== Deployed to: $DOWNLOAD_DIR/BOOTV.apk ==="
fi

if grep -q "tv.bootv.app.beta" AndroidManifest.xml; then
  cp -f BOOTV.apk BOOTV_beta.apk
  if [ -d "$DOWNLOAD_DIR" ]; then
    (rm -f "$DOWNLOAD_DIR/BOOTV_beta.apk" 2>/dev/null || true)
    cp -f BOOTV.apk "$DOWNLOAD_DIR/BOOTV_beta.apk" 2>/dev/null || true
    echo "=== Deployed to: $DOWNLOAD_DIR/BOOTV_beta.apk ==="
  fi
fi

END_TIME=$(date +%s%N 2>/dev/null || date +%s)
if [ ${#START_TIME} -gt 10 ] && [ ${#END_TIME} -gt 10 ]; then
  DIFF_MS=$(( (END_TIME - START_TIME) / 1000000 ))
  echo "=== Build finished in ${DIFF_MS}ms ==="
fi

echo "=== APK Built Successfully: $WORKSPACE_DIR/BOOTV.apk ==="
ls -lh "$WORKSPACE_DIR/BOOTV.apk"
