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
 * [--maven-args=...]
 * <p>
 * --native-args is a comma separated list of native-image options, e.g. --native-args=-H:+PrintClassInitialization.
 * --maven-args is a comma separated list of options of both Maven builds, e.g.
 * --maven-args=-Dquarkus.platform.version=3.33.3.3,-Dquarkus.native.native-image-xmx=5g (the native build gets a 6g
 * native-image heap otherwise). Options for the snapshot runs can be given after {@code --} and apply to both the JVM
 * and the native run.
 * <p>
 * Exit code 0 when both runs wrote their report and exited normally and the comparison matches (the runtime dependent
 * pages excepted, see Compare.java) : the last line is then {@code cycle <label> OK}, and {@code cycle <label> FAILED :
 * <reasons>} otherwise, with the exit code 1 (also for a failed build). 2 for invalid arguments (with a usage or an
 * error message).
 */
public class Cycle {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: java tools/Cycle.java <label> [--trace] [--skip-jvm] [--skip-native-build] [--offline] "
                    + "[--native-args=...] [--maven-args=...] [-- snapshot options...]");
            System.exit(2);
        }
        String label = args[0];
        // a directory name of comparison/
        if (!label.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) {
            System.err.println("Invalid label " + label + " : letters, digits, '.', '_' and '-'");
            System.exit(2);
        }
        boolean trace = false;
        boolean skipJvm = false;
        boolean skipNativeBuild = false;
        boolean offline = false;
        String nativeArgs = null;
        List<String> mavenOptions = new ArrayList<>();
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
            } else if (arg.startsWith("--maven-args=")) {
                for (String option : arg.substring("--maven-args=".length()).split(",")) {
                    if (!option.isBlank()) {
                        mavenOptions.add(option.strip());
                    }
                }
            } else {
                System.err.println("Unknown option " + arg);
                System.exit(2);
            }
        }
        List<String> failures = new ArrayList<>();

        Path logs = Path.of("comparison", "logs-" + label);
        Files.createDirectories(logs);

        if (!skipJvm) {
            step("JVM build");
            List<String> jvmArgs = new ArrayList<>(List.of("package", "-DskipTests"));
            jvmArgs.addAll(mavenOptions);
            if (maven(logs.resolve("jvm-build.log"), offline, jvmArgs.toArray(String[]::new)) != 0) {
                step("JVM build FAILED, see " + logs.resolve("jvm-build.log"));
                failed(label, List.of("the JVM build failed (" + logs.resolve("jvm-build.log") + ")"));
            }
            step("JVM snapshots");
            if (Snapshot.run("jvm", "jvm-" + label, null, snapshotOptions, 900) != 0) {
                failures.add("the JVM run failed (comparison/jvm-" + label + "/run.log)");
            }
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
            List<String> mavenArgs = new ArrayList<>(List.of("package", "-Dnative", "-DskipTests"));
            if (mavenOptions.stream().noneMatch(o -> o.startsWith("-Dquarkus.native.native-image-xmx="))) {
                mavenArgs.add("-Dquarkus.native.native-image-xmx=6g");
            }
            mavenArgs.addAll(mavenOptions);
            if (nativeArgs != null) {
                mavenArgs.add("-Dquarkus.native.additional-build-args=-H:+UnlockExperimentalVMOptions," + nativeArgs
                        + ",-H:-UnlockExperimentalVMOptions");
            }
            Path log = logs.resolve("native-build.log");
            if (maven(log, offline, mavenArgs.toArray(String[]::new)) != 0) {
                step("native build FAILED, see " + log);
                Files.readAllLines(log).stream().filter(l -> l.contains("Fatal error") || l.startsWith("Error:")).limit(5)
                        .forEach(System.out::println);
                failed(label, List.of("the native build failed (" + log + ")"));
            }
            Files.readAllLines(log).stream().filter(l -> l.contains("Finished generating") || l.contains("Peak RSS"))
                    .forEach(System.out::println);
        }

        step("native snapshots");
        if (Snapshot.run("native", "native-" + label, null, snapshotOptions, 900) != 0) {
            failures.add("the native run failed (comparison/native-" + label + "/run.log)");
        }

        step("compare");
        Path summary = logs.resolve("compare.txt");
        int compared = java(summary, "tools/Compare.java", "comparison/jvm-" + label, "comparison/native-" + label,
                "comparison/diff-" + label);
        List<String> lines = Files.readAllLines(summary);
        String verdict = lines.isEmpty() ? "no comparison" : lines.getFirst();
        System.out.println(verdict);
        if (compared != 0) {
            failures.add(verdict.startsWith("MISMATCH") ? "the runs do not match (comparison/diff-" + label + ")" : verdict);
        }

        if (!failures.isEmpty()) {
            failed(label, failures);
        }
        step("cycle " + label + " OK");
    }

    static void failed(String label, List<String> failures) {
        step("cycle " + label + " FAILED : " + String.join(" ; ", failures));
        System.exit(1);
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
