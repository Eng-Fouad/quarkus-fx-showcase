package io.quarkiverse.fx.showcase.pages.media;

import java.nio.file.Path;
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
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.AudioEqualizer;
import javafx.scene.media.EqualizerBand;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * Audio playback : Media / MediaPlayer for WAV, AIFF and M4A (never played, volume 0), AudioClip, AudioEqualizer.
 */
@Singleton
public class AudioPage implements FeaturePage {

    private static final String STATE = AudioPage.class.getName();
    private static final String[][] FILES = {
            { "/showcase/media/hello.wav", "WAVE, PCM 16 bit, 22.05 kHz" },
            { "/showcase/media-pages/hello-pcm.aiff", "AIFF, PCM 16 bit big endian, 22.05 kHz" },
            { "/showcase/media/hello.m4a", "MPEG-4, AAC" } };
    /** An AIFF-C file : not supported by JavaFX, the error must be reported through the error properties. */
    private static final String AIFC = "/showcase/media/hello.aiff";
    private static final double[] GAINS = { 6, 4, 2, 0, -2, -4, -2, 0, 3, 6 };

    @Override
    public String id() {
        return "media-audio";
    }

    @Override
    public String title() {
        return "Audio";
    }

    @Override
    public String category() {
        return Categories.MEDIA;
    }

    @Override
    public int order() {
        return 10;
    }

    private static final class Player {
        final String path;
        final String file;
        final String description;
        Media media;
        MediaPlayer player;
        String error;
        MediaPlayer.Status status;
        final Label statusLabel = new Label("WAITING");
        final Label time = new Label("-:--.- / -:--.-");
        final Slider position = new Slider(0, 1, 0);
        CompletionStage<Void> done;

        Player(String path, String description) {
            this.path = path;
            this.file = path.substring(path.lastIndexOf('/') + 1);
            this.description = description;
        }
    }

    private static final class State {
        final List<Player> players = new ArrayList<>();
        Player aifc;
        AudioClip clip;
        String clipError;
        final VBox mediaChecks = new VBox();
        final VBox otherChecks = new VBox();
        CompletionStage<?> ready;
    }

    @Override
    public Node build() {
        State state = new State();
        HBox cards = new HBox(16);
        for (String[] file : FILES) {
            Player player = new Player(file[0], file[1]);
            state.players.add(player);
            open(player);
            updateCard(player);
            player.done = player.done.thenRun(() -> updateCard(player));
            cards.getChildren().add(card(player));
        }
        state.aifc = new Player(AIFC, "AIFF-C");
        open(state.aifc);

        HBox tools = new HBox(16, equalizerCard(state.players.getFirst()), clipCard(state));

        state.mediaChecks.getChildren().add(new Label("Waiting for the players to be READY..."));
        state.mediaChecks.setPrefWidth(506);
        state.otherChecks.setPrefWidth(506);
        HBox.setHgrow(state.mediaChecks, Priority.ALWAYS);
        HBox.setHgrow(state.otherChecks, Priority.ALWAYS);
        HBox checks = new HBox(16, state.mediaChecks, state.otherChecks);

        VBox root = new VBox(12,
                MediaSupport.caption("MediaPlayer (volume 0, muted, never played) for three audio formats, "
                        + "copied from the classpath to temporary files"),
                cards, tools, checks);
        root.getStylesheets().add(Fx.resourceUrl(MediaSupport.STYLESHEET));
        root.getProperties().put(STATE, state);

        List<Player> players = new ArrayList<>(state.players);
        players.add(state.aifc);
        CompletableFuture<?>[] all = players.stream().map(p -> p.done.toCompletableFuture())
                .toArray(CompletableFuture[]::new);
        state.ready = CompletableFuture.allOf(all)
                .thenComposeAsync(v -> Fx.pulses(3), Fx.FX_THREAD)
                .thenApply(v -> {
                    showChecks(state);
                    return null;
                })
                .thenCompose(v -> Fx.pulses(5));
        return root;
    }

