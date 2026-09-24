package io.quarkiverse.fx.showcase.core;

import io.quarkus.runtime.ImageMode;

/**
 * Global state of the showcase run.
 */
public final class ShowcaseMode {

    private static volatile boolean snapshot;

    private ShowcaseMode() {
    }

    /**
     * {@code true} when pages are rendered to be compared : pages must then be deterministic
     * (animations paused at a fixed time, no caret, no hover, no clock...).
     */
    public static boolean snapshot() {
        return snapshot;
    }

    static void enableSnapshot() {
        snapshot = true;
    }

    /**
     * {@code JVM} or {@code NATIVE}.
     */
    public static String runtime() {
        return ImageMode.current().isNativeImage() ? "NATIVE" : "JVM";
    }
}
