package io.quarkiverse.fx.showcase.pages.media;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Singleton;

import io.quarkiverse.fx.showcase.core.Categories;
import io.quarkiverse.fx.showcase.core.Check;
import io.quarkiverse.fx.showcase.core.Checks;
import io.quarkiverse.fx.showcase.core.FeaturePage;
import io.quarkiverse.fx.showcase.core.Fx;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Video : MediaView of an H.264 clip paused at 1.5 s, several MediaViews sharing the same MediaPlayer (fit,
 * preserveRatio, viewport, effects, clip).
 */
@Singleton
public class VideoPage implements FeaturePage {

    private static final String STATE = VideoPage.class.getName();
    private static final Duration SEEK = Duration.seconds(1.5);
    private static final Duration[] STRIP = { Duration.ZERO, Duration.seconds(1), Duration.seconds(2) };

    @Override
    public String id() {
        return "media-video";
    }

    @Override
    public String title() {
        return "Video";
    }

    @Override
    public String category() {
        return Categories.MEDIA;
    }

    @Override
    public int order() {
        return 20;
    }

    private static final class State {
        Media media;
        MediaPlayer player;
        final MediaView main = new MediaView();
        final List<MediaView> variants = new ArrayList<>();
        final List<ImageView> strip = new ArrayList<>();
        final Label statusLabel = new Label("WAITING");
        final Label time = new Label("-:--.- / -:--.-");
        final Slider position = new Slider(0, 1, 0);
        final VBox videoChecks = new VBox();
        final VBox viewChecks = new VBox();
        int readyEvents;
        String error;
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();
        try {
            String uri = Fx.resourceToTempFile("/showcase/media/clip.mp4").toUri().toString();
            state.media = new Media(uri);
            state.media.getMarkers().put("start", Duration.seconds(0.5));
            state.media.getMarkers().put("end", Duration.seconds(2.5));
            state.player = new MediaPlayer(state.media);
            state.player.setVolume(0);
            state.player.setMute(true);
            state.player.setOnReady(() -> state.readyEvents++);
            state.main.setMediaPlayer(state.player);
        } catch (Throwable t) {
            state.error = Checks.describe(t);
        }

        GridPane variants = new GridPane();
        variants.setHgap(14);
        variants.setVgap(10);
        addVariant(state, variants, 0, 0, "fitWidth 192, preserveRatio", view -> view.setFitWidth(192));
        addVariant(state, variants, 1, 0, "fit 120x112, preserveRatio=false", view -> {
            view.setFitWidth(120);
            view.setFitHeight(112);
            view.setPreserveRatio(false);
        });
        addVariant(state, variants, 2, 0, "fit 192x72, preserveRatio", view -> {
            view.setFitWidth(192);
            view.setFitHeight(72);
        });
        addVariant(state, variants, 0, 1, "viewport 128x72 at (96, 54), fitWidth 192", view -> {
            view.setViewport(new Rectangle2D(96, 54, 128, 72));
            view.setFitWidth(192);
        });
        addVariant(state, variants, 1, 1, "rotate -6°, DropShadow, fitWidth 160", view -> {
            view.setFitWidth(160);
            view.setRotate(-6);
            view.setEffect(new DropShadow(10, 3, 3, Color.rgb(0, 0, 0, 0.6)));
        });
        addVariant(state, variants, 2, 1, "ColorAdjust grayscale, rounded clip", view -> {
            view.setFitWidth(192);
            view.setEffect(new ColorAdjust(0, -1, 0, 0.2));
            Rectangle clip = new Rectangle(192, 108);
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            view.setClip(clip);
        });

        state.videoChecks.getChildren().add(new Label("Waiting for the player..."));
        state.videoChecks.setPrefWidth(506);
        state.viewChecks.setPrefWidth(506);
        HBox.setHgrow(state.videoChecks, Priority.ALWAYS);
        HBox.setHgrow(state.viewChecks, Priority.ALWAYS);
        HBox checks = new HBox(16, state.videoChecks, state.viewChecks);

