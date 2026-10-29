#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.13"
if command -v gradle >/dev/null 2>&1 && gradle --version 2>/dev/null | grep -q '^Gradle 8\.13'; then
  exec gradle "$@"
fi

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
GRADLE_HOME_LOCAL="$GRADLE_USER_HOME/wrapper/dists/gradle-${GRADLE_VERSION}-bin/manual/gradle-${GRADLE_VERSION}"
if [ ! -x "$GRADLE_HOME_LOCAL/bin/gradle" ]; then
  mkdir -p "$(dirname "$GRADLE_HOME_LOCAL")"
  ARCHIVE="$(dirname "$GRADLE_HOME_LOCAL")/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  else
    echo "Please install Gradle ${GRADLE_VERSION} or curl/wget." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ARCHIVE" -d "$(dirname "$GRADLE_HOME_LOCAL")"
  else
    echo "Please install unzip to extract Gradle." >&2
    exit 1
  fi
fi
exec "$GRADLE_HOME_LOCAL/bin/gradle" "$@"
