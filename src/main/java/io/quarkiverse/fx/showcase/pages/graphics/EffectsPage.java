package io.quarkiverse.fx.showcase.pages.graphics;

import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.W;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.row;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.section;
import static io.quarkiverse.fx.showcase.pages.graphics.Tiles.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.FloatMap;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

@Singleton
public class EffectsPage implements FeaturePage {

    private static final double H = 80;

    @Override
    public String id() {
        return "graphics-effects";
    }

    @Override
    public String title() {
        return "Effects";
    }

    @Override
    public String category() {
        return Categories.GRAPHICS;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return Tiles.warmUp(content);
    }

    @Override
    public Node build() {
        Image photo = Tiles.image("photo.jpg", 96, 64);
        Image icon = Tiles.image("icon.png");
        return Tiles.page(
                section("Shadows"),
                row(dropShadow(BlurType.GAUSSIAN), dropShadow(BlurType.ONE_PASS_BOX), dropShadow(BlurType.TWO_PASS_BOX),
                        dropShadow(BlurType.THREE_PASS_BOX), spreadShadow(), innerShadow(), innerShadowText(), shadow()),
                section("Blur, glow and color"),
                row(photo("GaussianBlur r=6", photo, new GaussianBlur(6)),
                        photo("BoxBlur 8x3, 3 iterations", photo, new BoxBlur(8, 3, 3)),
                        motionBlur(), bloom(),
                        photo("Glow 0.9", photo, new Glow(0.9)),
                        photo("SepiaTone 0.9", photo, new SepiaTone(0.9)),
                        photo("ColorAdjust hue/sat/bright/contrast", photo, new ColorAdjust(0.45, 0.3, -0.1, 0.4)),
                        reflection()),
                section("Lighting, inputs, geometry, chains"),
                row(lighting("Lighting Distant 225°/45°", new Light.Distant(225, 45, Color.WHITE)),
                        lighting("Lighting Point (10,10,40)", new Light.Point(10, 10, 40, Color.WHITE)),
                        lighting("Lighting Spot → (70,45)", spot()),
                        colorInput(), imageInput(icon), displacement(photo), perspective(photo), chained(photo)),
                section("CSS effects, effects on groups, nested effects"),
                row(cssCard(), cssText(), groupShadow(), rotatedShadow(), nested(), imageLighting(photo), doubleShadowText(),
                        groupReflection(photo)),
                Tiles.checks(Checks.view("CSS effects and effect bounds", boundsChecks()),
                        Checks.view("Rendered pixels (Node.snapshot)", pixelChecks())));
    }

    // ---- Shadows ----

    private static Rectangle card(double w, double h, String color) {
        Rectangle rect = new Rectangle(w, h, Color.web(color));
        rect.setArcWidth(14);
        rect.setArcHeight(14);
        return rect;
    }

    private static Text text(String value, double size, Color fill) {
        Text text = new Text(value);
        text.setFont(Font.font("System", FontWeight.BOLD, size));
        text.setFill(fill);
        return text;
    }

    private static Node dropShadow(BlurType type) {
        Rectangle rect = card(62, 42, "#81d4fa");
        rect.setEffect(new DropShadow(type, Color.rgb(0, 0, 0, 0.75), 14, 0, 5, 5));
        return tile("DropShadow " + type, W, H, rect);
    }

