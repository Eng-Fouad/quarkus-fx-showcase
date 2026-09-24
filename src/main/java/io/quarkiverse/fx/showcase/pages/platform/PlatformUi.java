package io.quarkiverse.fx.showcase.pages.platform;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.MainView;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Shared building blocks of the platform-* pages.
 */
final class PlatformUi {

    static final String STYLESHEET = "/showcase/fxml/pages.css";

    /** Page content width (page frame minus its padding). */
    static final double CONTENT_WIDTH = MainView.PAGE_WIDTH - 32;

    static final double HALF_WIDTH = (CONTENT_WIDTH - 12) / 2;

    static final String READY = "platform.ready";
    static final String EXECUTOR = "platform.executor";

    private PlatformUi() {
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-caption");
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    static Label note(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-note");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    static Label line(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("event-line");
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    /**
     * A bordered box : a small caption above the demo nodes.
     */
    static VBox demo(String caption, Node... content) {
        VBox box = new VBox(5);
        box.getStyleClass().add("demo-box");
        box.getChildren().add(caption(caption));
        box.getChildren().addAll(content);
        return box;
    }

    static <T extends Region> T width(T region, double width) {
        region.setPrefWidth(width);
        region.setMinWidth(width);
        region.setMaxWidth(width);
        return region;
    }

    /**
     * A page root with the group stylesheet.
     */
    static VBox page(double spacing, Node... content) {
        VBox root = new VBox(spacing, content);
        root.getStyleClass().add("group-page");
        root.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return root;
    }

    /**
     * A checks table with a narrower name column, wrapping values within {@code width}.
     */
    static VBox checks(String title, List<Check> checks, double nameWidth, double width) {
        VBox view = Checks.view(title, checks);
        view.getChildren().stream().filter(GridPane.class::isInstance).map(GridPane.class::cast).forEach(grid -> {
            var name = grid.getColumnConstraints().get(1);
            name.setMinWidth(nameWidth);
            name.setPrefWidth(nameWidth);
            name.setMaxWidth(nameWidth);
        });
        if (title == null || title.isEmpty()) {
            view.getChildren().removeIf(Label.class::isInstance);
        }
        return width(view, width);
    }

    /**
     * A fixed pool of daemon threads.
     */
    static ExecutorService executor(String name) {
        AtomicInteger count = new AtomicInteger();
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, name + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Completes once {@code content} is shown and laid out.
     */
    static CompletableFuture<Void> whenShown(Node content) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Fx.when(content.sceneProperty(), Objects::nonNull)
                .thenCompose(scene -> Fx.pulses(2))
                .whenComplete((v, error) -> {
                    if (error != null) {
                        done.completeExceptionally(error);
                    } else {
                        done.complete(null);
                    }
                });
        return done;
    }

    static CompletionStage<?> ready(Node content) {
        Object ready = content.getProperties().get(READY);
        return ready instanceof CompletionStage<?> stage ? stage : CompletableFuture.completedFuture(null);
    }

    static void shutdown(Node content) {
        if (content.getProperties().get(EXECUTOR) instanceof ExecutorService executor) {
            executor.shutdownNow();
        }
    }

    /**
     * Rounds for display, independently of the default locale.
     */
    static String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
