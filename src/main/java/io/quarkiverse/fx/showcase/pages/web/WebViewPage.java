package io.quarkiverse.fx.showcase.pages.web;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.EventTarget;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * WebView rendering a rich HTML page (CSS, web fonts, SVG, canvas, form controls, unicode), JavaScript evaluation,
 * JavaScript to Java upcalls and DOM access from Java.
 */
@Singleton
public class WebViewPage implements FeaturePage {

    static final String PAGE = "/showcase/web/webview.html";
    private static final String STATE = WebViewPage.class.getName();

    private String html;

    @Override
    public String id() {
        return "web-webview";
    }

    @Override
    public String title() {
        return "WebView";
    }

    @Override
    public String category() {
        return Categories.WEB;
    }

    @Override
    public int order() {
        return 10;
    }

    private String html() {
        if (html == null) {
            html = Fx.resourceText(PAGE)
                    .replace("@ROBOTO_BASE64@", WebSupport.base64(WebSupport.resourceBytes("/showcase/fonts/Roboto-Light.ttf")))
                    .replace("@ICONS_BASE64@", WebSupport.base64(WebSupport.resourceBytes("/showcase/fonts/fa-solid-900.ttf")));
        }
        return html;
    }

    private static final class State {
        final WebView view;
        final WebEngine engine;
        final WebBridge bridge = new WebBridge();
        final VBox jsChecks = new VBox();
        final VBox bridgeChecks = new VBox();
        Worker.State loadState;
        String loadOutcome;
        Throwable fontsError;
        Throwable canvasError;
        CompletionStage<?> ready;

        State(WebView view) {
            this.view = view;
            this.engine = view.getEngine();
        }
    }

    @Override
    public Node build() {
        WebView view = new WebView();
        view.setContextMenuEnabled(false);
        view.setPrefSize(1028, 380);
        view.setMinSize(1028, 380);
        view.setMaxSize(1028, 380);

        State state = new State(view);
        state.jsChecks.getChildren().add(new Label("Waiting for the page to load..."));
        HBox.setHgrow(state.jsChecks, Priority.ALWAYS);
        HBox.setHgrow(state.bridgeChecks, Priority.ALWAYS);
        state.jsChecks.setPrefWidth(506);
        state.bridgeChecks.setPrefWidth(506);
        HBox checks = new HBox(16, state.jsChecks, state.bridgeChecks);

        VBox root = new VBox(6,
                WebSupport.caption("WebEngine.loadContent(" + PAGE.substring(1)
                        + ") : flexbox, gradients, @font-face data: URIs, inline SVG, canvas drawn by JavaScript, table, form controls, unicode"),
                view, checks);
        root.getProperties().put(STATE, state);

        state.ready = WebSupport.loaded(state.engine, 20_000)
                .handle((loadState, error) -> {
                    state.loadState = loadState;
                    state.loadOutcome = WebSupport.outcome(state.engine, loadState, error);
                    return loadState;
                })
                .thenCompose(loadState -> loadState != Worker.State.SUCCEEDED
                        ? CompletableFuture.completedFuture(null)
                        : WebSupport.until(() -> fontsLoaded(state.engine), 10_000, "web fonts and canvas")
                                .handle((v, error) -> {
                                    state.fontsError = error;
                                    return null;
                                }))
                .thenCompose(v -> state.loadState != Worker.State.SUCCEEDED
                        ? CompletableFuture.completedFuture(null)
                        : canvasPainted(state).handle((painted, error) -> {
                            state.canvasError = error;
                            return null;
                        }))
                .thenApply(v -> {
                    showChecks(state);
                    return null;
                })
                .thenCompose(v -> Fx.pulses(10))
                .thenCompose(v -> Fx.delay(200));

        state.engine.loadContent(html());
        return root;
    }

    private static boolean fontsLoaded(WebEngine engine) {
        Object status = engine.executeScript(
                "document.fonts ? document.fonts.status : 'loaded'");
        Object canvas = engine.executeScript("document.body.getAttribute('data-canvas')");
        return "loaded".equals(status) && "drawn".equals(canvas);
    }

