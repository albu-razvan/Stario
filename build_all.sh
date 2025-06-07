#!/bin/bash

set -euo pipefail

APP_NAME="Stario"
SHOULD_SIGN=false
GRADLE_SIGNING_PROPS=()

# Unix timestamp of last commit
export SOURCE_DATE_EPOCH=$(git log -1 --format=%at)

echo ""
echo "Building with SOURCE_DATE_EPOCH=$SOURCE_DATE_EPOCH"
echo ""

# Keystore Path
if [ -n "${KEYSTORE_PATH:-}" ]; then
    export KEYSTORE_PATH
    echo "Using KEYSTORE_PATH from environment variable: $KEYSTORE_PATH"
else
    read -p "Enter path to your keystore.jks [leave empty to skip signing]: " USER_KEYSTORE_PATH
    if [ -z "$USER_KEYSTORE_PATH" ]; then
        unset KEYSTORE_PATH
        echo "Keystore path is not set. Signed AAB/APK will NOT be built."
    else
        export KEYSTORE_PATH="$(realpath "$USER_KEYSTORE_PATH")"
    fi
fi

# Keystore Password
if [ -n "${KEYSTORE_PASSWORD:-}" ]; then
    export KEYSTORE_PASSWORD
    echo "Using KEYSTORE_PASSWORD from environment variable."
else
    if [ -n "${KEYSTORE_PATH:-}" ]; then
        read -s -p "Enter keystore password: " KEYSTORE_PASSWORD_INPUT
        echo ""
        export KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD_INPUT"
    fi
fi

# Key Alias
if [ -n "${KEY_ALIAS:-}" ]; then
    export KEY_ALIAS
    echo "Using KEY_ALIAS from environment variable: $KEY_ALIAS"
else
    if [ -n "${KEYSTORE_PATH:-}" ]; then
        read -p "Enter key alias: " KEY_ALIAS_INPUT
        export KEY_ALIAS="$KEY_ALIAS_INPUT"
    fi
fi

# Key Password
if [ -n "${KEY_PASSWORD:-}" ]; then
    export KEY_PASSWORD
    echo "Using KEY_PASSWORD from environment variable."
else
    if [ -n "${KEYSTORE_PATH:-}" ]; then
        read -s -p "Enter key password: " KEY_PASSWORD_INPUT
        echo ""
        export KEY_PASSWORD="$KEY_PASSWORD_INPUT"
    fi
fi

if [ -n "$KEYSTORE_PATH" ] && \
   [ -n "$KEYSTORE_PASSWORD" ] && \
   [ -n "$KEY_ALIAS" ] && \
   [ -n "$KEY_PASSWORD" ]; then
    GRADLE_SIGNING_PROPS+=("-PkeystorePath=$KEYSTORE_PATH")
    GRADLE_SIGNING_PROPS+=("-PkeystorePassword=$KEYSTORE_PASSWORD")
    GRADLE_SIGNING_PROPS+=("-PkeyAlias=$KEY_ALIAS")
    GRADLE_SIGNING_PROPS+=("-PkeyPassword=$KEY_PASSWORD")
    SHOULD_SIGN=true
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

# Release AAB and Release APK
if [ "$SHOULD_SIGN" = true ]; then
  echo ""
  echo "Building Release AAB..."
  echo ""
  ./gradlew bundleRelease "${GRADLE_SIGNING_PROPS[@]}"

  echo ""
  echo "Building Release APK..."
  echo ""
  ./gradlew assembleRelease "${GRADLE_SIGNING_PROPS[@]}"
else
  echo "Skipping signed release builds; missing credentials."
fi

echo ""
echo "Gathering builds..."
echo ""

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

# Release AAB and Signed Release APK
if [ "$SHOULD_SIGN" = true ]; then
    SIGNED_AAB_SOURCE="app/build/outputs/bundle/release/app-release.aab"
    if [ -f "$SIGNED_AAB_SOURCE" ]; then
        cp "$SIGNED_AAB_SOURCE" "${FINAL_BUILD_DIR}/${APP_NAME}-v${APP_VERSION_NAME}.aab"
    else
        echo "Signed release AAB not found at $SIGNED_AAB_SOURCE"
    fi

    SIGNED_APK_SOURCE="app/build/outputs/apk/release/app-release.apk"
    if [ -f "$SIGNED_APK_SOURCE" ]; then
        cp "$SIGNED_APK_SOURCE" "${FINAL_BUILD_DIR}/${APP_NAME}-v${APP_VERSION_NAME}.apk"
    else
        echo "Signed release APK not found at $SIGNED_APK_SOURCE"
    fi
fi

echo ""
echo "Build process finished."
echo "All artifacts copied to: $FINAL_BUILD_DIR"