    private static void open(Player player) {
        try {
            String uri = Fx.resourceToTempFile(player.path).toUri().toString();
            player.media = new Media(uri);
            player.player = new MediaPlayer(player.media);
            player.player.setVolume(0);
            player.player.setMute(true);
            player.done = MediaSupport.ready(player.player, 15_000).handle((status, error) -> {
                player.status = status;
                player.error = error != null ? MediaSupport.describe(error) : MediaSupport.playerError(player.player);
                return null;
            });
        } catch (Throwable t) {
            player.error = Checks.describe(t);
            player.done = CompletableFuture.completedFuture(null);
        }
    }

    private static VBox card(Player player) {
        Label title = new Label(player.file);
        title.getStyleClass().add("media-card-title");
        Label subtitle = new Label(player.description);
        subtitle.getStyleClass().add("media-card-subtitle");
        player.statusLabel.getStyleClass().addAll("media-status", "waiting");
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox header = new HBox(8, new VBox(0, title, subtitle), grow, player.statusLabel);
        header.setAlignment(Pos.TOP_LEFT);

        Button backward = MediaSupport.button(MediaSupport.BACKWARD, "<<");
        Button play = MediaSupport.button(MediaSupport.PLAY, ">");
        play.getStyleClass().add("primary");
        Button pause = MediaSupport.button(MediaSupport.PAUSE, "||");
        Button stop = MediaSupport.button(MediaSupport.STOP, "[]");
        Button forward = MediaSupport.button(MediaSupport.FORWARD, ">>");
        HBox buttons = new HBox(6, backward, play, pause, stop, forward);
        buttons.setAlignment(Pos.CENTER_LEFT);

        player.position.setFocusTraversable(false);
        player.time.getStyleClass().add("media-time");

        ToggleButton mute = MediaSupport.toggle(MediaSupport.MUTE, "M");
        Slider volume = new Slider(0, 1, 0);
        volume.setPrefWidth(90);
        volume.setFocusTraversable(false);
        Region grow2 = new Region();
        HBox.setHgrow(grow2, Priority.ALWAYS);
        HBox footer = new HBox(8, player.time, grow2, mute, volume);
        footer.setAlignment(Pos.CENTER_LEFT);

        MediaPlayer mp = player.player;
        if (mp != null) {
            play.setOnAction(e -> mp.play());
            pause.setOnAction(e -> mp.pause());
            stop.setOnAction(e -> mp.stop());
            backward.setOnAction(e -> mp.seek(Duration.ZERO));
            forward.setOnAction(e -> mp.seek(mp.getCurrentTime().add(Duration.seconds(1))));
            mute.selectedProperty().bindBidirectional(mp.muteProperty());
            volume.valueProperty().bindBidirectional(mp.volumeProperty());
            mp.currentTimeProperty().addListener((o, oldTime, newTime) -> {
                if (!player.position.isValueChanging()) {
                    player.position.setValue(newTime.toSeconds());
                }
                player.time.setText(MediaSupport.time(newTime) + " / " + MediaSupport.time(mp.getTotalDuration()));
            });
            player.position.valueChangingProperty().addListener((o, was, changing) -> {
                if (!changing) {
                    mp.seek(Duration.seconds(player.position.getValue()));
                }
            });
        }

        VBox card = new VBox(header, buttons, player.position, footer);
        card.getStyleClass().add("media-card");
        card.setPrefWidth(332);
        card.setMinWidth(332);
        return card;
    }

    private static void updateCard(Player player) {
        if (player.status == null && player.error == null) {
            return;
        }
        boolean ok = player.error == null && player.status == MediaPlayer.Status.READY;
        player.statusLabel.getStyleClass().removeAll("waiting", "failed");
        if (!ok) {
            player.statusLabel.getStyleClass().add("failed");
        }
        player.statusLabel.setText(ok ? player.status.name() : "FAILED");
        if (player.player != null && ok) {
            Duration total = player.player.getTotalDuration();
            player.position.setMax(total.toSeconds());
            player.position.setValue(0);
            player.time.setText(MediaSupport.time(Duration.ZERO) + " / " + MediaSupport.time(total));
        }
    }

