package io.quarkiverse.fx.showcase.pages.images;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import io.quarkiverse.fx.showcase.core.ShowcaseMode;
import io.quarkiverse.fx.showcase.pages.text.Ui;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Image loading : PNG, JPEG, GIF, BMP, data URI and stream sources, ImageView options (fit, ratio, smooth, viewport,
 * rotation), requested sizes, background loading and error reporting.
 */
@Singleton
public class ImagesFormatsPage implements FeaturePage {

    private static final String READY = "images-formats.ready";

    static final String PNG = "/showcase/images/pattern.png";
    static final String JPG = "/showcase/images/photo.jpg";
    static final String GIF = "/showcase/images/pattern.gif";
    static final String BMP = "/showcase/images/pattern.bmp";
    static final String TEXTURE = "/showcase/images/texture.png";
    static final String ICON = "/showcase/images/icon.png";
    static final String ANIMATED = "/showcase/images/animated.gif";

    /** 8x8 RGBA PNG : red, green, blue and half transparent amber quadrants. */
    static final String DATA_URI = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAJUlEQVR42mN4amn6"
            + "Hxk7L3BHwQx0UCDX8fQ/Mv5/kL0BGdNBAQB0PZTBuob4+gAAAABJRU5ErkJggg==";
    static final String INVALID_DATA_URI = "data:image/png;base64,bm90IGFuIGltYWdl";

    static final double CELL = 150;

    @Override
    public String id() {
        return "images-formats";
    }

    @Override
    public String title() {
        return "Image Formats & ImageView";
    }

