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
 * libjvm.dylib next to a native executable, so WebKit fails to load and WebView cannot be used. An empty libjvm.dylib is
 * installed in the directory where JavaFX extracts its native libraries (NativeLibLoader.cacheLibrary).
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
        try (InputStream in = WebKitNativeSupport.class.getResourceAsStream("/showcase/native/libjvm.dylib")) {
            if (in != null && !Files.exists(stub)) {
                Files.createDirectories(stub.getParent());
                Files.copy(in, stub, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.warnf(e, "Unable to install %s : WebView will not be available", stub);
        }
    }
}
