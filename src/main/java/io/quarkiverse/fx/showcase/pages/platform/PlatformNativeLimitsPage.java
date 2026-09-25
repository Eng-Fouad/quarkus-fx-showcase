package io.quarkiverse.fx.showcase.pages.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.pages.web.WebSupport;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

/**
 * Where a native image behaves differently from the JVM because of JavaFX itself : the cause, found in the JavaFX
 * sources, and a workaround, which must work in both runtimes.
 * <p>
 * This page is {@link #runtimeDependent() runtime dependent} : its differences between a JVM run and a native run are
 * reported as EXPECTED by tools/Compare.java. Its checks still fail when a runtime does not behave as described.
 */
@Singleton
public class PlatformNativeLimitsPage implements FeaturePage {

    private static final String STATE = PlatformNativeLimitsPage.class.getName();
    private static final String CSS = "/showcase/web/loaded.css";
    private static final String HTML = "<html><body><h1>User style sheet</h1>"
            + "<p class='lead'>h1 and body styled by loaded.css</p></body></html>";
    private static final String STYLED = "accepted, h1 color rgb(46, 125, 50)";
    private static final String REJECTED = "rejected: java.lang.IllegalArgumentException: Invalid stylesheet URL";

    @Override
    public String id() {
        return "platform-native-limits";
    }

    @Override
    public String title() {
        return "Native image limits";
    }

    @Override
    public String category() {
        return Categories.PLATFORM;
    }

    @Override
    public int order() {
        return 90;
    }

    @Override
    public boolean runtimeDependent() {
        return true;
    }

    /** A WebView whose engine is given a user style sheet location. */
    private static final class StyledView {
        final WebView view = new WebView();
        final String location;
        String error;
        CompletionStage<?> loaded;

        StyledView(String location) {
            this.location = location;
            view.setContextMenuEnabled(false);
            view.setPrefSize(300, 84);
            view.setMinSize(300, 84);
            view.setMaxSize(300, 84);
            try {
                view.getEngine().setUserStyleSheetLocation(location);
            } catch (Throwable t) {
                error = Checks.describe(t);
            }
            loaded = WebSupport.loaded(view.getEngine(), 20_000);
            view.getEngine().loadContent(HTML);
        }

        /** Whether the style sheet was accepted, and applied. */
        String outcome() {
            if (error != null) {
                return "rejected: " + error;
            }
            return "accepted, h1 color "
                    + view.getEngine().executeScript("getComputedStyle(document.querySelector('h1')).color");
        }
    }

    private static final class State {
        StyledView classpath;
        StyledView data;
        final VBox checks = new VBox(new Label("Waiting for the pages to load..."));
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();
        String classpathUrl = Fx.resourceUrl(CSS);
        state.classpath = new StyledView(classpathUrl);
        state.data = new StyledView("data:text/css;charset=utf-8;base64," + WebSupport.base64(WebSupport.resourceBytes(CSS)));

        VBox userStyleSheet = PlatformUi.demo("WebEngine.setUserStyleSheetLocation(url) : a style sheet on the class path",
                PlatformUi.note("Cause : a class path resource is a jar: (or file:) URL on the JVM, and a resource: URL in "
                        + "a native image. The user style sheet location only accepts file:, jar:, jrt: and data: URLs : "
                        + "any other scheme throws IllegalArgumentException(\"Invalid stylesheet URL\") (javafx.web, "
                        + "javafx.scene.web.WebEngine, userStyleSheetLocation property). The other class path URLs of this "
                        + "showcase (images, media, web pages, style sheets, fonts, FXML) work in both runtimes."),
                PlatformUi.note("Workaround : read the style sheet and give its content as a data: URL "
                        + "(data:text/css;charset=utf-8;base64,...), which is what WebEngine itself does with a file: or "
                        + "jar: URL."),
                new HBox(16,
                        column("setUserStyleSheetLocation(class path URL)", state.classpath.view),
                        column("workaround : data: URL of the same file", state.data.view)),
                state.checks);
        VBox root = PlatformUi.page(10,
                PlatformUi.note("JavaFX APIs that behave differently in a native image, with a workaround working in both "
                        + "runtimes. Differences between the JVM and native snapshots of this page are expected."),
                PlatformUi.width(userStyleSheet, PlatformUi.CONTENT_WIDTH));
        root.getProperties().put(STATE, state);

        String scheme = classpathUrl.substring(0, classpathUrl.indexOf(':'));
        boolean nativeImage = ShowcaseMode.runtime().equals("NATIVE");
        state.ready = CompletableFuture.allOf(state.classpath.loaded.toCompletableFuture(),
                state.data.loaded.toCompletableFuture())
                .thenComposeAsync(v -> Fx.pulses(5), Fx.FX_THREAD)
                .thenApply(v -> {
                    List<Check> checks = new ArrayList<>();
                    checks.add(Check.info("class path URL scheme", scheme));
                    checks.add(Checks.expect("setUserStyleSheetLocation(class path URL)", nativeImage ? REJECTED : STYLED,
                            state.classpath::outcome));
                    checks.add(Checks.expect("workaround : setUserStyleSheetLocation(data: URL)", STYLED,
                            state.data::outcome));
                    state.checks.getChildren().setAll(PlatformUi.checks(null, checks, 300,
                            PlatformUi.CONTENT_WIDTH - 18));
                    return null;
                })
                .thenCompose(v -> Fx.pulses(5))
                .thenCompose(v -> WebSupport.stable(root, 10_000))
                .thenCompose(v -> Fx.delay(200));
        return root;
    }

    private static VBox column(String caption, WebView view) {
        return new VBox(4, WebSupport.caption(caption), view);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null) {
            state.classpath.view.getEngine().loadContent("");
            state.data.view.getEngine().loadContent("");
        }
    }
}
