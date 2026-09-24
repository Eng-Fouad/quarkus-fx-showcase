#!/usr/bin/env bash
# Regenerates the binary test assets of the showcase (images, audio, video) with local macOS tools only.
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA_HOME=${JAVA_HOME:-$HOME/.sdkman/candidates/java/25-graalce}
RES=src/main/resources/showcase

"$JAVA_HOME/bin/java" tools/GenImages.java "$RES/images"

TMP=$(mktemp -d)
say -o "$TMP/hello.aiff" "Hello from Quarkus FX. JavaFX in native mode."
cp "$TMP/hello.aiff" "$RES/media/hello.aiff"
afconvert -f WAVE -d LEI16@22050 "$TMP/hello.aiff" "$RES/media/hello.wav"
afconvert -f m4af -d aac "$TMP/hello.aiff" "$RES/media/hello.m4a"
rm -rf "$TMP"

swift tools/GenVideo.swift "$RES/media/clip.mp4"
ls -la "$RES/images" "$RES/media"