        HBox top = new HBox(16, mainCard(state), variants);
        HBox strip = new HBox(16);
        for (Duration time : STRIP) {
            ImageView view = new ImageView();
            view.setFitWidth(160);
            view.setFitHeight(90);
            state.strip.add(view);
            StackPane frame = new StackPane(view);
            frame.getStyleClass().add("video-frame");
            frame.setPrefSize(162, 92);
            frame.setMinSize(162, 92);
            frame.setMaxSize(162, 92);
            strip.getChildren().add(new VBox(4, MediaSupport.caption("seek(" + MediaSupport.seconds(time) + ")"), frame));
        }
        Label stripCaption = new Label("MediaView.snapshot() of the paused player after each seek, before the final "
                + "seek(" + MediaSupport.seconds(SEEK) + ").\nOn macOS, AVFoundation seeks to whole seconds.");
        stripCaption.getStyleClass().add("media-caption");
        stripCaption.setWrapText(true);
        stripCaption.setPrefWidth(300);
        strip.getChildren().add(stripCaption);
        VBox root = new VBox(12,
                MediaSupport.caption("clip.mp4 (H.264 320x180, 3 s, muted) : pause() then seek("
                        + MediaSupport.seconds(SEEK) + "), every MediaView shares the same MediaPlayer"),
                top, strip, checks);
        root.getStylesheets().add(Fx.resourceUrl(MediaSupport.STYLESHEET));
        root.getProperties().put(STATE, state);

        state.ready = state.player == null ? CompletableFuture.completedFuture(null).thenApply(v -> {
            showChecks(state);
            return null;
        }) : prepare(state);
        return root;
    }

    private static CompletionStage<?> prepare(State state) {
        MediaPlayer player = state.player;
        CompletionStage<?> chain = MediaSupport.ready(player, 15_000)
                .thenCompose(status -> {
                    if (status != MediaPlayer.Status.READY) {
                        throw new IllegalStateException(status + ": " + MediaSupport.playerError(player));
                    }
                    player.pause();
                    return MediaSupport.until(() -> player.getStatus() == MediaPlayer.Status.PAUSED, 10_000, "PAUSED");
                });
        // frames at whole seconds, captured with MediaView.snapshot()
        for (int i = 0; i < STRIP.length; i++) {
            int index = i;
            chain = chain.thenCompose(v -> seekAndSettle(state, STRIP[index]))
                    .thenAccept(image -> state.strip.get(index).setImage(image));
        }
        // final position
        return chain.thenCompose(v -> seekAndSettle(state, SEEK))
                .thenCompose(v -> Fx.pulses(5))
                .handle((v, error) -> {
                    if (error != null) {
                        state.error = MediaSupport.describe(error);
                    }
                    updateControls(state);
                    showChecks(state);
                    return null;
                })
                .thenCompose(v -> Fx.pulses(5));
    }

    /**
     * Seeks, waits until the current time left its previous value and is stable (the position reached can differ from
     * the requested one : AVFoundation seeks to whole seconds on macOS), then until the main MediaView renders the
     * same frame twice, with the background color of the clip at that time (red, green then blue every second).
     */
    private static CompletionStage<WritableImage> seekAndSettle(State state, Duration time) {
        MediaPlayer player = state.player;
        double before = player.getCurrentTime().toSeconds();
        boolean moves = Math.abs(before - time.toSeconds()) >= 0.5;
        player.seek(time);
        double[] last = { -1 };
        int[] stable = { 0 };
        return MediaSupport.until(() -> {
            double now = player.getCurrentTime().toSeconds();
            stable[0] = now == last[0] ? stable[0] + 1 : 0;
            last[0] = now;
            return stable[0] >= 10 && Math.abs(now - time.toSeconds()) < 0.6 && (!moves || now != before);
        }, 10_000, "current time after seek(" + MediaSupport.seconds(time) + ")")
                .thenCompose(v -> Fx.delay(200))
                .thenCompose(v -> stableSnapshot(state.main, expectedColor(player.getCurrentTime())));
    }

    /**
     * Background color of the clip at {@code time}.
     */
    private static String expectedColor(Duration time) {
        int second = (int) Math.floor(time.toSeconds() + 0.001);
        return second <= 0 ? "red" : second == 1 ? "green" : "blue";
    }

