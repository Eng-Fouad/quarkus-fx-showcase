package io.quarkiverse.fx.showcase.graal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkiverse.fx.FxApplicationStartupEvent;
import io.quarkiverse.fx.showcase.core.Platforms;
import io.quarkus.runtime.ImageMode;

/**
 * On macOS, libjfxwebkit.dylib links to libjvm.dylib (through @loader_path) without using any of its symbols. There is no
 * libjvm.dylib next to a native executable without AWT, so WebKit fails to load and WebView cannot be used. A
 * libjvm.dylib is installed in the directory where JavaFX extracts its native libraries (NativeLibLoader.cacheLibrary) :
 * an empty one, or, with Quarkus Desktop, a copy of the libjvm.dylib that GraalVM generates next to the executable for
 * the AWT libraries. Both have the install name {@code @rpath/libjvm.dylib}, and dyld uses the first one loaded for
 * every library linked to it : with the empty one loaded first (a WebView shown before AWT starts), libawt.dylib found
 * none of the JVM_ functions it imports and the process crashed in its JNI_OnLoad.
 */
@Singleton
public class WebKitNativeSupport {

    private static final Logger LOG = Logger.getLogger(WebKitNativeSupport.class);

    void onFxStartup(@Observes FxApplicationStartupEvent event) {
        if (!ImageMode.current().isNativeImage() || !Platforms.isMac()) {
            return;
        }
        String version = System.getProperty("javafx.runtime.version", "versionless").replace(":", "-");
        String cacheDir = System.getProperty("javafx.cachedir", "");
        if (cacheDir.isEmpty()) {
            cacheDir = System.getProperty("user.home") + "/.openjfx/cache/" + version + "/" + System.getProperty("os.arch");
        }
        Path stub = Path.of(cacheDir, "libjvm.dylib");
        Path shim = ProcessHandle.current().info().command().map(Path::of).map(Path::getParent)
                .map(directory -> directory.resolve("libjvm.dylib")).filter(Files::isRegularFile).orElse(null);
        try {
            if (shim != null) {
                // Quarkus Desktop : the libjvm.dylib of the AWT libraries, replacing an empty one
                if (!Files.exists(stub) || Files.mismatch(shim, stub) != -1) {
                    Files.createDirectories(stub.getParent());
                    Files.copy(shim, stub, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            }
            try (InputStream in = WebKitNativeSupport.class.getResourceAsStream("/showcase/native/libjvm.dylib")) {
                if (in != null && !Files.exists(stub)) {
                    Files.createDirectories(stub.getParent());
                    Files.copy(in, stub, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            LOG.warnf(e, "Unable to install %s : WebView will not be available", stub);
        }
    }
}