    @Override
    public String category() {
        return Categories.IMAGES_CANVAS;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Node build() throws Exception {
        List<Check> checks = new ArrayList<>();
        List<Node> cells = new ArrayList<>();

        Image png = new Image(Fx.resourceUrl(PNG));
        Image jpg = new Image(Fx.resourceUrl(JPG));
        Image gif = new Image(Fx.resourceUrl(GIF));
        Image bmp = new Image(Fx.resourceUrl(BMP));
        Image data = new Image(DATA_URI);
        Image stream;
        try (InputStream in = Fx.resource(JPG).openStream()) {
            stream = new Image(in);
        }

        // formats
        StackPane alpha = new StackPane(view(new Image(Fx.resourceUrl(TEXTURE)), 110, 110, true, true), view(png, 110, 110,
                true, true));
        cells.add(cell("PNG alpha over texture", alpha));
        cells.add(cell("JPEG, fitWidth 150", view(jpg, 150, 0, true, true)));
        cells.add(cell("GIF", view(gif, 110, 110, true, true)));
        cells.add(cell("BMP", view(bmp, 110, 110, true, true)));
        HBox dataBox = new HBox(10, new ImageView(data), view(data, 48, 48, true, false));
        dataBox.setAlignment(Pos.CENTER);
        cells.add(cell("data: URI 8x8 PNG, 1:1 and x6", dataBox));
        cells.add(cell("Image(InputStream), JPEG", view(stream, 150, 0, true, true)));

        // ImageView options
        cells.add(cell("fit 150x80, no ratio", view(jpg, 150, 80, false, true)));
        cells.add(cell("fit 150x80, preserveRatio", view(jpg, 150, 80, true, true)));
        cells.add(cell("8x8 at 96, smooth true", view(data, 96, 96, true, true)));
        cells.add(cell("8x8 at 96, smooth false", view(data, 96, 96, true, false)));
        ImageView viewport = view(jpg, 150, 0, true, true);
        viewport.setViewport(new Rectangle2D(160, 100, 180, 120));
        cells.add(cell("viewport 160,100 180x120", viewport));
        ImageView rotated = view(new Image(Fx.resourceUrl(ICON)), 64, 64, true, true);
        rotated.setRotate(30);
        cells.add(cell("rotate 30", rotated));

        // requested sizes, background loading, errors
        Image requestedSmooth = new Image(Fx.resourceUrl(PNG), 32, 32, false, true);
        Image requestedRough = new Image(Fx.resourceUrl(PNG), 32, 32, false, false);
        Image requestedRatio = new Image(Fx.resourceUrl(JPG), 100, 100, true, true);
        cells.add(cell("requested 32px, smooth", view(requestedSmooth, 96, 96, true, false)));
        cells.add(cell("requested 32px, rough", view(requestedRough, 96, 96, true, false)));
        cells.add(cell("requested 100, ratio", view(requestedRatio, 0, 0, true, true)));

        Image background = new Image(Fx.resourceUrl(JPG), 150, 0, true, true, true);
        cells.add(cell("background loading", view(background, 150, 0, true, true)));

        String missingUrl = Fx.resourceUrl(PNG).replace("pattern.png", "missing-image.png");
        Image missing = new Image(missingUrl);
        Image invalid = new Image(INVALID_DATA_URI);
        Label missingLabel = new Label("isError: " + missing.isError() + "\n"
                + (missing.getException() == null ? "no exception" : missing.getException().getClass().getSimpleName())
                + "\ninvalid data: " + invalid.isError());
        missingLabel.setStyle("-fx-text-fill: #c62828; -fx-font-size: 11px;");
        cells.add(cell("missing / invalid", missingLabel));

        Image animated = new Image(Fx.resourceUrl(ANIMATED));
        if (ShowcaseMode.snapshot()) {
            Label info = new Label("animated.gif\n" + (int) animated.getWidth() + " x " + (int) animated.getHeight()
                    + "\n(animation shown\noutside snapshots)");
            info.setStyle("-fx-font-size: 11px; -fx-text-fill: #455a64;");
            cells.add(cell("animated GIF (snapshot)", info));
        } else {
            cells.add(cell("animated GIF", view(animated, 96, 96, true, true)));
        }

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        for (int i = 0; i < cells.size(); i++) {
            grid.add(cells.get(i), i % 6, i / 6);
        }

        checks.add(Check.info("sizes png/jpg/gif/bmp/data/stream", String.join(", ", size(png), size(jpg), size(gif),
                size(bmp), size(data), size(stream))));
        checks.add(Checks.run("ARGB png(10,10) gif(10,10) bmp(10,10)", () -> String.join(", ", argb(png, 10, 10),
                argb(gif, 10, 10), argb(bmp, 10, 10))));
        checks.add(Checks.run("ARGB png(200,60) gif(200,60) bmp(200,60)", () -> String.join(", ", argb(png, 200, 60),
                argb(gif, 200, 60), argb(bmp, 200, 60))));
        checks.add(Checks.run("ARGB jpg(40,40) jpg(240,160) stream(240,160)", () -> String.join(", ", argb(jpg, 40, 40),
                argb(jpg, 240, 160), argb(stream, 240, 160))));
        checks.add(Checks.run("ARGB data (1,1) (6,1) (1,6) (6,6)", () -> String.join(", ", argb(data, 1, 1), argb(data, 6,
                1), argb(data, 1, 6), argb(data, 6, 6))));
        checks.add(Check.info("requested sizes (smooth / rough / ratio)", String.join(", ", size(requestedSmooth),
                size(requestedRough), size(requestedRatio))));
        checks.add(Checks.run("requested smooth vs rough ARGB (16,16)", () -> argb(requestedSmooth, 16, 16) + " vs "
                + argb(requestedRough, 16, 16)));
        checks.add(Check.of("missing image isError / exception", missing.isError(), missing.isError() + ", "
                + (missing.getException() == null ? "null" : missing.getException().getClass().getName())));
        checks.add(Check.of("invalid data URI isError / exception", invalid.isError(), invalid.isError() + ", "
                + (invalid.getException() == null ? "null" : Checks.describe(invalid.getException()))));
        checks.add(Check.info("animated GIF size / progress / error", size(animated) + ", " + animated.getProgress()
                + ", " + animated.isError()));

        VBox checksHolder = new VBox(Checks.view("Image loading", checks));
        VBox root = new VBox(8, grid, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        CompletionStage<?> ready = Fx.when(background.progressProperty(), p -> p.doubleValue() >= 1.0)
                .thenCompose(p -> Fx.pulses(1))
                .thenRun(() -> {
                    List<Check> all = new ArrayList<>(checks);
                    all.add(Check.of("background image progress / error / size", !background.isError(),
                            background.getProgress() + ", " + background.isError() + ", " + size(background)));
                    checksHolder.getChildren().setAll(Checks.view("Image loading", all));
                });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }

    static ImageView view(Image image, double fitWidth, double fitHeight, boolean preserveRatio, boolean smooth) {
        ImageView view = new ImageView(image);
        view.setFitWidth(fitWidth);
        view.setFitHeight(fitHeight);
        view.setPreserveRatio(preserveRatio);
        view.setSmooth(smooth);
        return view;
    }

    static VBox cell(String caption, Node content) {
        StackPane holder = new StackPane(content);
        holder.setPrefSize(CELL, 110);
        holder.setMinSize(CELL, 110);
        holder.setMaxSize(CELL, 110);
        holder.setAlignment(Pos.CENTER);
        VBox tile = Ui.tile(caption, holder);
        tile.setPrefWidth(CELL + 14);
        tile.setMaxWidth(CELL + 14);
        return tile;
    }

    static String size(Image image) {
        return (int) image.getWidth() + "x" + (int) image.getHeight();
    }

    static String argb(Image image, int x, int y) {
        return Ui.argb(image.getPixelReader().getArgb(x, y));
    }
}
