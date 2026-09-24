package io.quarkiverse.fx.showcase.pages.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;

/**
 * Helpers shared by the web pages.
 */
final class WebSupport {

    private WebSupport() {
    }

    /**
     * Completes (on the Fx thread) once {@code condition} holds, checked on every pulse, or exceptionally after
     * {@code timeoutMillis}.
     */
    static CompletionStage<Void> until(BooleanSupplier condition, double timeoutMillis, String what) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        new AnimationTimer() {
            private long start = -1;

            @Override
            public void handle(long now) {
                if (start < 0) {
                    start = now;
                }
                try {
                    if (condition.getAsBoolean()) {
                        stop();
                        done.complete(null);
                    } else if (now - start > timeoutMillis * 1_000_000) {
                        stop();
                        done.completeExceptionally(new TimeoutException("Timeout waiting for " + what));
                    }
                } catch (Throwable t) {
                    stop();
                    done.completeExceptionally(t);
                }
            }
        }.start();
        return done;
    }

    /**
     * Completes with the final state of the current load of {@code engine} (SUCCEEDED, FAILED or CANCELLED).
     */
    static CompletionStage<Worker.State> loaded(WebEngine engine, double timeoutMillis) {
        return Fx.timeout(Fx.when(engine.getLoadWorker().stateProperty(),
                state -> state == Worker.State.SUCCEEDED || state == Worker.State.FAILED
                        || state == Worker.State.CANCELLED),
                timeoutMillis, "web page load");
    }

    /**
     * Description of the outcome of a load : {@code SUCCEEDED}, or the state with the exception.
     */
    static String outcome(WebEngine engine, Worker.State state, Throwable error) {
        if (error != null) {
            return Checks.describe(unwrap(error));
        }
        Throwable exception = engine.getLoadWorker().getException();
        return exception == null ? String.valueOf(state) : state + ": " + Checks.describe(exception);
    }

    static Throwable unwrap(Throwable error) {
        while (error instanceof java.util.concurrent.CompletionException && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    static byte[] resourceBytes(String path) {
        try (InputStream in = Fx.resource(path).openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    static String base64(String text) {
        return base64(text.getBytes(StandardCharsets.UTF_8));
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-text-fill: #52606d; -fx-font-weight: bold;");
        return label;
    }
}
