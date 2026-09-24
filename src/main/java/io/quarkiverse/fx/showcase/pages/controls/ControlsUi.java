package io.quarkiverse.fx.showcase.pages.controls;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Shared building blocks of the controls pages : captioned demo boxes, the group stylesheet and the fonts loaded from
 * the classpath.
 */
final class ControlsUi {

    static final String FONT_AWESOME = "/showcase/fonts/fa-solid-900.ttf";
    static final String DROID_KUFI = "/showcase/fonts/DroidKufi-Regular.ttf";

    static final String HOME = "\uf015";
    static final String HEART = "\uf004";
    static final String COG = "\uf013";
    static final String SEARCH = "\uf002";
    static final String SAVE = "\uf0c7";
    static final String FOLDER = "\uf07b";
    static final String TRASH = "\uf1f8";
    static final String BOLD = "\uf032";
    static final String ITALIC = "\uf033";
    static final String UNDERLINE = "\uf0cd";
    static final String CUT = "\uf0c4";
    static final String COPY = "\uf0c5";
    static final String PASTE = "\uf0ea";
    static final String UNDO = "\uf0e2";
    static final String REDO = "\uf01e";
    static final String PRINT = "\uf02f";
    static final String STAR = "\uf005";
    static final String USER = "\uf007";
    static final String BELL = "\uf0f3";
    static final String INFO = "\uf05a";

    private static Font fontAwesome;
    private static Font droidKufi;

    private ControlsUi() {
    }

    /**
     * The group stylesheet, loaded from the classpath.
     */
    static String stylesheet() {
        return Fx.resourceUrl("/showcase/controls/controls.css");
    }

    /**
     * Font Awesome 5 Free Solid, loaded once from a classpath URL ({@link Font#loadFont(String, double)}).
     */
    static synchronized Font fontAwesome() {
        if (fontAwesome == null) {
            fontAwesome = Objects.requireNonNull(Font.loadFont(Fx.resourceUrl(FONT_AWESOME), 14),
                    "Font.loadFont returned null for " + FONT_AWESOME);
        }
        return fontAwesome;
    }

    /**
     * Droid Arabic Kufi, loaded once from a classpath stream ({@link Font#loadFont(InputStream, double)}).
     */
    static synchronized Font droidKufi() {
        if (droidKufi == null) {
            try (InputStream in = Fx.resource(DROID_KUFI).openStream()) {
                droidKufi = Objects.requireNonNull(Font.loadFont(in, 13),
                        "Font.loadFont returned null for " + DROID_KUFI);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return droidKufi;
    }

    /**
     * A Font Awesome glyph as a label.
     */
    static Label glyph(String code, double size) {
        Label label = new Label(code);
        label.setFont(new Font(fontAwesome().getName(), size));
        label.getStyleClass().add("glyph");
        return label;
    }

    static Label glyph(String code, double size, Color color) {
        Label label = glyph(code, size);
        label.setTextFill(color);
        return label;
    }

    static Label caption(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("demo-caption");
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

    /**
     * A page root with the group stylesheet.
     */
    static VBox page(double spacing, Node... content) {
        VBox root = new VBox(spacing, content);
        root.getStyleClass().add("controls-page");
        root.getStylesheets().add(stylesheet());
        root.setPadding(Insets.EMPTY);
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return root;
    }

    /**
     * First node of the given type in the tree of {@code node} (depth first, including {@code node}).
     */
    static <T> T find(Node node, Class<T> type) {
        return find(node, type, null);
    }

    /**
     * First node of the given type, and having the given style class when not null.
     */
    static <T> T find(Node node, Class<T> type, String styleClass) {
        if (type.isInstance(node) && (styleClass == null || node.getStyleClass().contains(styleClass))) {
            return type.cast(node);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                T found = find(child, type, styleClass);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static <T extends Node> T grow(T node) {
        javafx.scene.layout.HBox.setHgrow(node, javafx.scene.layout.Priority.ALWAYS);
        return node;
    }

    /**
     * A container whose content is replaced once the page has been laid out, for checks depending on the layout.
     */
    static final class ChecksHolder extends VBox {

        final List<Check> early = new ArrayList<>();

        ChecksHolder() {
            getStyleClass().add("checks-holder");
        }

        void show(String title, List<Check> late) {
            List<Check> all = new ArrayList<>(early);
            all.addAll(late);
            getChildren().setAll(Checks.view(title, all));
        }
    }

    private static final String FOCUS_BEFORE = "controls.focusBefore";

    /**
     * Remembers the focus owner of the scene when {@code content} is added to it, see {@link #restoreFocus(Node)}.
     */
    static void rememberFocus(Node content) {
        content.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                Node owner = newScene.getFocusOwner();
                if (owner != null) {
                    content.getProperties().put(FOCUS_BEFORE, owner);
                } else {
                    content.getProperties().remove(FOCUS_BEFORE);
                }
            }
        });
    }

    /**
     * Some skins request the focus (e.g. ButtonBarSkin on its default button) : a focused control is only painted as
     * such while the window is active, which is not deterministic. Gives the focus back to the node that owned it
     * before the page was shown.
     */
    static void restoreFocus(Node content) {
        javafx.scene.Scene scene = content.getScene();
        if (scene == null) {
            return;
        }
        Node owner = scene.getFocusOwner();
        boolean inside = false;
        for (Node n = owner; n != null; n = n.getParent()) {
            if (n == content) {
                inside = true;
                break;
            }
        }
        if (inside) {
            Node before = (Node) content.getProperties().get(FOCUS_BEFORE);
            if (before != null && before.getScene() == scene) {
                before.requestFocus();
            } else {
                scene.getRoot().requestFocus();
            }
        }
    }

    /**
     * An invisible node carrying checks computed after the page snapshot (e.g. while capturing popups).
     */
    static Pane hiddenChecks() {
        Pane pane = new Pane();
        pane.setManaged(false);
        pane.setVisible(false);
        pane.getStyleClass().add("hidden-checks");
        return pane;
    }
}
