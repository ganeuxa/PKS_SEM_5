package ru.mirea.autosalon;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.autosalon.controller.MainController;
import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.ui.Alerts;
import ru.mirea.autosalon.ui.ViewLoader;

/**
 * JavaFX-приложение «Автосалон: заявки на тест-драйв».
 * <p>
 * {@link Stage} — окно, {@link Scene} — содержимое окна, корень сцены и все элементы внутри него — узлы (Node).
 * Интерфейс главного окна описан в main-view.fxml, а его поведение — в {@link MainController}.
 */
public class AutosalonApp extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(AutosalonApp.class);

    private static final double WINDOW_WIDTH = 1280;
    private static final double WINDOW_HEIGHT = 800;

    @Override
    public void start(Stage stage) {
        // Любое непойманное исключение показываем пользователю понятным сообщением, а не stack trace
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> showUnexpectedError(error));

        AppContext context;
        try {
            context = AppContext.create();
        } catch (AutosalonException e) {
            Alerts.showError(null, "Не удалось запустить приложение", e);
            Platform.exit();
            return;
        }

        FXMLLoader loader = ViewLoader.loader("main-view.fxml", context);
        Parent root = ViewLoader.load(loader);
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(ViewLoader.stylesheet());

        stage.setTitle("Автосалон — заявки на тест-драйв");
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();

        // Данные загружаем, когда окно уже на экране: так сообщение об ошибке подключения появится поверх окна
        MainController mainController = loader.getController();
        mainController.connectAndLoad();
    }

    private static void showUnexpectedError(Throwable error) {
        LOG.error("Непредвиденная ошибка", error);
        if (Platform.isFxApplicationThread()) {
            Alerts.showError(null, "Непредвиденная ошибка", error);
        } else {
            Platform.runLater(() -> Alerts.showError(null, "Непредвиденная ошибка", error));
        }
    }
}
