#!/usr/bin/env bash

set -euo pipefail

APP_NAME="SideSignal"
PRODUCT_NAME="SideSignal"
BUNDLE_IDENTIFIER="com.sidesignal.SideSignal"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="${ROOT_DIR}/.build/release"
DIST_DIR="${ROOT_DIR}/dist"
APP_DIR="${DIST_DIR}/${APP_NAME}.app"
CONTENTS_DIR="${APP_DIR}/Contents"
MACOS_DIR="${CONTENTS_DIR}/MacOS"

API_BASE_URL="${SIDESIGNAL_API_BASE_URL:-http://localhost:8080/api/v1}"

swift build \
  --package-path "${ROOT_DIR}" \
  -c release

rm -rf "${APP_DIR}"
mkdir -p "${MACOS_DIR}"

cp "${BUILD_DIR}/${PRODUCT_NAME}" "${MACOS_DIR}/${PRODUCT_NAME}"

cat > "${CONTENTS_DIR}/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>${PRODUCT_NAME}</string>
    <key>CFBundleIdentifier</key>
    <string>${BUNDLE_IDENTIFIER}</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundleDisplayName</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>SideSignalAPIBaseURL</key>
    <string>${API_BASE_URL}</string>
</dict>
</plist>
PLIST

codesign --force --deep --sign - "${APP_DIR}"

cd "${DIST_DIR}"
zip -qry "${APP_NAME}.zip" "${APP_NAME}.app"

echo "${APP_DIR}"
echo "${DIST_DIR}/${APP_NAME}.zip"
