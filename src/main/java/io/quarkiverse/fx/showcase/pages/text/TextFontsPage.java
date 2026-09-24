package io.quarkiverse.fx.showcase.pages.text;

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
import javafx.css.CssParser;
import javafx.css.Stylesheet;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Fonts : loading application fonts (URL, InputStream, CSS @font-face), icon fonts, system font families and font
 * resolution by family, weight and posture.
 */
@Singleton
public class TextFontsPage implements FeaturePage {

    private static final String READY = "text-fonts.ready";
    private static final String STYLESHEET = "/showcase/text/fonts.css";

    /** Font Awesome 5 Free Solid code points, with their names. */
    static final String[][] ICONS = { { "\uf015", "home" }, { "\uf004", "heart" }, { "\uf013", "cog" },
            { "\uf0e0", "envelope" }, { "\uf007", "user" }, { "\uf002", "search" }, { "\uf1b9", "car" },
            { "\uf0f4", "coffee" }, { "\uf0c2", "cloud" }, { "\uf135", "rocket" }, { "\uf0e7", "bolt" },
            { "\uf005", "star" }, { "\uf06e", "eye" }, { "\uf023", "lock" }, { "\uf0f3", "bell" },
            { "\uf2dc", "snowflake" } };
    static final Color[] ICON_COLORS = { Color.web("#1565c0"), Color.web("#d81b60"), Color.web("#546e7a"),
            Color.web("#ef6c00"), Color.web("#2e7d32"), Color.web("#6a1b9a") };

    static final String[] SYSTEM_FAMILIES = { "Helvetica", "Helvetica Neue", "Times New Roman", "Georgia", "Menlo",
            "Courier New", "Avenir Next", "Optima", "Palatino", "Marker Felt", "Serif", "SansSerif", "Monospaced",
            "System", "NoSuchFamily" };

    @Override
    public String id() {
        return "text-fonts";
    }

    @Override
    public String title() {
        return "Fonts";
    }

