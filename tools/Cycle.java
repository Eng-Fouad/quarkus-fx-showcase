import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One JVM vs native iteration : JVM build and snapshots (optionally under the tracing agent), native build and snapshots,
 * comparison. Results: comparison/jvm-&lt;label&gt;, comparison/native-&lt;label&gt;, comparison/diff-&lt;label&gt;
 * (summary.txt, index.html), build logs in comparison/logs-&lt;label&gt;.
 * <p>
 * usage: java tools/Cycle.java &lt;label&gt; [--trace] [--skip-jvm] [--skip-native-build] [--offline] [--native-args=...]
 * <p>
 * --native-args is a comma separated list of native-image options, e.g. --native-args=-H:+PrintClassInitialization.
 * Options for the snapshot runs can be given after {@code --} and apply to both the JVM and the native run.
 */
public class Cycle {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: java tools/Cycle.java <label> [--trace] [--skip-jvm] [--skip-native-build] [--offline] "
                    + "[--native-args=...] [-- snapshot options...]");
            System.exit(2);
        }
        String label = args[0];
        boolean trace = false;
        boolean skipJvm = false;
        boolean skipNativeBuild = false;
        boolean offline = false;
        String nativeArgs = null;
        List<String> snapshotOptions = new ArrayList<>();
        boolean inOptions = false;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (inOptions) {
                snapshotOptions.add(arg);
            } else if (arg.equals("--")) {
                inOptions = true;
            } else if (arg.equals("--trace")) {
                trace = true;
            } else if (arg.equals("--skip-jvm")) {
                skipJvm = true;
            } else if (arg.equals("--skip-native-build")) {
                skipNativeBuild = true;
            } else if (arg.equals("--offline")) {
                offline = true;
            } else if (arg.startsWith("--native-args=")) {
                nativeArgs = arg.substring("--native-args=".length());
            }
        }

        Path logs = Path.of("comparison", "logs-" + label);
        Files.createDirectories(logs);

        if (!skipJvm) {
            step("JVM build");
            if (maven(logs.resolve("jvm-build.log"), offline, "package", "-DskipTests") != 0) {
                step("JVM build FAILED, see " + logs.resolve("jvm-build.log"));
                System.exit(1);
            }
            step("JVM snapshots");
            Snapshot.run("jvm", "jvm-" + label, null, snapshotOptions, 900);
            if (trace) {
                step("JVM snapshots under the tracing agent");
                Path metadata = Path.of("comparison", "trace-" + label, "metadata");
                List<String> options = new ArrayList<>(snapshotOptions);
                options.add(0, "-agentlib:native-image-agent=config-output-dir=" + metadata);
                Snapshot.run("jvm", "trace-" + label, null, options, 900);
                Path diff = Path.of("comparison", "trace-" + label, "metadata-diff.md");
                java(diff, "tools/MetadataDiff.java", metadata.resolve("reachability-metadata.json").toString());
                Files.readAllLines(diff).stream().filter(l -> l.startsWith("## ")).forEach(System.out::println);
            }
        }

        if (!skipNativeBuild) {
            step("native build");
            List<String> mavenArgs = new ArrayList<>(List.of("package", "-Dnative", "-DskipTests",
                    "-Dquarkus.native.native-image-xmx=6g"));
            if (nativeArgs != null) {
                mavenArgs.add("-Dquarkus.native.additional-build-args=-H:+UnlockExperimentalVMOptions," + nativeArgs
                        + ",-H:-UnlockExperimentalVMOptions");
            }
            Path log = logs.resolve("native-build.log");
            if (maven(log, offline, mavenArgs.toArray(String[]::new)) != 0) {
                step("native build FAILED, see " + log);
                Files.readAllLines(log).stream().filter(l -> l.contains("Fatal error") || l.startsWith("Error:")).limit(5)
                        .forEach(System.out::println);
                System.exit(1);
            }
            Files.readAllLines(log).stream().filter(l -> l.contains("Finished generating") || l.contains("Peak RSS"))
                    .forEach(System.out::println);
        }

        step("native snapshots");
        Snapshot.run("native", "native-" + label, null, snapshotOptions, 900);

        step("compare");
        Path summary = logs.resolve("compare.txt");
        java(summary, "tools/Compare.java", "comparison/jvm-" + label, "comparison/native-" + label,
                "comparison/diff-" + label);
        System.out.println(Files.readAllLines(summary).getFirst());
    }

    static void step(String message) {
        System.out.println("[" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message);
    }

    static int maven(Path log, boolean offline, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        boolean windows = Snapshot.isWindows();
        Path wrapper = Path.of(windows ? "mvnw.cmd" : "mvnw");
        if (Files.exists(wrapper)) {
            command.add(wrapper.toAbsolutePath().toString());
        } else {
            command.add(windows ? "mvn.cmd" : "mvn");
        }
        if (offline) {
            command.add("-o");
        }
        command.addAll(List.of(args));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        // build with the JDK running this tool (GraalVM for native builds), whatever the shell environment says
        Path javaHome = Path.of(System.getProperty("java.home"));
        builder.environment().put("JAVA_HOME", javaHome.toString());
        if (Files.exists(javaHome.resolve("bin").resolve(windows ? "native-image.cmd" : "native-image"))) {
            builder.environment().put("GRAALVM_HOME", javaHome.toString());
        }
        return builder.start().waitFor();
    }

    static int java(Path output, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(Snapshot.javaExecutable());
        command.addAll(List.of(args));
        return new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start().waitFor();
    }
}
