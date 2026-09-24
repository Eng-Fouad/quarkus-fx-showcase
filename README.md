# Quarkus FX Showcase

A JavaFX application built with [Quarkus](https://quarkus.io) and the [quarkus-fx](https://github.com/quarkiverse/quarkus-fx)
extension, exercising as much of JavaFX as possible: controls, data views, layouts and CSS, shapes, paint and effects,
text and fonts, images and canvas, charts, animation, 3D, WebView, media, Swing interop, windows, dialogs and popups,
FXML and quarkus-fx features, platform services and concurrency.

Its purpose is to verify that a GraalVM native executable renders **exactly** the same UI as the JVM, and to find the gaps
in the native image configuration of quarkus-fx.

## Requirements

- macOS (the native configuration is currently tuned for macOS)
- GraalVM for JDK 25 (`JAVA_HOME` and `GRAALVM_HOME`), Maven 3.9
- quarkus-fx `999-SNAPSHOT` installed in the local Maven repository (`mvn install` in a quarkus-fx checkout)

## Run

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar
```

Native executable:

```bash
mvn package -Dnative
./target/quarkus-fx-showcase-1.0.0-SNAPSHOT-runner
```

The application intentionally has no `@QuarkusMain`: the launcher provided by quarkus-fx is used.

## Compare JVM and native rendering

In snapshot mode (`-Dshowcase.snapshot.dir=...`), the application renders every page, writes each one to a PNG file
together with a `report.json` of non-visual checks and errors, then exits.

```bash
scripts/snapshot.sh jvm            # comparison/jvm
scripts/snapshot.sh native         # comparison/native
java tools/Compare.java comparison/jvm comparison/native comparison/diff   # summary.txt and index.html
```

`scripts/cycle.sh <label> [--trace]` runs a whole iteration: JVM build and snapshots, native build and snapshots, comparison.

Pages must be deterministic (no running animation, clock, randomness, caret or hover) so that two runs render the same
pixels. Differences of at most 2 levels per channel on less than 0.5% of the pixels are reported as floating point
noise: the same differences appear between two JVM runs using different execution modes (JIT vs `-Xint`).

## Native image configuration tools

- `scripts/trace.sh <label> [jvm options]`: runs the snapshots under the GraalVM tracing agent, then
  `tools/MetadataDiff.java` lists the JNI, reflection and resource accesses of JavaFX that quarkus-fx does not register.
- `tools/ClinitAudit.java` (with ASM on the class path): lists the JavaFX classes quarkus-fx leaves initialized at build
  time whose static initializer reaches native code, threads, native memory, system properties or resource bundles.

## Pages

Each page is a CDI bean implementing `io.quarkiverse.fx.showcase.core.FeaturePage` (see
`src/main/java/io/quarkiverse/fx/showcase/pages`). The binary test assets are regenerated with
`scripts/generate-assets.sh`. Third party fonts are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
