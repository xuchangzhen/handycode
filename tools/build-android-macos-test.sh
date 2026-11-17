#!/bin/bash
# Local test build only. Its disposable key cannot update the official release.
set -euo pipefail
project=$(cd "$(dirname "$0")/.." && pwd)
sdk=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}}
bt="$sdk/build-tools/36.0.0"
jar="$sdk/platforms/android-35/android.jar"
work=$(mktemp -d "${TMPDIR:-/tmp}/handycode-test.XXXXXX")
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/classes" "$work/gen" "$work/dex" "$work/tests" "$project/dist" "$project/.local"
cd "$project"
(cd third_party/androidx-webkit-1.13.0 && shasum -a 256 -c site-data-compat.jar.sha256)
node --test tests/*.test.mjs
javac -encoding UTF-8 --release 8 -d "$work/tests" app/src/main/java/com/fairycrane/handycode/{UrlPolicy,PddBundleAdapter,SwitchMotion,LogoutSites,SiteClearQueue,TaobaoLoadGuard,JdUrlPolicy}.java app/src/main/java/com/fairycrane/handycode/logistics/*.java app/src/main/java/com/fairycrane/handycode/update/*.java tests/{TaobaoLoadGuardTest,JdUrlPolicyTest,SiteClearQueueTest,UrlPolicyTest,PddUrlPolicyTest,PddBundleAdapterTest,SwitchMotionTest,PackageCollectionTest,UpdatePolicyTest}.java
for test in UrlPolicyTest PddUrlPolicyTest PddBundleAdapterTest SwitchMotionTest SiteClearQueueTest TaobaoLoadGuardTest JdUrlPolicyTest PackageCollectionTest UpdatePolicyTest; do java -cp "$work/tests" "$test"; done
"$bt/aapt2" compile --dir app/src/main/res -o "$work/resources.zip"
"$bt/aapt2" link -o "$work/base.apk" -I "$jar" --manifest app/src/main/AndroidManifest.xml -A app/src/main/assets "$work/resources.zip" --java "$work/gen" --min-sdk-version 26 --target-sdk-version 35
find app/src/main/java "$work/gen" -name '*.java' > "$work/sources.txt"
javac -encoding UTF-8 --release 8 -classpath "$jar:third_party/androidx-webkit-1.13.0/site-data-compat.jar" -d "$work/classes" @"$work/sources.txt"
find "$work/classes" -name '*.class' > "$work/classes.txt"
java -cp "$bt/lib/d8.jar" com.android.tools.r8.D8 --lib "$jar" --min-api 26 --output "$work/dex" third_party/androidx-webkit-1.13.0/site-data-compat.jar @"$work/classes.txt"
(cd "$work/dex" && zip -q "$work/base.apk" classes*.dex)
"$bt/zipalign" -f 4 "$work/base.apk" "$work/aligned.apk"
key="$project/.local/macos-test-only.keystore"
if [ ! -f "$key" ]; then keytool -genkeypair -keystore "$key" -storepass android -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 365 -dname 'CN=Local Test Only' -noprompt; fi
output="$project/dist/HandyCode-0.9.0-test.apk"
java -jar "$bt/lib/apksigner.jar" sign --ks "$key" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$output" "$work/aligned.apk"
java -jar "$bt/lib/apksigner.jar" verify --verbose "$output"
"$bt/aapt" dump badging "$output" | head -5
shasum -a 256 "$output"
printf 'Test APK only; it cannot replace the officially signed installation: %s\n' "$output"
