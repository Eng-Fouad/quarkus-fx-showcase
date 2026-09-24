#!/usr/bin/env bash
# JVM snapshot run under the GraalVM tracing agent, to record the reachability metadata JavaFX really uses:
#   comparison/trace-<label>/metadata/reachability-metadata.json
#
# usage: scripts/trace.sh <label> [extra JVM options]
#   scripts/trace.sh es2
#   scripts/trace.sh mtl -Dprism.order=mtl
set -uo pipefail
cd "$(dirname "$0")/.."
LABEL=${1:?usage: trace.sh <label> [jvm options]}
shift
export JVM_OPTS="-agentlib:native-image-agent=config-output-dir=comparison/trace-$LABEL/metadata $*"
scripts/snapshot.sh jvm "trace-$LABEL"
"${JAVA_HOME:-$HOME/.sdkman/candidates/java/25-graalce}/bin/java" tools/MetadataDiff.java "comparison/trace-$LABEL/metadata/reachability-metadata.json" > "comparison/trace-$LABEL/metadata-diff.md"
head -c 3000 "comparison/trace-$LABEL/metadata-diff.md"