    /**
     * Waits until the canvas drawn by JavaScript is visible in a snapshot of the WebView : when it is not, the canvas is
     * drawn again so that WebKit paints it again.
     */
    private static CompletionStage<Void> canvasPainted(State state) {
        int[] pulses = { 0 };
        return WebSupport.until(() -> {
            if (++pulses[0] % 10 != 0) {
                return false;
            }
            String[] probe = String.valueOf(state.engine.executeScript("(function () {"
                    + " var canvas = document.getElementById('canvas');"
                    + " var r = canvas.getBoundingClientRect();"
                    + " var d = canvas.getContext('2d').getImageData(100, 140, 1, 1).data;"
                    + " return [Math.round(r.left) + 100, Math.round(r.top) + 140, d[0], d[1], d[2]].join(','); })()"))
                    .split(",");
            int argb = state.view.snapshot(null, null).getPixelReader()
                    .getArgb(Integer.parseInt(probe[0]), Integer.parseInt(probe[1]));
            boolean visible = Math.abs(((argb >> 16) & 0xff) - Integer.parseInt(probe[2])) < 12
                    && Math.abs(((argb >> 8) & 0xff) - Integer.parseInt(probe[3])) < 12
                    && Math.abs((argb & 0xff) - Integer.parseInt(probe[4])) < 12;
            if (!visible) {
                state.engine.executeScript("drawCanvas()");
            }
            return visible;
        }, 10_000, "the canvas to be painted in the WebView");
    }

