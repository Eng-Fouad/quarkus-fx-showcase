#!/usr/bin/env bash
# One JVM vs native iteration: JVM build + snapshots (+ tracing agent), native build + snapshots, comparison.
# Results: comparison/jvm-<label>, comparison/native-<label>, comparison/diff-<label>/{summary.txt,index.html},
# build logs in comparison/logs-<label>.
#
# usage: scripts/cycle.sh <label> [--trace] [--skip-jvm] [--skip-native-build]
#   NATIVE_BUILD_ARGS="-Dquarkus.native.additional-build-args=..." scripts/cycle.sh it1 --trace
set -uo pipefail
cd "$(dirname "$0")/.."
LABEL=${1:?usage: cycle.sh <label> [--trace] [--skip-jvm] [--skip-native-build]}
shift
TRACE=false; SKIP_JVM=false; SKIP_NATIVE_BUILD=false
for arg in "$@"; do
    case $arg in
        --trace) TRACE=true ;;
        --skip-jvm) SKIP_JVM=true ;;
        --skip-native-build) SKIP_NATIVE_BUILD=true ;;
    esac
done
export JAVA_HOME=${JAVA_HOME:-$HOME/.sdkman/candidates/java/25-graalce}
export GRAALVM_HOME=${GRAALVM_HOME:-$JAVA_HOME}
export PATH=$JAVA_HOME/bin:$PATH
LOGS=comparison/logs-$LABEL
mkdir -p "$LOGS"
step() { echo "[$(date +%H:%M:%S)] $*"; }

if [ "$SKIP_JVM" = false ]; then
    step "JVM build"
    mvn -o package -DskipTests > "$LOGS/jvm-build.log" 2>&1 || { step "JVM build FAILED (see $LOGS/jvm-build.log)"; exit 1; }
    step "JVM snapshots"
    scripts/snapshot.sh jvm "jvm-$LABEL"
    if [ "$TRACE" = true ]; then
        step "JVM snapshots under the tracing agent"
        scripts/trace.sh "$LABEL" > /dev/null 2>&1
        grep -E "^## " "comparison/trace-$LABEL/metadata-diff.md"
    fi
fi

if [ "$SKIP_NATIVE_BUILD" = false ]; then
    step "native build"
    # shellcheck disable=SC2086
    mvn -o package -Dnative -DskipTests -Dquarkus.native.native-image-xmx=6g ${NATIVE_BUILD_ARGS:-} > "$LOGS/native-build.log" 2>&1 \
        || { step "native build FAILED (see $LOGS/native-build.log)"; grep -E "Fatal error|Error:" "$LOGS/native-build.log" | head -5; exit 1; }
    grep -E "Finished generating|Peak RSS" "$LOGS/native-build.log" | head -2
fi

step "native snapshots"
scripts/snapshot.sh native "native-$LABEL"

step "compare"
java tools/Compare.java "comparison/jvm-$LABEL" "comparison/native-$LABEL" "comparison/diff-$LABEL" > "$LOGS/compare.txt"
head -1 "$LOGS/compare.txt"
