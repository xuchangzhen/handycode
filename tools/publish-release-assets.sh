#!/bin/bash
# Upload both the public branded assets and the old filename required by existing clients.
set -euo pipefail
project=$(cd "$(dirname "$0")/.." && pwd)
cd "$project"
version=$(python3 -c 'import xml.etree.ElementTree as e; print(e.parse("app/src/main/AndroidManifest.xml").getroot().attrib["{http://schemas.android.com/apk/res/android}versionName"])')
tag="v$version"
gh release view "$tag" --repo xuchangzhen/handycode >/dev/null
for file in "YishiBaoguo-$version.apk" "YishiBaoguo-$version.apk.sha256" "HandyCode-$version-release.apk" "HandyCode-$version-release.apk.sha256"; do
  if [ ! -f "dist/$file" ]; then printf 'Missing dist/%s; build the release first.\n' "$file" >&2; exit 1; fi
done
(cd dist && shasum -a 256 -c "YishiBaoguo-$version.apk.sha256" && shasum -a 256 -c "HandyCode-$version-release.apk.sha256")
gh release upload "$tag" --repo xuchangzhen/handycode \
  "dist/YishiBaoguo-$version.apk#驿拾包裹-$version.apk" \
  "dist/YishiBaoguo-$version.apk.sha256#驿拾包裹-$version.apk SHA-256 校验" \
  "dist/HandyCode-$version-release.apk#旧版应用内更新兼容包（无需手动下载）" \
  "dist/HandyCode-$version-release.apk.sha256#旧版兼容包 SHA-256 校验"