    /**
     * Snapshots {@code view} every few pulses until two consecutive snapshots are identical and show the expected
     * background color (the frame of the new position was rendered).
     */
    private static CompletionStage<WritableImage> stableSnapshot(MediaView view, String color) {
        java.util.concurrent.CompletableFuture<WritableImage> done = new java.util.concurrent.CompletableFuture<>();
        int[][] previous = { null };
        int[] identical = { 0 };
        int[] attempts = { 0 };
        WritableImage[] image = { null };
        MediaSupport.until(() -> {
            if (++attempts[0] % 4 != 0) {
                return false;
            }
            image[0] = view.snapshot(new SnapshotParameters(), null);
            int width = (int) image[0].getWidth();
            int height = (int) image[0].getHeight();
            int[] pixels = new int[width * height];
            image[0].getPixelReader().getPixels(0, 0, width, height,
                    javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
            identical[0] = java.util.Arrays.equals(pixels, previous[0]) ? identical[0] + 1 : 0;
            previous[0] = pixels;
            return identical[0] >= 2 && color.equals(dominant(image[0]));
        }, 5_000, "a stable " + color + " video frame").whenComplete((v, error) -> {
            if (error != null) {
                done.completeExceptionally(error);
            } else {
                done.complete(image[0]);
            }
        });
        return done;
    }

    private interface Variant {
        void configure(MediaView view);
    }

    private static void addVariant(State state, GridPane grid, int column, int row, String caption, Variant variant) {
        MediaView view = new MediaView(state.player);
        variant.configure(view);
        view.getProperties().put("caption", caption);
        state.variants.add(view);
        StackPane frame = new StackPane(view);
        frame.setPrefSize(206, 116);
        frame.setMinSize(206, 116);
        frame.setMaxSize(206, 116);
        frame.setStyle("-fx-background-color: #d9e2ec; -fx-background-radius: 6;");
        Label label = MediaSupport.caption(caption);
        label.setMaxWidth(206);
        grid.add(new VBox(4, label, frame), column, row);
    }

    private static VBox mainCard(State state) {
        Label title = new Label("MediaView, natural size");
        title.getStyleClass().add("media-card-title");
        state.statusLabel.getStyleClass().addAll("media-status", "waiting");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox header = MediaSupport.fixHeight(new HBox(8, title, grow, state.statusLabel), 24);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane frame = new StackPane(state.main);
        frame.getStyleClass().add("video-frame");
        frame.setPrefSize(322, 182);
        frame.setMinSize(322, 182);
        frame.setMaxSize(322, 182);

        Button play = MediaSupport.button(MediaSupport.PLAY, ">");
        play.getStyleClass().add("primary");
        Button pause = MediaSupport.button(MediaSupport.PAUSE, "||");
        Button stop = MediaSupport.button(MediaSupport.STOP, "[]");
        ToggleButton mute = MediaSupport.toggle(MediaSupport.MUTE, "M");
        state.time.getStyleClass().add("media-time");
        state.position.setFocusTraversable(false);
        Region grow2 = new Region();
        HBox.setHgrow(grow2, Priority.ALWAYS);
        HBox buttons = MediaSupport.fixHeight(new HBox(6, play, pause, stop, grow2, state.time, mute), 30);
        buttons.setAlignment(Pos.CENTER_LEFT);
        MediaSupport.fixHeight(state.position, 20);

        MediaPlayer player = state.player;
        if (player != null) {
            play.setOnAction(e -> player.play());
            pause.setOnAction(e -> player.pause());
            stop.setOnAction(e -> player.stop());
            mute.selectedProperty().bindBidirectional(player.muteProperty());
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.currentTimeProperty().addListener((o, oldTime, newTime) -> {
                if (!state.position.isValueChanging()) {
                    state.position.setValue(newTime.toSeconds());
                }
                state.time.setText(MediaSupport.time(newTime) + " / " + MediaSupport.time(player.getMedia().getDuration()));
            });
            state.position.valueChangingProperty().addListener((o, was, changing) -> {
                if (!changing) {
                    player.seek(Duration.seconds(state.position.getValue()));
                }
            });
        }

        VBox card = new VBox(header, frame, state.position, buttons);
        card.getStyleClass().add("media-card");
        // no drop shadow : its blur of the card, video included, differs by 1 level between runs at its edges
        card.setStyle("-fx-effect: null;");
        card.setPrefWidth(344);
        card.setMinWidth(344);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private static void updateControls(State state) {
        MediaPlayer player = state.player;
        boolean ok = state.error == null && player != null;
        state.statusLabel.getStyleClass().removeAll("waiting", "failed");
        if (!ok) {
            state.statusLabel.getStyleClass().add("failed");
        }
        state.statusLabel.setText(player == null ? "FAILED" : player.getStatus().name());
        if (ok) {
            Duration total = player.getMedia().getDuration();
            Duration current = player.getCurrentTime();
            state.position.setMax(total.toSeconds());
            state.position.setValue(current.toSeconds());
            state.time.setText(MediaSupport.time(current) + " / " + MediaSupport.time(total));
        }
    }

    private static void showChecks(State state) {
        MediaPlayer player = state.player;
        List<Check> video = new ArrayList<>();
        video.add(state.error == null ? Check.pass("ready, paused and seeked", "OK") : Check.fail("ready, paused and seeked", state.error));
        if (player != null) {
            Media media = state.media;
            video.add(Checks.expect("status, onReady handler calls", "PAUSED, 1",
                    () -> player.getStatus().name() + ", " + state.readyEvents));
            video.add(Checks.expect("media width x height", "320x180", () -> media.getWidth() + "x" + media.getHeight()));
            video.add(Checks.expect("duration", "3.0 s", () -> MediaSupport.seconds(media.getDuration())));
            video.add(Checks.run("currentTime after seek(" + MediaSupport.seconds(SEEK) + ")",
                    () -> MediaSupport.seconds(player.getCurrentTime())));
            video.add(Checks.expect("media markers", "{end=2.5 s, start=0.5 s}", () -> {
                java.util.Map<String, String> markers = new java.util.TreeMap<>();
                media.getMarkers().forEach((name, time) -> markers.put(name, MediaSupport.seconds(time)));
                return markers.toString();
            }));
            video.add(Checks.run("tracks", () -> MediaSupport.tracks(media)));
            video.add(Check.info("metadata", MediaSupport.metadata(media.getMetadata())));
            video.add(Checks.run("snapshot colors at 0 s, 1 s, 2 s, final", () -> {
                List<String> colors = new ArrayList<>();
                for (ImageView view : state.strip) {
                    colors.add(dominant(view.getImage()));
                }
                colors.add(dominant(state.main.snapshot(new SnapshotParameters(), null)));
                return String.join(", ", colors);
            }));
        }

        List<Check> views = new ArrayList<>();
        views.add(Checks.expect("MediaViews sharing the player", 1 + state.variants.size(), () -> {
            int count = state.main.getMediaPlayer() == player ? 1 : 0;
            for (MediaView view : state.variants) {
                count += view.getMediaPlayer() == player ? 1 : 0;
            }
            return count;
        }));
        views.add(Checks.run("main view layout bounds", () -> size(state.main.getLayoutBounds())));
        for (MediaView view : state.variants) {
            views.add(Checks.run(String.valueOf(view.getProperties().get("caption")),
                    () -> size(view.getLayoutBounds())));
        }

        state.videoChecks.getChildren().setAll(Checks.view("Media & MediaPlayer", video));
        state.viewChecks.getChildren().setAll(Checks.view("MediaView variants", views));
    }

    /**
     * Dominant color of the pixel (8, 8) of a video frame (the background of the clip is red, green, then blue).
     */
    private static String dominant(javafx.scene.image.Image image) {
        if (image == null) {
            return "none";
        }
        Color color = image.getPixelReader().getColor(8, 8);
        return color.getGreen() > color.getRed() && color.getGreen() > color.getBlue() ? "green"
                : color.getRed() > color.getBlue() ? "red" : "blue";
    }

    private static String size(Bounds bounds) {
        return number(bounds.getWidth()) + "x" + number(bounds.getHeight());
    }

    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public CompletionStage<?> ready(Node content) {
        return ((State) content.getProperties().get(STATE)).ready;
    }

    @Override
    public void dispose(Node content) {
        State state = (State) content.getProperties().get(STATE);
        if (state != null && state.player != null) {
            state.player.dispose();
        }
    }
}
