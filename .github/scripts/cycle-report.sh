#!/bin/bash
# The verdict of a cycle (tools/Cycle.java) in the summary of the run and as an annotation of the job : an error for a
# blocking job, a warning for a non-blocking one (annotations are readable through the public API without logging in,
# unlike the logs). Run from the workspace (showcase/, cycle.log, install.log) with LABEL, RUNNER and BLOCKING set.
# Portable : bash 3.2 and the BSD tools of macOS, Git Bash on Windows, GNU tools on Linux. Exits 0 : the cycle step
# fails the job.
c=showcase/comparison
summary=$c/diff-$LABEL/summary.txt
verdict=$(grep -m1 -E '^(MATCH|MISMATCH) :' "$c/logs-$LABEL/compare.txt" 2>/dev/null) || verdict="no comparison"
# what failed : the last line of the cycle (a build, a run, the comparison), its arguments, a cycle that did not finish,
# or the steps before it
failed="" details=""
if [ -f cycle.log ]; then
    failed=$(grep -m1 -E "cycle $LABEL FAILED|Invalid label|Unknown option|usage: " cycle.log | sed 's/^\[[0-9:]*\] //')
    if [ -z "$failed" ] && ! grep -q "cycle $LABEL OK" cycle.log; then
        last=$(grep -E '^\[[0-9:]*\] ' cycle.log | tail -1 | sed 's/^\[[0-9:]*\] //')
        failed="the cycle did not finish (a time-out or a crash), last step : ${last:-none}"
    fi
elif [ -f install.log ]; then
    failed="installing quarkus-fx failed"
else
    failed="the job failed before installing quarkus-fx"
fi
case "$failed" in
    *"JVM build failed"*) build=jvm ;;
    *"native build failed"*) build=native ;;
    *) build="" ;;
esac
if [ -f "$summary" ]; then
    # the page ids with their notes, the expected differences (runtime dependent pages) left out
    details=$({ grep -E '^(DIFFERENT|SIZE|ONLY_A|ONLY_B|ENV DIFF)' "$summary"
                sed -n '/^== Checks and errors/,$p' "$summary" | tail -n +2 \
                    | awk '/^ / { if ($0 !~ /^ *expected: /) { if (id != "") print id; id = ""; print } next } { id = $0 }'
              } | head -30)
elif [ -n "$build" ]; then
    details=$(grep -E '^\[ERROR\]|Fatal error|^Error:' "$c/logs-$LABEL/$build-build.log" 2>/dev/null | head -15)
elif [ "$failed" = "installing quarkus-fx failed" ]; then
    details=$(grep -E '^\[ERROR\]' install.log | head -15)
fi
# what the runs got : the screen and the Prism pipeline
environment=""
k='screen|pipeline|javafxVersion'
for run in jvm native; do
    report=$c/$run-$LABEL/report.json
    [ -f "$report" ] || continue
    keys=$(sed -n -E -e "s/^  \"($k)\": \"([^\"]*)\",?\$/\1=\2/p" -e "s/^  \"($k)\": ([^\",]*),?\$/\1=\2/p" "$report" \
        | paste -s -d ';' -)
    environment="$environment$run: $keys
"
done
suffix="" level=error
if [ "$BLOCKING" != true ]; then suffix=" (non-blocking)" level=warning; fi
if [ -n "$failed" ] || [ "${verdict#MATCH}" = "$verdict" ]; then
    # one line : the % and the line breaks encoded
    message=$(printf '%s\n%s\n' "${failed:-$verdict}" "$details" \
        | awk 'BEGIN { ORS = "" } { gsub(/%/, "%25"); gsub(/\r/, "%0D"); if (NR > 1) print "%0A"; print }')
    echo "::$level title=Showcase $LABEL on $RUNNER$suffix::$message"
fi
{
    echo "### $LABEL on $RUNNER$suffix"
    echo
    echo "\`$verdict\`"
    if [ -n "$failed" ]; then echo; echo "$failed"; fi
    if [ -n "$environment" ]; then echo; echo '```'; printf '%s' "$environment"; echo '```'; fi
    if [ -n "$details" ]; then echo; echo '```'; echo "$details"; echo '```'; fi
    if [ -f "$c/trace-$LABEL/metadata-diff.md" ]; then
        echo; echo "MetadataDiff (informational, see the artifact):"
        sections=$(grep '^## ' "$c/trace-$LABEL/metadata-diff.md" | sed 's/^## /- /')
        echo "${sections:-- (no MetadataDiff output)}"
    fi
} >> "$GITHUB_STEP_SUMMARY"
exit 0
