#!/usr/bin/env bash
# Runs the showcase in snapshot mode : every page is rendered to comparison/<label>/<page>.png,
# with checks and errors in comparison/<label>/report.json and the console output in comparison/<label>/run.log.
#
# usage: scripts/snapshot.sh jvm|native [label] [page-id-prefixes]
#   scripts/snapshot.sh jvm
#   scripts/snapshot.sh native
#   scripts/snapshot.sh jvm jvm-controls controls-,data-
set -uo pipefail
cd "$(dirname "$0")/.."

MODE=${1:?usage: snapshot.sh jvm|native [label] [page-id-prefixes]}
LABEL=${2:-$MODE}
PAGES=${3:-}
OUT="comparison/$LABEL"
TIMEOUT=${TIMEOUT:-900}
JAVA_HOME=${JAVA_HOME:-$HOME/.sdkman/candidates/java/25-graalce}

rm -rf "$OUT"
mkdir -p "$OUT"

ARGS=("-Dshowcase.snapshot.dir=$OUT")
if [ -n "$PAGES" ]; then
    ARGS+=("-Dshowcase.snapshot.pages=$PAGES")
fi

if [ "$MODE" = "jvm" ]; then
    CMD=("$JAVA_HOME/bin/java" "${ARGS[@]}" ${JVM_OPTS:-} -jar target/quarkus-app/quarkus-run.jar)
else
    CMD=(./target/quarkus-fx-showcase-1.0.0-SNAPSHOT-runner "${ARGS[@]}" ${NATIVE_OPTS:-})
fi

"${CMD[@]}" > "$OUT/run.log" 2>&1 &
PID=$!
( sleep "$TIMEOUT"; if kill -9 "$PID" 2>/dev/null; then echo "WATCHDOG: killed after ${TIMEOUT}s" >> "$OUT/run.log"; fi ) &
WATCHDOG=$!
wait "$PID"
STATUS=$?
kill "$WATCHDOG" 2>/dev/null
wait "$WATCHDOG" 2>/dev/null
echo "exit=$STATUS" >> "$OUT/run.log"

IMAGES=$(find "$OUT" -name '*.png' | wc -l | tr -d ' ')
REPORT=$([ -f "$OUT/report.json" ] && echo "report.json written" || echo "NO report.json")
echo "$LABEL: exit=$STATUS, $IMAGES images, $REPORT"
