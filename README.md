# Quarkus FX Showcase

A JavaFX application built with [Quarkus](https://quarkus.io) and the [quarkus-fx](https://github.com/quarkiverse/quarkus-fx)
extension, exercising as much of JavaFX as possible: controls, data views, layouts and CSS, shapes, paint and effects,
text and fonts, images and canvas, charts, animation, 3D, WebView, media, Swing interop, windows, dialogs and popups,
FXML and quarkus-fx features, platform services and concurrency.

Its purpose is to verify that a GraalVM native executable renders **exactly** the same UI as the JVM, and to find the gaps
in the native image configuration of quarkus-fx.

## Requirements

- macOS, Windows or Linux
- GraalVM for JDK 25, with `JAVA_HOME` and `GRAALVM_HOME` pointing to it (the tools are JDK 25 single-file programs)
- quarkus-fx `999-SNAPSHOT` installed in the local Maven repository (`mvn install` in a quarkus-fx checkout)
- Native builds: see the [Quarkus native prerequisites](https://quarkus.io/guides/building-native-image) (Xcode command
  line tools on macOS, Visual Studio Build Tools on Windows, gcc/zlib/freetype development packages on Linux)

Maven is provided by the wrapper (`./mvnw` on macOS and Linux, `mvnw.cmd` on Windows).

## Run

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

Native executable (`...-runner.exe` on Windows):

```bash
./mvnw package -Dnative
./target/quarkus-fx-showcase-1.0.0-SNAPSHOT-runner
```

The application intentionally has no `@QuarkusMain`: the launcher provided by quarkus-fx is used.

## Compare JVM and native rendering

In snapshot mode (`-Dshowcase.snapshot.dir=...`), the application renders every page, writes each one to a PNG file
together with a `report.json` of non-visual checks and errors, then exits.

```bash
java tools/Snapshot.java jvm                  # comparison/jvm
java tools/Snapshot.java native               # comparison/native
java tools/Compare.java comparison/jvm comparison/native comparison/diff   # summary.txt and index.html
```

`java tools/Cycle.java <label> [--trace] [--offline] [--maven-args=a,b]` runs a whole iteration: JVM build and snapshots,
native build and snapshots, comparison. Options after `--` are passed to both runs, e.g.
`java tools/Cycle.java sw -- -Dprism.order=sw`; `--maven-args` to both builds (e.g.
`--maven-args=-Dquarkus.platform.version=3.33.3.3,-Dquarkus.native.native-image-xmx=5g`). It exits 1 when a build or a
run fails or when the runs do not match (its last line: `cycle <label> OK` or `cycle <label> FAILED : ...`).

Pages use `core/Platforms` to pick operating system specific fonts and expectations: snapshots are only compared between
runs on the same machine, so a page may look different on another operating system, but its checks must pass everywhere.

Pages must be deterministic (no running animation, clock, randomness, caret or hover) so that two runs render the same
pixels. Differences of at most 2 levels per channel on less than 0.5% of the pixels are reported as floating point
noise: the same differences appear between two JVM runs using different execution modes (JIT vs `-Xint`).

A page whose `runtimeDependent()` is `true` shows where a native image legitimately behaves differently from the JVM
because of JavaFX itself (`platform-native-limits`: the cause, found in the JavaFX sources, and a workaround working in
both runtimes). Its differences are reported as `EXPECTED`, not as mismatches; its failed checks still are.

Both runs of a comparison must use the same Prism pipeline (`d3d`, `mtl`, `es2` or `sw`): it is shown on the
environment page and in `report.json`, and a difference is reported as an `ENV DIFF` mismatch.

## Application-level native configuration

Everything JavaFX needs in a native executable comes from quarkus-fx, except what depends on the application itself:

- `@RegisterForReflection` on the application classes JavaFX reaches by reflection (models of `PropertyValueFactory`,
  JavaBean property adapters, FXML controllers and custom components)
- `src/main/resources/META-INF/native-image/io.quarkiverse.fx.showcase/quarkus-fx-showcase/`: JNI access for the Java
  objects exposed to JavaScript in a WebView, serialization of the clipboard custom format, and the Hijrah calendar data
  (with `JavaHomeFeature`, a workaround for [oracle/graal#11410](https://github.com/oracle/graal/issues/11410))
- `WebKitNativeSupport`: on macOS, `libjfxwebkit.dylib` links to `libjvm.dylib` without using it, an empty stand-in is
  installed next to it so that WebView loads in native executables (not needed on Windows: no JavaFX DLL imports
  `jvm.dll`, see `dumpbin /dependents jfxwebkit.dll`)

## JavaFX behaviors the pages work around

They exist in JVM mode too, but make runs differ or fail depending on timing:

- Windows clipboard: when another process (e.g. the Windows clipboard history) asks for a format of clipboard content
  JavaFX already replaced, Glass gets no data and calls `GetArrayLength` on a null array (`GlassClipboard.cpp`,
  `OLE_CHECK_NOTNULL` does not stop in release builds): the process crashes. `platform-services` keeps its content until
  the page is left, so that such readers complete first.
- Text layout cache: layouts of the same text and font share their runs (`PrismTextLayout`), whose metrics depend on
  the bounds type of the layout that created them. A Label laid out with Modena's centered bounds moves Canvas text drawn
  with `VPos.CENTER` in the same font. `images-canvas` uses a font size no label uses.
- Media (GStreamer engine, Windows): after a seek of a paused player, the frame shown can be the previous key frame
  instead of the frame of the new position; some players show frames about one second ahead of their time from the
  start, and a few fail before READY (`ERROR_MEDIA_INVALID`). `media-video` reads the frame number drawn in the clip:
  it repeats a seek from another position until the frame of the current time is shown, and replaces a player whose
  first seek to 0 does not show frame 0. A few runs still fail (all the new players fail with `ERROR_MEDIA_INVALID`
  once one got stuck).

## Native image configuration tools

- `java tools/Cycle.java <label> --trace`: also runs the JVM snapshots under the GraalVM tracing agent, then
  `tools/MetadataDiff.java` lists the JNI, reflection and resource accesses of JavaFX that quarkus-fx does not register
  for the current platform.
- `tools/ClinitAudit.java` (with ASM on the class path): lists the JavaFX classes quarkus-fx leaves initialized at build
  time whose static initializer reaches native code, threads, native memory, system properties or resource bundles:
  `java -cp ~/.m2/repository/org/ow2/asm/asm/9.9/asm-9.9.jar tools/ClinitAudit.java`

## Linux in Docker

`docker/linux/Dockerfile` provides a Linux environment (GraalVM, virtual X server, GTK, Mesa, ffmpeg, fonts):

```bash
docker build -t quarkus-fx-showcase-linux docker/linux
docker run --rm --init -v "$PWD":/showcase -v "$HOME/.m2":/root/.m2 quarkus-fx-showcase-linux java tools/Cycle.java linux
```

## Continuous integration

`.github/workflows/cycle.yml` runs the cycles on GitHub Actions against a quarkus-fx commit (its runtime and deployment
modules installed first), built with the Quarkus version of that commit, on four platforms. A plan job builds the
matrices from one table of variants: every run has the default variants, and the nightly run adds the software pipeline
(`-Dprism.order=sw`) on each platform.

| Platform | Runner | What runs |
|---|---|---|
| Linux-arm64 | `ubuntu-24.04-arm` | the Linux image (Xvfb 1920x1200) : default with the tracing agent |
| Linux-x64 | `ubuntu-24.04` | the Linux image : default |
| Windows-x64 | `windows-2025` | Oracle GraalVM for JDK 25 on the runner desktop, at its largest display mode : default with the tracing agent |
| macOS-arm64 | `macos-26` | Oracle GraalVM for JDK 25 on the runner desktop (a 5 GB native-image heap : 7 GB runners) : default with the tracing agent |

There is no Windows-arm64 variant: neither JavaFX nor GraalVM exist for Windows on arm64. `Cycle.java` exits 1 when a
build or a run fails or when the runs do not match. `.github/scripts/cycle-report.sh` writes the verdict, what differs,
the screen and the pipeline of the runs to the summary of the run and as an annotation of the job; the logs, reports
and metadata are artifacts (with the images of both runs when the job failed). `.github/scripts/windows-desktop.ps1`
and `macos-desktop.sh` prepare and describe the runner desktops (display mode, screenshots before and after the cycle).
The platforms that never ran on GitHub are non-blocking (`blocking` in the plan table) until they are reliably green.

- `.github/workflows/showcase.yml` (this repository): on every push to main and every pull request (except the ones
  that only change the README), every night (the builder image, the runner images and quarkus-fx change without the
  showcase), and manually (another quarkus-fx commit, some of the platforms, the nightly variants). It tests the
  `optimize-native` branch of Eng-Fouad/quarkus-fx (the defaults of `cycle.yml`) until it is merged.
- `.github/workflows/showcase.yml` of quarkus-fx calls the same workflow on every push to its main branch, on its pull
  requests labelled `showcase`, and manually.

## Pages

Each page is a CDI bean implementing `io.quarkiverse.fx.showcase.core.FeaturePage` (see
`src/main/java/io/quarkiverse/fx/showcase/pages`). The binary test assets are regenerated with
`scripts/generate-assets.sh` (macOS only: it uses `afconvert` and AVFoundation). Third party fonts are listed in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
