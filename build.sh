#!/bin/bash

set -euo pipefail

APP_NAME="Stario"
SHOULD_SIGN=false
GRADLE_SIGNING_PROPS=()

# Signed build
KEYSTORE_PATH=""
KEYSTORE_PASSWORD=""
KEY_ALIAS=""
KEY_PASSWORD=""

usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -K, --keystore-path      Path to your keystore (.jks) file."
    echo "  -P, --keystore-password  Password for the keystore."
    echo "  -a, --key-alias          Alias for the key."
    echo "  -p, --key-password       Password for the key alias. If same as keystore password, you must still provide it."
    echo "  -h, --help               Display this help message and exit."
    exit 1
}

# Parse CLI args
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -K|--keystore-path)
            KEYSTORE_PATH="$2"
            shift
            ;;
        -P|--keystore-password)
            KEYSTORE_PASSWORD="$2"
            shift
            ;;
        -a|--key-alias)
            KEY_ALIAS="$2"
            shift
            ;;
        -p|--key-password)
            KEY_PASSWORD="$2"
            shift
            ;;
        -h|--help)
            usage
            ;;
        *)
            usage
            ;;
    esac
    shift
done

# Unix timestamp of last commit
export SOURCE_DATE_EPOCH=$(git log -1 --format=%at)

echo ""
echo "Building with SOURCE_DATE_EPOCH=$SOURCE_DATE_EPOCH"
echo ""

if [ -n "${KEYSTORE_PATH:-}" ] && \
   [ -n "${KEYSTORE_PASSWORD:-}" ] && \
   [ -n "${KEY_ALIAS:-}" ] && \
   [ -n "${KEY_PASSWORD:-}" ]; then

    if [ ! -f "$KEYSTORE_PATH" ]; then
        echo "Error: Keystore file not found at: $KEYSTORE_PATH"
        exit 1
    fi

    echo "All signing credentials provided. Proceeding with a signed build."

    GRADLE_SIGNING_PROPS+=("-PkeystorePath=$(realpath "$KEYSTORE_PATH")")
    GRADLE_SIGNING_PROPS+=("-PkeystorePassword=$KEYSTORE_PASSWORD")
    GRADLE_SIGNING_PROPS+=("-PkeyAlias=$KEY_ALIAS")
    GRADLE_SIGNING_PROPS+=("-PkeyPassword=$KEY_PASSWORD")
    SHOULD_SIGN=true
else
    echo "One or more signing credentials were not provided. Skipping signed release builds."
    echo "To create a signed build, provide all four options: --keystore-path, --keystore-password, --key-alias, and --key-password."
fi

echo ""
echo "Starting Gradle Builds..."
echo ""

./gradlew clean

# Debug APK
echo ""
echo "Building Debug APK..."
echo ""
./gradlew assembleDebug

# Unsigned Release APK
echo ""
echo "Building Release APK..."
echo ""
./gradlew assembleRelease

echo ""
echo "Gathering unsigned builds..."
echo ""

# Get build details
APP_VERSION_NAME=$(./gradlew -q :app:getVersionName)

if [ -z "$APP_VERSION_NAME" ]; then
    echo "WARNING: Could not determine app version name from Gradle. Using 'undefined'."
    APP_VERSION_NAME="undefined"
fi

FINAL_BUILD_DIR="build/v${APP_VERSION_NAME}"
mkdir -p "$FINAL_BUILD_DIR"

# Debug APK
DEBUG_APK_SOURCE="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$DEBUG_APK_SOURCE" ]; then
    cp "$DEBUG_APK_SOURCE" "${FINAL_BUILD_DIR}/${APP_NAME}-v${APP_VERSION_NAME}-debug.apk"
else
    echo "Debug APK not found at $DEBUG_APK_SOURCE"
fi

# Unsigned Release APK
UNSIGNED_APK_SOURCE="app/build/outputs/apk/release/app-release-unsigned.apk"
if [ -f "$UNSIGNED_APK_SOURCE" ]; then
    cp "$UNSIGNED_APK_SOURCE" "${FINAL_BUILD_DIR}/${APP_NAME}-v${APP_VERSION_NAME}-unsigned.apk"
else
    echo "Unsigned release APK not found at $UNSIGNED_APK_SOURCE"
fi

if [ "${SHOULD_SIGN}" = true ]; then
  # Signed Release AAB
  echo ""
  echo "Building Release AAB..."
  echo ""
  ./gradlew bundleRelease "${GRADLE_SIGNING_PROPS[@]}"

  # Signed Release APK
  echo ""
  echo "Building Release APK..."
  echo ""
  ./gradlew assembleRelease "${GRADLE_SIGNING_PROPS[@]}"

  echo ""
  echo "Gathering signed builds..."
  echo ""

  # Signed Release AAB
  SIGNED_AAB_SOURCE="app/build/outputs/bundle/release/app-release.aab"
  if [ -f "$SIGNED_AAB_SOURCE" ]; then
      cp "$SIGNED_AAB_SOURCE" "${FINAL_BUILD_DIR}/${APP_NAME}-v${APP_VERSION_NAME}.aab"
  else
      echo "Signed release AAB not found at $SIGNED_AAB_SOURCE"
  fi

  # Signed Release APK
  SIGNED_APK_SOURCE="app/build/outputs/apk/release/app-release.apk"
  if [ -f "$SIGNED_APK_SOURCE" ]; then
      cp "$SIGNED_APK_SOURCE" "${FINAL_BUILD_DIR}/${APP_NAME}-v${APP_VERSION_NAME}.apk"
  else
      echo "Signed release APK not found at $SIGNED_APK_SOURCE"
  fi
else
  echo "Skipping signed release builds."
fi

echo ""
echo "Build process finished."
echo "All artifacts copied to: $FINAL_BUILD_DIR"