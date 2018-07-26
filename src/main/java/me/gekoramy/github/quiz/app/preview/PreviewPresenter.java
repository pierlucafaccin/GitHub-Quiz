package me.gekoramy.github.quiz.app.preview;

import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import me.gekoramy.github.quiz.app.AppDownload;
import me.gekoramy.github.quiz.exception.NotLoggedException;
import me.gekoramy.github.quiz.service.ExamStarter;
import me.gekoramy.github.quiz.service.OpenLink;
import me.gekoramy.github.quiz.service.Store;
import me.gekoramy.github.quiz.service.UpdateAvailable;
import me.gekoramy.github.quiz.util.Constants;
import me.gekoramy.github.quiz.util.GitHubClients;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * @author Luca Mosetti
 */
public class PreviewPresenter implements Initializable {
    public Label lblQuestions;
    public Slider sldQuestions;

    @FXML
    private StackPane root;
    @FXML
    private ProgressBar progress;
    @FXML
    private Hyperlink lblRepo;
    @FXML
    private Hyperlink btnStore;
    @FXML
    private Hyperlink btnRefresh;
    @FXML
    private Hyperlink btnDownload;
    @FXML
    private Hyperlink btnStart;

    @Inject
    private UpdateAvailable updateAvailable;
    @Inject
    private ExamStarter examStarter;
    @Inject
    private OpenLink openLink;

    private Store store;
    private final RotateTransition rotate;
    private final FileChooser fileChooser;

    public PreviewPresenter() {
        this.fileChooser = new FileChooser();
        this.fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Questions file", Constants.MIME));
        this.fileChooser.setTitle("Store to quiz yourself offline");

        rotate = new RotateTransition(Duration.millis(750));
        rotate.setByAngle(360);
        rotate.setCycleCount(1);
        rotate.setOnFinished(e -> {
            if (updateAvailable.isRunning())
                rotate.playFromStart();
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fileChooser.setInitialFileName(updateAvailable.getRepo().getName().replaceFirst(Constants.PREFIX, ""));

        rotate.setNode(btnRefresh);

        store = new Store(updateAvailable.getRepo());
        btnStore.disableProperty().bind(store.runningProperty());

        lblRepo.setText(updateAvailable.getRepo().getName());

        progress.progressProperty().bind(examStarter.getQuestionPool().progressProperty());

        updateAvailable.valueProperty().addListener((value, oldValue, newValue) -> {
            if (newValue != null && newValue) {
                btnDownload.visibleProperty().set(true);
                btnRefresh.visibleProperty().set(false);
            }
        });
        btnRefresh.disableProperty().bind(updateAvailable.runningProperty());

        lblQuestions.textProperty().bind(Bindings.createIntegerBinding(() -> new Double(sldQuestions.valueProperty().get()).intValue(), sldQuestions.valueProperty()).asString());
        sldQuestions.setMax(examStarter.getQuestionPool().total());
        sldQuestions.setValue(12);

        lblRepo.setOnAction(e -> onRepo());
        btnStore.setOnAction(e -> onStore());
        btnDownload.setOnAction(e -> onDownload());
        btnRefresh.setOnAction(e -> onRefresh());
        btnStart.setOnAction(e -> onStart());
    }

    public void setAccelerators(Scene scene) {
        ObservableMap<KeyCombination, Runnable> accelerators = scene.getAccelerators();
        accelerators.put(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_ANY), this::increase);
        accelerators.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHORTCUT_ANY), this::increase);
        accelerators.put(new KeyCodeCombination(KeyCode.UP, KeyCombination.SHORTCUT_ANY), this::increase);

        accelerators.put(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_ANY), this::decrease);
        accelerators.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.SHORTCUT_ANY), this::decrease);
        accelerators.put(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.SHORTCUT_ANY), this::decrease);

        accelerators.put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::onStore);
        accelerators.put(new KeyCodeCombination(KeyCode.F5, KeyCombination.SHORTCUT_ANY), this::onRefresh);
    }

    private void increase() {
        sldQuestions.valueProperty().set(sldQuestions.valueProperty().add(1).get());
    }

    private void decrease() {
        sldQuestions.valueProperty().set(sldQuestions.valueProperty().subtract(1).get());
    }

    private void onStore() {
        if (!store.isRunning()) {
            store.setOutput(fileChooser.showSaveDialog(null));
            store.setQuestionPool(examStarter.getQuestionPool());
            store.reset();
            store.start();
        }
    }

    private void onRepo() {
        if (!openLink.isRunning()) {
            openLink.reset();
            openLink.start();
        }
    }

    private void onStart() {
        if (!examStarter.isRunning()) {
            examStarter.setMany(new Double(sldQuestions.getValue()).intValue());
            examStarter.reset();
            examStarter.start();
        }
    }

    private void onDownload() {
        Platform.runLater(() -> {
            try {
                AppDownload app = new AppDownload(GitHubClients.get(), updateAvailable.getRepo());
                app.start((Stage) root.getScene().getWindow());
            } catch (NotLoggedException e) {
                e.printStackTrace();
            }
        });
    }

    private void onRefresh() {
        if (!updateAvailable.isRunning()) {
            try {
                updateAvailable.setClient(GitHubClients.get());
                updateAvailable.reset();
                updateAvailable.start();
                rotate.playFromStart();
            } catch (NotLoggedException e) {
                e.printStackTrace();
            }
        }
    }
}