    @Override
    public String category() {
        return Categories.TEXT;
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Node build() {
        List<Check> checks = new ArrayList<>();
        Font roboto = LoadedFonts.roboto();
        Font kufi = LoadedFonts.kufi();
        Font awesome = LoadedFonts.awesome();
        String robotoFamily = LoadedFonts.family(roboto, "Roboto");
        String kufiFamily = LoadedFonts.family(kufi, "Droid Arabic Kufi");
        String awesomeName = awesome == null ? "Font Awesome 5 Free Solid" : awesome.getName();

        checks.add(Check.of("Roboto Font.loadFont(url)", roboto != null, LoadedFonts.describeRoboto()));
        checks.add(Check.of("DroidKufi Font.loadFont(InputStream)", kufi != null, LoadedFonts.describeKufi()));
        checks.add(Check.of("Font Awesome Font.loadFonts(url)", awesome != null, LoadedFonts.describeAwesome()));

        // Row 1 : Roboto (URL) | Droid Kufi (InputStream)
        VBox robotoBox = new VBox(0);
        for (double size : new double[] { 13, 20, 30 }) {
            Text t = new Text("Roboto Light " + (int) size + " : The quick brown fox");
            t.setFont(Font.font(robotoFamily, size));
            robotoBox.getChildren().add(t);
        }
        VBox robotoTile = Ui.grow(Ui.tile("Roboto Light, Font.loadFont(classpath URL, 20), Font.font(family, size)",
                robotoBox));

        Text kufiText = new Text("خط الكوفي العربي");
        kufiText.setFont(Font.font(kufiFamily, 26));
        Text kufiLatin = new Text("Droid Arabic Kufi: Latin 0123456789");
        kufiLatin.setFont(Font.font(kufiFamily, 14));
        VBox kufiTile = Ui.grow(Ui.tile("Droid Arabic Kufi, Font.loadFont(InputStream, 20)", kufiText, kufiLatin));
        HBox row1 = new HBox(8, robotoTile, kufiTile);

        // Row 2 : icon font
        GridPane icons = new GridPane();
        icons.setHgap(6);
        icons.setVgap(0);
        for (int i = 0; i < ICONS.length; i++) {
            Text glyph = new Text(ICONS[i][0]);
            glyph.setFont(new Font(awesomeName, 26));
            glyph.setFill(ICON_COLORS[i % ICON_COLORS.length]);
            Label name = Ui.caption(ICONS[i][1] + String.format(" %04x", (int) ICONS[i][0].charAt(0)));
            VBox cell = new VBox(0, glyph, name);
            cell.setAlignment(Pos.CENTER);
            cell.setMinWidth(56);
            icons.add(cell, i, 0);
        }
        HBox sizes = new HBox(8);
        sizes.setAlignment(Pos.BASELINE_LEFT);
        for (double size : new double[] { 12, 18, 26, 40 }) {
            Text glyph = new Text("\uf004");
            glyph.setFont(new Font(awesomeName, size));
            glyph.setFill(Color.web("#e53935"));
            sizes.getChildren().add(glyph);
        }
        Text stroked = new Text("\uf005\uf135");
        stroked.setFont(new Font(awesomeName, 40));
        stroked.setFill(Color.web("#fff176"));
        stroked.setStroke(Color.web("#f57f17"));
        stroked.setStrokeWidth(1.5);
        sizes.getChildren().add(stroked);
        VBox iconTile = Ui.tile("Font Awesome 5 Free Solid icon glyphs, new Font(name, size), sizes 12 .. 40 and stroked",
                icons, sizes);

        // Row 3 : system families | CSS @font-face
        GridPane families = new GridPane();
        families.setHgap(14);
        families.setVgap(1);
        List<String> resolved = new ArrayList<>();
        for (int i = 0; i < SYSTEM_FAMILIES.length; i++) {
            String family = SYSTEM_FAMILIES[i];
            Font font = Font.font(family, 15);
            Text t = new Text(family + " Aa Gg 123");
            t.setFont(font);
            families.add(t, i % 3, i / 3);
            resolved.add(family + "→" + font.getName());
        }
        VBox familiesTile = Ui.tile("System and logical families, Font.font(family, 15) (NoSuchFamily falls back)",
                families);

        Label cssRoboto = new Label("Roboto Light from @font-face");
        cssRoboto.getStyleClass().add("roboto-css");
        Label cssAwesome = new Label("\uf015 \uf004 \uf013 \uf0e0 \uf007");
        cssAwesome.getStyleClass().add("awesome-css");
        Label cssShorthand = new Label("-fx-font: bold italic 16px Georgia");
        cssShorthand.getStyleClass().add("font-shorthand-css");
        VBox cssTile = Ui.grow(Ui.tile("Stylesheet fonts.css : @font-face url(..), -fx-font-family, -fx-font", cssRoboto,
                cssAwesome, cssShorthand));
        cssTile.getStylesheets().add(Fx.resourceUrl(STYLESHEET));
        HBox row3 = new HBox(8, familiesTile, cssTile);

        // Checks
        checks.add(Checks.run("Font.getFontNames(Roboto / Roboto Light / FA)", () -> Font.getFontNames("Roboto")
                + " / " + Font.getFontNames("Roboto Light") + " / " + Font.getFontNames("Font Awesome 5 Free Solid")));
        checks.add(Checks.expect("Font.getFamilies() contains", "Helvetica=true, Menlo=true, Times New Roman=true, "
                + "Roboto=true, Droid Arabic Kufi=true, Font Awesome 5 Free Solid=true", () -> {
                    List<String> all = Font.getFamilies();
                    return String.join(", ", List.of("Helvetica", "Menlo", "Times New Roman", "Roboto",
                            "Droid Arabic Kufi", "Font Awesome 5 Free Solid").stream().map(f -> f + "=" + all.contains(f))
                            .toList());
                }));
        checks.add(Check.info("Font.font(family, 15).getName() (1)", String.join(", ", resolved.subList(0, 8))));
        checks.add(Check.info("Font.font(family, 15).getName() (2)", String.join(", ", resolved.subList(8,
                resolved.size()))));
        checks.add(Checks.run("Font.font(family, weight, posture, 14).getName()", () -> String.join(", ",
                Font.font("Helvetica", FontWeight.BOLD, FontPosture.ITALIC, 14).getName(),
                Font.font("Times New Roman", FontWeight.BOLD, FontPosture.REGULAR, 14).getName(),
                Font.font("Menlo", FontWeight.NORMAL, FontPosture.ITALIC, 14).getName(),
                Font.font("Courier New", FontWeight.BOLD, FontPosture.ITALIC, 14).getName(),
                Font.font("Georgia", FontWeight.LIGHT, FontPosture.ITALIC, 14).getName(),
                Font.font("Monospaced", FontWeight.BOLD, FontPosture.REGULAR, 14).getName())));
        checks.add(Checks.run("Font.getDefault()", () -> Font.getDefault().getName() + ", " + Font.getDefault().getFamily()
                + ", " + Ui.num(Font.getDefault().getSize())));
        checks.add(Checks.run("invalid fonts (PNG stream / missing URL)", () -> {
            Font fromPng;
            try (InputStream in = Fx.resource("/showcase/images/icon.png").openStream()) {
                fromPng = Font.loadFont(in, 12);
            }
            Font missing = Font.loadFont(Fx.resourceUrl(LoadedFonts.ROBOTO).replace("Roboto-Light.ttf", "Missing.ttf"),
                    12);
            return fromPng + " / " + missing;
        }));
        checks.add(Checks.run("CssParser.parse(fonts.css)", () -> {
            Stylesheet sheet = new CssParser().parse(Fx.resource(STYLESHEET));
            return sheet.getFontFaces().size() + " @font-face, " + sheet.getRules().size() + " rules";
        }));

        VBox checksHolder = new VBox(Checks.view("Font loading and resolution", checks));
        VBox root = new VBox(8, row1, iconTile, row3, checksHolder);
        root.setPrefWidth(1028);
        root.setMaxWidth(1028);

        CompletionStage<?> ready = Fx.pulses(2).thenRun(() -> {
            List<Check> all = new ArrayList<>(checks);
            all.add(Check.info("fonts of the CSS styled labels", String.join(", ", cssRoboto.getFont().getName() + " "
                    + Ui.num(cssRoboto.getFont().getSize()), cssAwesome.getFont().getName() + " "
                            + Ui.num(cssAwesome.getFont().getSize()),
                    cssShorthand.getFont().getName() + " " + Ui.num(cssShorthand.getFont().getSize()))));
            checksHolder.getChildren().setAll(Checks.view("Font loading and resolution", all));
        });
        root.getProperties().put(READY, ready);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return (CompletionStage<?>) content.getProperties().get(READY);
    }
}