    private static void showChecks(State state) {
        WebEngine engine = state.engine;
        List<Check> js = new ArrayList<>();
        List<Check> bridge = new ArrayList<>();
        js.add(Check.of("load worker state", state.loadState == Worker.State.SUCCEEDED, state.loadOutcome));
        if (state.loadState == Worker.State.SUCCEEDED) {
            js.add(state.fontsError != null
                    ? Check.fail("@font-face data: URIs loaded", Checks.describe(WebSupport.unwrap(state.fontsError)))
                    : Checks.expect("@font-face data: URIs loaded", "true, true",
                            () -> engine.executeScript("document.fonts.check('24px ShowcaseRoboto') + ', '"
                                    + " + document.fonts.check('28px ShowcaseIcons')")));
            js.add(Checks.expect("document.title", "Quarkus FX WebView", () -> engine.executeScript("document.title")));
            js.add(Checks.expect("6 * 7 (Integer), Math.sqrt(2) (Double)", "42, 1.4142135623730951", () -> {
                Object integer = engine.executeScript("6 * 7");
                Object decimal = engine.executeScript("Math.sqrt(2)");
                if (!(integer instanceof Integer) || !(decimal instanceof Double)) {
                    return "types " + integer.getClass().getSimpleName() + ", " + decimal.getClass().getSimpleName();
                }
                return integer + ", " + decimal;
            }));
            js.add(Checks.expect("JSON.stringify(object)", "{\"fx\":25,\"ok\":true,\"a\":[\"é\",null,1.5]}",
                    () -> engine.executeScript("JSON.stringify({fx: 25, ok: true, a: ['é', null, 1.5]})")));
            js.add(Checks.expect("Date.UTC(2024, 1, 29, 12, 30).toISOString()", "2024-02-29T12:30:00.000Z",
                    () -> engine.executeScript("new Date(Date.UTC(2024, 1, 29, 12, 30)).toISOString()")));
            js.add(Checks.expect("Intl.NumberFormat('de-DE')", "1.234.567,891",
                    () -> engine.executeScript("new Intl.NumberFormat('de-DE').format(1234567.891)")));
            js.add(Checks.expect("JSObject.getMember of a JS object", "quarkus / 3",
                    () -> {
                        JSObject object = (JSObject) engine.executeScript("({name: 'quarkus', size: 3})");
                        return object.getMember("name") + " / " + object.getMember("size");
                    }));
            js.add(Checks.expect("JSObject.call(\"greet\", \"Java\")", "Hello Java from JavaScript",
                    () -> ((JSObject) engine.executeScript("window")).call("greet", "Java")));
            js.add(state.canvasError == null ? Check.pass("canvas visible in a WebView snapshot", true)
                    : Check.fail("canvas visible in a WebView snapshot",
                            Checks.describe(WebSupport.unwrap(state.canvasError))));
            js.add(Checks.run("canvas getImageData(100, 140)",
                    () -> engine.executeScript("Array.from(document.getElementById('canvas').getContext('2d')"
                            + ".getImageData(100, 140, 1, 1).data).join(',')")));
            js.add(Checks.run("navigator.userAgent contains JavaFX", () -> {
                String userAgent = engine.getUserAgent();
                int index = userAgent.indexOf("JavaFX");
                if (index < 0) {
                    throw new IllegalStateException("No JavaFX token in " + userAgent);
                }
                int end = userAgent.indexOf(' ', index);
                return userAgent.substring(index, end < 0 ? userAgent.length() : end);
            }));

            // JavaScript -> Java upcalls
            bridge.add(Checks.run("JSObject.setMember(\"javaBridge\", ...)", () -> {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", state.bridge);
                return engine.executeScript("typeof window.javaBridge");
            }));
            bridge.add(Checks.expect("javaBridge.greet('WebView')", "Hello WebView from Java",
                    () -> engine.executeScript("javaBridge.greet('WebView')")));
            bridge.add(Checks.expect("add(20, 22) / sum([1.5, 2.5, 3])", "42 / 7",
                    () -> engine.executeScript("javaBridge.add(20, 22) + ' / ' + javaBridge.sum([1.5, 2.5, 3])")));
            bridge.add(Checks.expect("JS values as Java types", "Double,Double,String,Boolean,JSObject",
                    () -> engine.executeScript("[42, 1.5, 's', true, {a: 1}].map(function (v) {"
                            + " return javaBridge.describe(v); }).join(',')")));
            bridge.add(Checks.run("runBridgeDemo() (page script)", () -> {
                engine.executeScript("runBridgeDemo()");
                return state.bridge.calls().size() + " upcalls received by Java";
            }));

            // DOM access from Java
            Document document = engine.getDocument();
            bridge.add(Checks.expect("getDocument() root element", "HTML",
                    () -> document.getDocumentElement().getTagName()));
            bridge.add(Checks.run("getElementsByTagName(\"*\")", () -> document.getElementsByTagName("*").getLength()
                    + " elements"));
            bridge.add(Checks.expect("DOM text of #headline", "Java says: Hello DOM from Java",
                    () -> document.getElementById("headline").getTextContent()));
            bridge.add(Checks.expect("DOM appendChild from Java", "Added by Java (org.w3c.dom)", () -> {
                Element badge = document.createElement("div");
                badge.setAttribute("class", "badge");
                badge.setTextContent("Added by Java (org.w3c.dom)");
                document.getElementById("dom-target").appendChild(badge);
                return engine.executeScript("document.querySelector('#dom-target .badge').textContent");
            }));
            bridge.add(Checks.expect("DOM EventListener in Java, click()", 1, () -> {
                int[] clicks = { 0 };
                ((EventTarget) document.getElementById("dom-button"))
                        .addEventListener("click", event -> clicks[0]++, false);
                engine.executeScript("document.getElementById('dom-button').click()");
                return clicks[0];
            }));
            bridge.add(Checks.run("WebHistory entries", () -> engine.getHistory().getEntries().size()));
        }
        state.jsChecks.getChildren().setAll(Checks.view("JavaScript", js));
        if (!bridge.isEmpty()) {
            state.bridgeChecks.getChildren().setAll(Checks.view("Java bridge & DOM", bridge));
        }
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null) {
            state.engine.loadContent("");
        }
    }
}
