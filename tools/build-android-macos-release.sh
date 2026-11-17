#!/bin/bash
# Signed release build; key and password remain outside the repository.
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
sign_dir="${HANDYCODE_SIGNING_DIR:-$HOME/Library/Application Support/HandyCode/Signing}"
key="$sign_dir/release.keystore"
if [ ! -f "$key" ] || [ ! -f "$sign_dir/password.txt" ]; then printf 'Release signing key or password is missing.\n' >&2; exit 1; fi
export HANDYCODE_RELEASE_PASSWORD="$(cat "$sign_dir/password.txt")"
actual=$(keytool -exportcert -keystore "$key" -storepass:env HANDYCODE_RELEASE_PASSWORD -alias handycode-release 2>/dev/null | shasum -a 256 | awk '{print $1}')
expected=$(tr -d '[:space:]' < release-signing-cert.txt)
if [ "$actual" != "$expected" ]; then printf 'Release signing certificate mismatch; refusing to build.\n' >&2; exit 1; fi
version=$(python3 -c 'import xml.etree.ElementTree as e; print(e.parse("app/src/main/AndroidManifest.xml").getroot().attrib["{http://schemas.android.com/apk/res/android}versionName"])')
output="$project/dist/HandyCode-$version-release.apk"
java -jar "$bt/lib/apksigner.jar" sign --ks "$key" --ks-key-alias handycode-release --ks-pass env:HANDYCODE_RELEASE_PASSWORD --key-pass env:HANDYCODE_RELEASE_PASSWORD --out "$output" "$work/aligned.apk"
java -jar "$bt/lib/apksigner.jar" verify --verbose --print-certs "$output"
"$bt/aapt" dump badging "$output" | head -6
named="$project/dist/YishiBaoguo-$version.apk"
cp "$output" "$named"
(cd "$project/dist" && shasum -a 256 "YishiBaoguo-$version.apk" > "YishiBaoguo-$version.apk.sha256")
(cd "$project/dist" && shasum -a 256 "HandyCode-$version-release.apk" > "HandyCode-$version-release.apk.sha256")
shasum -a 256 "$named"
printf 'Release APK: %s\n' "$named"
printf 'Compatibility APK: %s\n' "$output"