    private static VBox equalizerCard(Player player) {
        Label title = new Label("AudioEqualizer of " + player.file);
        title.getStyleClass().add("media-card-title");
        GridPane bands = new GridPane();
        bands.setHgap(22);
        bands.setVgap(2);
        if (player.player != null) {
            AudioEqualizer equalizer = player.player.getAudioEqualizer();
            List<EqualizerBand> list = equalizer.getBands();
            for (int i = 0; i < list.size(); i++) {
                EqualizerBand band = list.get(i);
                if (i < GAINS.length) {
                    band.setGain(GAINS[i]);
                }
                Slider slider = new Slider(EqualizerBand.MIN_GAIN, EqualizerBand.MAX_GAIN, band.getGain());
                slider.setOrientation(Orientation.VERTICAL);
                slider.setPrefHeight(110);
                slider.setFocusTraversable(false);
                slider.valueProperty().bindBidirectional(band.gainProperty());
                Label gain = new Label();
                gain.textProperty().bind(band.gainProperty().asString(Locale.ROOT, "%+.0f"));
                gain.setStyle("-fx-font-size: 10px;");
                Label frequency = new Label(frequency(band.getCenterFrequency()));
                frequency.setStyle("-fx-font-size: 10px; -fx-text-fill: #52606d;");
                bands.add(gain, i, 0);
                bands.add(slider, i, 1);
                bands.add(frequency, i, 2);
                GridPane.setHalignment(gain, javafx.geometry.HPos.CENTER);
                GridPane.setHalignment(slider, javafx.geometry.HPos.CENTER);
                GridPane.setHalignment(frequency, javafx.geometry.HPos.CENTER);
            }
        }
        VBox card = new VBox(title, bands);
        card.getStyleClass().add("media-card");
        card.setPrefWidth(506);
        card.setMinWidth(506);
        return card;
    }

    private static String frequency(double hz) {
        return hz >= 1000 ? String.format(Locale.ROOT, "%.0fk", hz / 1000) : String.format(Locale.ROOT, "%.0f", hz);
    }

    private static VBox clipCard(State state) {
        Label title = new Label("AudioClip (not played)");
        title.getStyleClass().add("media-card-title");
        VBox card = new VBox(title);
        card.getStyleClass().add("media-card");
        card.setPrefWidth(506);
        card.setMinWidth(506);
        try {
            state.clip = new AudioClip(Fx.resourceToTempFile("/showcase/media/hello.wav").toUri().toString());
            state.clip.setVolume(0);
            AudioClip clip = state.clip;
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(6);
            String source = clip.getSource();
            grid.addRow(0, new Label("source"),
                    new Label(source.substring(source.lastIndexOf('/') + 1) + " (temporary file)"));
            grid.addRow(1, new Label("volume"), slider(0, 1, clip.volumeProperty()));
            grid.addRow(2, new Label("balance"), slider(-1, 1, clip.balanceProperty()));
            grid.addRow(3, new Label("rate"), slider(0.125, 8, clip.rateProperty()));
            Button play = MediaSupport.button(MediaSupport.PLAY, ">");
            play.getStyleClass().add("primary");
            play.setOnAction(e -> clip.play());
            Button stop = MediaSupport.button(MediaSupport.STOP, "[]");
            stop.setOnAction(e -> clip.stop());
            grid.addRow(4, new Label("controls"), new HBox(6, play, stop));
            card.getChildren().add(grid);
        } catch (Throwable t) {
            state.clipError = Checks.describe(t);
            Label error = new Label(state.clipError);
            error.getStyleClass().add("check-fail");
            error.setWrapText(true);
            card.getChildren().add(error);
        }
        return card;
    }

    private static Slider slider(double min, double max, javafx.beans.property.DoubleProperty property) {
        Slider slider = new Slider(min, max, property.get());
        slider.setPrefWidth(300);
        slider.setFocusTraversable(false);
        slider.valueProperty().bindBidirectional(property);
        return slider;
    }

