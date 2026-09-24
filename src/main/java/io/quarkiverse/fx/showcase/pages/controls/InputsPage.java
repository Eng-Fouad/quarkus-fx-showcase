package io.quarkiverse.fx.showcase.pages.controls;

import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.caption;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.demo;
import static io.quarkiverse.fx.showcase.pages.controls.ControlsUi.grow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import javafx.util.converter.NumberStringConverter;

@Singleton
public class InputsPage implements FeaturePage {

    private static final String WRAPPED_TEXT = "TextArea with wrapText=true : this paragraph is long enough to be "
            + "wrapped on several lines. Quarkus FX integrates JavaFX with Quarkus : CDI injection of FXML controllers, "
            + "a JavaFX application started by the extension and native executables built with GraalVM.";

    @Override
    public String id() {
        return "controls-inputs";
    }

    @Override
    public String title() {
        return "Text Inputs, Spinners & Sliders";
    }

    @Override
    public String category() {
        return Categories.CONTROLS;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Node build() {
        ControlsUi.ChecksHolder checks = new ControlsUi.ChecksHolder();
        List<Check> early = checks.early;

        // TextFields
        TextField prompt = new TextField();
        prompt.setPromptText("Prompt text (empty field)");
        TextField plain = new TextField("Plain text");
        TextField right = new TextField("123.45");
        right.setAlignment(Pos.CENTER_RIGHT);
        TextField centered = new TextField("Centered");
        centered.setAlignment(Pos.CENTER);
        TextField promptStyled = new TextField();
        promptStyled.setPromptText("Styled prompt text");
        promptStyled.setStyle("-fx-prompt-text-fill: #8e24aa; -fx-font-style: italic;");
        TextField disabled = new TextField("Disabled");
        disabled.setDisable(true);
        TextField readOnly = new TextField("Not editable");
        readOnly.setEditable(false);
        TextField styled = new TextField("Styled field");
        styled.setStyle("-fx-control-inner-background: #fff8e1; -fx-text-fill: #bf360c; -fx-font-weight: bold;");
        PasswordField password = new PasswordField();
        password.setText("secret");
        PasswordField passwordPrompt = new PasswordField();
        passwordPrompt.setPromptText("Password");
        GridPane fields = new GridPane(8, 6);
        fields.addRow(0, prompt, plain);
        fields.addRow(1, right, centered);
        fields.addRow(2, promptStyled, styled);
        fields.addRow(3, disabled, readOnly);
        fields.addRow(4, password, passwordPrompt);
        for (Node n : fields.getChildren()) {
            ((TextField) n).setPrefColumnCount(11);
        }
        early.add(Checks.expect("PasswordField text / characters", "6 chars",
                () -> password.getText().length() + " chars"));
        early.add(Checks.expect("TextField selectRange / selected text", "Selected", () -> {
            TextField field = new TextField("Selected range");
            field.selectRange(0, 8);
            return field.getSelectedText();
        }));

        // TextAreas
        TextArea wrapped = new TextArea(WRAPPED_TEXT);
        wrapped.setWrapText(true);
        wrapped.setPrefColumnCount(22);
        wrapped.setPrefRowCount(5);
        StringBuilder lines = new StringBuilder();
        for (int i = 1; i <= 40; i++) {
            lines.append(String.format(Locale.ROOT, "Line %02d : a line that is wider than the text area viewport, "
                    + "to scroll horizontally too%n", i));
        }
        TextArea scrolled = new TextArea(lines.toString().stripTrailing());
        scrolled.setId("scrolled-area");
        scrolled.setPrefColumnCount(22);
        scrolled.setPrefRowCount(5);

        // Spinners
        Spinner<Integer> intSpinner = new Spinner<>(0, 100, 42, 5);
        Spinner<Double> doubleSpinner = new Spinner<>(0.0, 1.0, 0.25, 0.05);
        Spinner<String> listSpinner = new Spinner<>(
                FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
        listSpinner.getValueFactory().setValue("Wednesday");
        Spinner<Integer> editable = new Spinner<>(-50, 50, -7);
        editable.setEditable(true);
        Spinner<Integer> leftVertical = styledSpinner(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL, 7);
        Spinner<Integer> rightHorizontal = styledSpinner(Spinner.STYLE_CLASS_ARROWS_ON_RIGHT_HORIZONTAL, 8);
        Spinner<Integer> leftHorizontal = styledSpinner(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_HORIZONTAL, 9);
        Spinner<Integer> splitVertical = styledSpinner(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL, 10);
        Spinner<Integer> splitHorizontal = styledSpinner(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL, 11);
        GridPane spinners = new GridPane(8, 4);
        spinners.addRow(0, caption("Integer"), caption("Double"), caption("List"), caption("Editable"));
        spinners.addRow(1, intSpinner, doubleSpinner, listSpinner, editable);
        spinners.addRow(2, caption("arrows-on-left-vertical"), caption("arrows-on-right-horizontal"),
                caption("arrows-on-left-horizontal"), caption("split-arrows-vertical"));
        spinners.addRow(3, leftVertical, rightHorizontal, leftHorizontal, splitVertical);
        spinners.add(caption("split-arrows-horizontal"), 0, 4);
        spinners.add(splitHorizontal, 0, 5);
        for (Spinner<?> spinner : List.of(intSpinner, doubleSpinner, listSpinner, editable, leftVertical,
                rightHorizontal, leftHorizontal, splitVertical, splitHorizontal)) {
            spinner.setPrefWidth(130);
        }
        early.add(Checks.expect("IntegerSpinnerValueFactory increment(3)", 57, () -> {
            SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    0, 100, 42, 5);
            factory.increment(3);
            return factory.getValue();
        }));
        early.add(Checks.expect("IntegerSpinnerValueFactory max clamp", 100, () -> {
            SpinnerValueFactory.IntegerSpinnerValueFactory factory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                    0, 100, 42, 5);
            factory.increment(50);
            return factory.getValue();
        }));
        early.add(Checks.expect("DoubleSpinnerValueFactory converter", "0.25 > 0.3",
                () -> {
                    SpinnerValueFactory<Double> factory = doubleSpinner.getValueFactory();
                    String before = factory.getConverter().toString(0.25);
                    return before + " > " + factory.getConverter().toString(0.25 + 0.05);
                }));
        early.add(Checks.expect("ListSpinnerValueFactory wrap-around", "Friday > Monday", () -> {
            SpinnerValueFactory.ListSpinnerValueFactory<String> factory = new SpinnerValueFactory.ListSpinnerValueFactory<>(
                    FXCollections.observableArrayList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
            factory.setWrapAround(true);
            factory.setValue("Friday");
            String before = factory.getValue();
            factory.increment(1);
            return before + " > " + factory.getValue();
        }));

        // Sliders
        Slider ticks = new Slider(0, 100, 35);
        ticks.setShowTickMarks(true);
        ticks.setShowTickLabels(true);
        ticks.setMajorTickUnit(25);
        ticks.setMinorTickCount(4);
        ticks.setSnapToTicks(true);
        ticks.setPrefWidth(260);
        Slider formatted = new Slider(0, 1, 0.6);
        formatted.setShowTickLabels(true);
        formatted.setShowTickMarks(true);
        formatted.setMajorTickUnit(0.25);
        formatted.setMinorTickCount(0);
        formatted.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return Math.round(value * 100) + "%";
            }

            @Override
            public Double fromString(String string) {
                return Double.parseDouble(string.replace("%", "")) / 100;
            }
        });
        formatted.setPrefWidth(260);
        Slider plainSlider = new Slider(0, 10, 3);
        plainSlider.setPrefWidth(260);
        Slider disabledSlider = new Slider(0, 10, 7);
        disabledSlider.setDisable(true);
        disabledSlider.setPrefWidth(260);
        Slider vertical = new Slider(0, 50, 20);
        vertical.setOrientation(Orientation.VERTICAL);
        vertical.setShowTickMarks(true);
        vertical.setShowTickLabels(true);
        vertical.setMajorTickUnit(10);
        vertical.setMinorTickCount(1);
        vertical.setPrefHeight(150);
        Slider verticalPlain = new Slider(0, 1, 0.8);
        verticalPlain.setOrientation(Orientation.VERTICAL);
        verticalPlain.setPrefHeight(150);
        VBox horizontalSliders = new VBox(6, caption("ticks, labels, snap to ticks"), ticks,
                caption("label formatter (StringConverter)"), formatted, caption("plain / disabled"), plainSlider,
                disabledSlider);
        HBox sliders = new HBox(12, horizontalSliders, vertical, verticalPlain);
        early.add(Checks.expect("Slider.adjustValue(33) snaps to tick", 35.0, () -> {
            Slider slider = new Slider(0, 100, 0);
            slider.setMajorTickUnit(25);
            slider.setMinorTickCount(4);
            slider.setSnapToTicks(true);
            slider.adjustValue(33);
            return slider.getValue();
        }));
        early.add(Checks.expect("Slider label formatter", "60%", () -> formatted.getLabelFormatter()
                .toString(formatted.getValue())));

        // ScrollBars and separators
        ScrollBar hbar = new ScrollBar();
        hbar.setMin(0);
        hbar.setMax(100);
        hbar.setValue(30);
        hbar.setVisibleAmount(20);
        hbar.setPrefWidth(200);
        ScrollBar vbar = new ScrollBar();
        vbar.setOrientation(Orientation.VERTICAL);
        vbar.setMin(0);
        vbar.setMax(10);
        vbar.setValue(8);
        vbar.setVisibleAmount(3);
        vbar.setPrefHeight(110);
        Separator hsep = new Separator();
        hsep.setPrefWidth(200);
        Separator vsep = new Separator(Orientation.VERTICAL);
        vsep.setPrefHeight(110);
        VBox hStack = new VBox(10, caption("horizontal ScrollBar"), hbar, caption("horizontal Separator"), hsep);
        HBox bars = new HBox(12, hStack, vbar, vsep);
        early.add(Checks.expect("ScrollBar increment / decrement", "30.0 > 31.0 > 30.0", () -> {
            ScrollBar bar = new ScrollBar();
            bar.setMax(100);
            bar.setValue(30);
            String start = String.valueOf(bar.getValue());
            bar.increment();
            String up = String.valueOf(bar.getValue());
            bar.decrement();
            return start + " > " + up + " > " + bar.getValue();
        }));

        // TextFormatter
        UnaryOperator<TextFormatter.Change> digitsOnly = change -> {
            String text = change.getText();
            return text.chars().allMatch(Character::isDigit) ? change : filtered(change, text.replaceAll("\\D", ""));
        };
        TextField digits = new TextField();
        digits.setTextFormatter(new TextFormatter<>(digitsOnly));
        digits.replaceText(0, 0, "a1b2c3 d4");
        UnaryOperator<TextFormatter.Change> upper = change -> {
            change.setText(change.getText().toUpperCase(Locale.ROOT));
            return change;
        };
        TextField upperCase = new TextField();
        upperCase.setTextFormatter(new TextFormatter<>(upper));
        upperCase.replaceText(0, 0, "upper case filter");
        TextFormatter<Integer> intFormatter = new TextFormatter<>(new IntegerStringConverter(), 0, digitsOnly);
        TextField integer = new TextField();
        integer.setTextFormatter(intFormatter);
        integer.setText("0042");
        integer.commitValue();
        Label intValue = new Label();
        intValue.textProperty().bind(intFormatter.valueProperty().asString("value = %d"));
        TextFormatter<Number> numberFormatter = new TextFormatter<>(
                new NumberStringConverter(Locale.US, "#,##0.00"), 1234.5);
        TextField number = new TextField();
        number.setTextFormatter(numberFormatter);
        number.setAlignment(Pos.CENTER_RIGHT);
        for (TextField f : List.of(digits, upperCase, integer, number)) {
            f.setPrefColumnCount(12);
        }
        GridPane formatters = new GridPane(8, 4);
        formatters.addRow(0, caption("digits-only filter"), caption("upper-case filter"));
        formatters.addRow(1, digits, upperCase);
        formatters.addRow(2, caption("IntegerStringConverter"), caption("NumberStringConverter #,##0.00"));
        formatters.addRow(3, new HBox(6, integer, intValue), number);
        early.add(Checks.expect("TextFormatter filter (replaceText)", "1234", digits::getText));
        early.add(Checks.expect("TextFormatter upper-case filter", "UPPER CASE FILTER", upperCase::getText));
        early.add(Checks.expect("TextFormatter converter commit", "42 / 42",
                () -> intFormatter.getValue() + " / " + integer.getText()));
        early.add(Checks.expect("TextFormatter setValue updates text", "1,234.50 > 7.00", () -> {
            TextField field = new TextField();
            TextFormatter<Number> formatter = new TextFormatter<>(new NumberStringConverter(Locale.US, "#,##0.00"),
                    1234.5);
            field.setTextFormatter(formatter);
            String before = field.getText();
            formatter.setValue(7);
            return before + " > " + field.getText();
        }));
        early.add(Checks.expect("IntegerStringConverter.fromString(\" 12 \")", 12,
                () -> new IntegerStringConverter().fromString(" 12 ")));

        HBox row1 = new HBox(8,
                demo("TextField : prompt, text, alignment, CSS, disabled, read-only / PasswordField", fields),
                grow(demo("TextArea : wrapped / scrolled (scrollTop, scrollLeft)", new HBox(8, wrapped, scrolled))));
        HBox row2 = new HBox(8, demo("Spinner : value factories and arrow styles", spinners),
                grow(demo("Slider : horizontal / vertical", sliders)));
        VBox left3 = new VBox(8, demo("TextFormatter : filters and value converters", formatters),
                demo("ScrollBar / Separator", bars));
        left3.setMinWidth(Region.USE_PREF_SIZE);
        HBox row3 = new HBox(10, left3, grow(checks));
        HBox.setHgrow(wrapped, Priority.ALWAYS);
        HBox.setHgrow(scrolled, Priority.ALWAYS);

        VBox root = ControlsUi.page(8, row1, row2, row3);
        root.setPrefWidth(1028);
        return root;
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        TextArea scrolled = (TextArea) content.lookup("#scrolled-area");
        return Fx.pulses(2).thenRunAsync(() -> {
            scrolled.setScrollTop(120);
            scrolled.setScrollLeft(40);
        }, Fx.FX_THREAD).thenCompose(v -> Fx.pulses(2)).thenRunAsync(() -> {
            ControlsUi.ChecksHolder holder = ControlsUi.find(content, ControlsUi.ChecksHolder.class);
            List<Check> late = new ArrayList<>();
            late.add(Checks.expect("TextArea scrollTop / scrollLeft", "120.0 / 40.0",
                    () -> scrolled.getScrollTop() + " / " + scrolled.getScrollLeft()));
            holder.show("Checks", late);
        }, Fx.FX_THREAD);
    }

    private static TextFormatter.Change filtered(TextFormatter.Change change, String text) {
        change.setText(text);
        return change;
    }

    private static Spinner<Integer> styledSpinner(String styleClass, int value) {
        Spinner<Integer> spinner = new Spinner<>(0, 20, value);
        spinner.getStyleClass().add(styleClass);
        return spinner;
    }

}