    private static Node spreadShadow() {
        Text text = text("Aa", 40, Color.web("#1565c0"));
        text.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web("#ff9800"), 8, 0.6, 0, 0));
        return tile("DropShadow spread 0.6 on Text", W, H, text);
    }

    private static Node innerShadow() {
        Rectangle rect = card(70, 48, "#eceff1");
        rect.setEffect(new InnerShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.8), 14, 0, 4, 4));
        return tile("InnerShadow", W, H, rect);
    }

    private static Node innerShadowText() {
        Text text = text("FX", 48, Color.web("#90caf9"));
        text.setEffect(new InnerShadow(BlurType.THREE_PASS_BOX, Color.web("#0d47a1"), 8, 0.3, 2, 2));
        return tile("InnerShadow choke 0.3 on Text", W, H, text);
    }

    private static Node shadow() {
        Polygon star = new Polygon(0, -30, 8, -10, 30, -10, 12, 4, 18, 26, 0, 14, -18, 26, -12, 4, -30, -10, -8, -10);
        star.setFill(Color.GOLD);
        star.setEffect(new Shadow(BlurType.GAUSSIAN, Color.web("#6a1b9a"), 10));
        return tile("Shadow (replaces content)", W, H, star);
    }

    // ---- Blur, glow, color ----

    private static Node photo(String caption, Image image, Effect effect) {
        ImageView view = new ImageView(image);
        view.setEffect(effect);
        return tile(caption, W, H, view);
    }

    private static Node motionBlur() {
        Text text = text("Motion", 24, Color.web("#c62828"));
        text.setEffect(new MotionBlur(20, 8));
        return tile("MotionBlur 20°, r=8", W, H, text);
    }

    private static Node bloom() {
        Circle sun = new Circle(34, 24, 14, Color.web("#fff59d"));
        Text text = text("Bloom", 22, Color.WHITE);
        text.setX(2);
        text.setY(62);
        Group group = new Group(sun, text);
        group.setEffect(new Bloom(0.3));
        return tile("Bloom threshold 0.3", W, H, "gfx-stage-dark", group);
    }

    private static Node reflection() {
        Text text = text("Reflect", 24, Color.web("#00695c"));
        text.setEffect(new Reflection(0, 0.8, 0.7, 0));
        return tile("Reflection fraction 0.8", W, H, text);
    }

    // ---- Lighting, inputs, geometry ----

    private static Light.Spot spot() {
        Light.Spot spot = new Light.Spot(0, 0, 70, 2, Color.WHITE);
        spot.setPointsAtX(70);
        spot.setPointsAtY(45);
        spot.setPointsAtZ(0);
        return spot;
    }

    private static Node lighting(String caption, Light light) {
        Rectangle rect = card(90, 58, "#ffb74d");
        Text text = text("FX", 34, Color.web("#e65100"));
        text.setX(26);
        text.setY(42);
        Group group = new Group(rect, text);
        Lighting lighting = new Lighting(light);
        lighting.setSurfaceScale(4);
        group.setEffect(lighting);
        return tile(caption, W, H, group);
    }

    private static Node colorInput() {
        Rectangle rect = new Rectangle(96, 60);
        rect.setEffect(new ColorInput(0, 0, 96, 60, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00c853")), new Stop(1, Color.web("#2962ff")))));
        return tile("ColorInput (gradient)", W, H, rect);
    }

    private static Node imageInput(Image icon) {
        Rectangle rect = new Rectangle(64, 64);
        rect.setEffect(new ImageInput(icon, 0, 0));
        return tile("ImageInput(icon.png)", W, H, rect);
    }

    private static FloatMap wave(int width, int height) {
        FloatMap map = new FloatMap(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map.setSamples(x, y, (float) (0.05 * StrictMath.sin(y * 2 * StrictMath.PI / 16)), 0f);
            }
        }
        return map;
    }

    private static Node displacement(Image photo) {
        ImageView view = new ImageView(photo);
        view.setEffect(new DisplacementMap(wave(96, 64)));
        return tile("DisplacementMap (FloatMap wave)", W, H, view);
    }

    private static PerspectiveTransform perspectiveTransform() {
        return new PerspectiveTransform(6, 2, 90, 16, 90, 50, 6, 66);
    }

    private static Node perspective(Image photo) {
        ImageView view = new ImageView(photo);
        view.setEffect(perspectiveTransform());
        return tile("PerspectiveTransform", W, H, view);
    }

    private static Node chained(Image photo) {
        ImageView view = new ImageView(photo);
        view.setFitWidth(54);
        view.setFitHeight(36);
        // note: a Reflection whose input is an InnerShadow renders no reflection (JavaFX behavior, JVM too)
        ColorAdjust gray = new ColorAdjust(0, -1, 0, 0);
        DropShadow glow = new DropShadow(BlurType.GAUSSIAN, Color.web("#ff6d00"), 6, 0.4, 0, 0);
        glow.setInput(gray);
        Reflection reflection = new Reflection(2, 0.4, 0.6, 0);
        reflection.setInput(glow);
        view.setEffect(reflection);
        return tile("ColorAdjust → DropShadow → Reflection", W, H, view);
    }

    // ---- CSS, groups, nesting ----

    private static Node cssCard() {
        Region region = new Region();
        region.setPrefSize(70, 44);
        region.setStyle("-fx-background-color: #fffde7; -fx-background-radius: 8;");
        region.getStyleClass().add("css-dropshadow");
        return tile("CSS -fx-effect: dropshadow(..)", W, H, region);
    }

    private static Node cssText() {
        Text text = text("CSS", 36, Color.web("#bbdefb"));
        text.getStyleClass().add("css-innershadow");
        return tile("CSS -fx-effect: innershadow(..)", W, H, text);
    }

    private static Node groupShadow() {
        Group group = new Group(new Circle(22, 26, 20, Color.web("#ef5350")), new Circle(46, 26, 20, Color.web("#42a5f5")),
                new Rectangle(20, 36, 50, 18));
        ((Rectangle) group.getChildren().get(2)).setFill(Color.web("#66bb6a"));
        group.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.7), 10, 0, 4, 4));
        return tile("DropShadow on a Group", W, H, group);
    }

    private static Node rotatedShadow() {
        Rectangle rect = card(56, 34, "#ce93d8");
        rect.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.rgb(0, 0, 0, 0.7), 8, 0, 8, 0));
        rect.setRotate(35);
        return tile("DropShadow dx=8, node rotated 35°", W, H, rect);
    }

    private static Node nested() {
        Text inner = text("in", 26, Color.WHITE);
        inner.setX(22);
        inner.setY(40);
        inner.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.BLACK, 4, 0.4, 0, 0));
        Group group = new Group(card(70, 50, "#26a69a"), inner);
        group.setEffect(new GaussianBlur(2.5));
        return tile("GaussianBlur ⊃ DropShadow (nested)", W, H, group);
    }

    private static Node imageLighting(Image photo) {
        ImageView view = new ImageView(photo);
        Lighting lighting = new Lighting(new Light.Distant(45, 60, Color.web("#fff3e0")));
        lighting.setSurfaceScale(2);
        lighting.setSpecularConstant(0.6);
        view.setEffect(lighting);
        return tile("Lighting on an image", W, H, view);
    }

    private static Node doubleShadowText() {
        Text text = text("Duo", 36, Color.web("#fffde7"));
        InnerShadow inner = new InnerShadow(BlurType.GAUSSIAN, Color.web("#f57f17"), 6, 0, 1, 1);
        DropShadow drop = new DropShadow(BlurType.GAUSSIAN, Color.web("#3e2723"), 5, 0.2, 2, 3);
        drop.setInput(inner);
        text.setEffect(drop);
        return tile("InnerShadow → DropShadow on Text", W, H, text);
    }

    private static Node groupReflection(Image photo) {
        ImageView view = new ImageView(photo);
        view.setFitWidth(60);
        view.setFitHeight(40);
        Text label = text("sky", 14, Color.WHITE);
        label.setX(4);
        label.setY(36);
        Group group = new Group(view, label);
        group.setEffect(new Reflection(1, 0.6, 0.8, 0.1));
        return tile("Reflection of a Group", W, H, group);
    }

    // ---- Checks ----

    private static List<Check> boundsChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.run("CSS dropshadow type r spread", () -> {
            DropShadow ds = (DropShadow) Tiles.css(new Region(), null, "css-dropshadow").getEffect();
            return ds.getBlurType() + " " + Tiles.num(ds.getRadius()) + " " + Tiles.num(ds.getSpread());
        }));
        checks.add(Checks.run("CSS dropshadow offset, color", () -> {
            DropShadow ds = (DropShadow) Tiles.css(new Region(), null, "css-dropshadow").getEffect();
            return Tiles.num(ds.getOffsetX()) + "," + Tiles.num(ds.getOffsetY()) + " " + Tiles.hex(ds.getColor());
        }));
        checks.add(Checks.run("CSS innershadow type r choke", () -> {
            InnerShadow is = (InnerShadow) Tiles.css(new Text("x"), null, "css-innershadow").getEffect();
            return is.getBlurType() + " " + Tiles.num(is.getRadius()) + " " + Tiles.num(is.getChoke()) + " "
                    + Tiles.hex(is.getColor());
        }));
        checks.add(Checks.run("40x40 + GaussianBlur(10) bounds", () -> effectBounds(new GaussianBlur(10))));
        checks.add(Checks.run("40x40 + DropShadow(10, dx=5, dy=5)", () -> effectBounds(
                new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, 10, 0, 5, 5))));
        checks.add(Checks.run("40x40 + PerspectiveTransform bounds", () -> effectBounds(perspectiveTransform())));
        return checks;
    }

    private static String effectBounds(Effect effect) {
        Rectangle rect = new Rectangle(40, 40, Color.RED);
        rect.setEffect(effect);
        return Tiles.bounds(rect.getBoundsInLocal());
    }

    private static List<Check> pixelChecks() {
        List<Check> checks = new ArrayList<>();
        checks.add(Checks.run("SepiaTone(1) on red, center", () -> pixel(new SepiaTone(1), 20, 20)));
        checks.add(Checks.run("ColorAdjust(hue .5) on red, center", () -> pixel(new ColorAdjust(0.5, 0, 0, 0), 20, 20)));
        checks.add(Checks.run("GaussianBlur(10) on red, corner", () -> pixel(new GaussianBlur(10), 10, 10)));
        checks.add(Checks.run("Lighting Distant on red, center",
                () -> pixel(new Lighting(new Light.Distant(45, 45, Color.WHITE)), 20, 20)));
        checks.add(Checks.run("MotionBlur(0, 10) on red, edge", () -> pixel(new MotionBlur(0, 10), 5, 20)));
        checks.add(Checks.run("Bloom(0) on red, center", () -> pixel(new Bloom(0), 20, 20)));
        return checks;
    }

    private static String pixel(Effect effect, int x, int y) {
        Rectangle rect = new Rectangle(40, 40, Color.RED);
        rect.setEffect(effect);
        return Tiles.pixel(rect, x, y);
    }
}