    private static void showChecks(State state) {
        List<Check> media = new ArrayList<>();
        for (Player player : state.players) {
            if (player.player == null || player.status != MediaPlayer.Status.READY) {
                media.add(Check.fail(player.file + ": status", player.error != null ? player.error : player.status));
                continue;
            }
            media.add(Checks.run(player.file + ": status, duration",
                    () -> player.player.getStatus() + ", " + MediaSupport.seconds(player.media.getDuration())));
            media.add(Checks.run(player.file + ": tracks", () -> MediaSupport.tracks(player.media)));
        }
        List<String> metadata = new ArrayList<>();
        for (Player player : state.players) {
            metadata.add(player.media == null ? "-" : MediaSupport.metadata(player.media.getMetadata()));
        }
        media.add(Check.info("metadata (wav, aiff, m4a)", String.join(", ", metadata)));

        List<Check> other = new ArrayList<>();
        if (state.clip == null) {
            other.add(Check.fail("new AudioClip(hello.wav)", state.clipError));
        } else {
            AudioClip clip = state.clip;
            other.add(Checks.expect("AudioClip source file", "hello.wav",
                    () -> clip.getSource().substring(clip.getSource().lastIndexOf('/') + 1)));
            other.add(Checks.run("AudioClip properties", () -> String.format(Locale.ROOT,
                    "volume %.1f, balance %.1f, rate %.1f, pan %.1f, priority %d, cycles %d",
                    clip.getVolume(), clip.getBalance(), clip.getRate(), clip.getPan(), clip.getPriority(),
                    clip.getCycleCount())));
            other.add(Checks.expect("AudioClip.isPlaying()", false, clip::isPlaying));
        }
        MediaPlayer first = state.players.getFirst().player;
        if (first != null) {
            other.add(Checks.expect("AudioEqualizer bands (Hz)", "32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000",
                    () -> String.join(", ", first.getAudioEqualizer().getBands().stream()
                            .map(b -> String.format(Locale.ROOT, "%.0f", b.getCenterFrequency())).toList())));
            other.add(Checks.expect("AudioEqualizer gains (dB)", "+6 +4 +2 +0 -2 -4 -2 +0 +3 +6",
                    () -> String.join(" ", first.getAudioEqualizer().getBands().stream()
                            .map(b -> String.format(Locale.ROOT, "%+.0f", b.getGain())).toList())));
            other.add(Checks.run("audio spectrum defaults", () -> String.format(Locale.ROOT,
                    "%d bands, interval %.2f s, threshold %d dB", first.getAudioSpectrumNumBands(),
                    first.getAudioSpectrumInterval(), first.getAudioSpectrumThreshold())));
        }
        other.add(Checks.expect(state.aifc.file + " (AIFF-C, unsupported)", "MEDIA_CORRUPTED", () -> {
            MediaPlayer player = state.aifc.player;
            if (player == null) {
                return state.aifc.error;
            }
            if (player.getError() != null) {
                return player.getError().getType().name();
            }
            return player.getStatus() + (state.aifc.error != null ? ": " + state.aifc.error : "");
        }));
        other.add(Checks.expect("Media of a missing file", "MEDIA_UNAVAILABLE", () -> {
            Path dir = Fx.resourceToTempFile("/showcase/media-pages/media.css").getParent();
            return mediaError(dir.resolve("missing-file.wav").toUri().toString());
        }));
        other.add(Checks.expect("Media of a .css file", "MEDIA_UNSUPPORTED",
                () -> mediaError(Fx.resourceToTempFile("/showcase/media-pages/media.css").toUri().toString())));

        state.mediaChecks.getChildren().setAll(Checks.view("Media & MediaPlayer", media));
        state.otherChecks.getChildren().setAll(Checks.view("AudioClip, equalizer & errors", other));
    }

    private static String mediaError(String uri) {
        try {
            new Media(uri);
            return "no exception";
        } catch (MediaException e) {
            return e.getType().name();
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
            List<Player> players = new ArrayList<>(state.players);
            players.add(state.aifc);
            for (Player player : players) {
                if (player.player != null) {
                    player.player.dispose();
                }
            }
            if (state.clip != null) {
                state.clip.stop();
            }
        }
    }
}
