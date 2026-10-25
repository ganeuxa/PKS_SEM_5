package ru.mirea.autosalon.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import ru.mirea.autosalon.AppContext;
import ru.mirea.autosalon.controller.FormController;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Диалоговое окно с формой добавления или редактирования объекта.
 * <p>
 * Содержимое формы загружается из FXML. При нажатии «Сохранить» вызывается {@link FormController#save()}:
 * если данные некорректны, событие нажатия поглощается ({@code event.consume()}) и окно остаётся открытым,
 * чтобы пользователь мог исправить ошибку.
 */
public final class FormDialog {

    private FormDialog() {
    }

    /**
     * @param setup настройка контроллера перед показом (например, передать редактируемый объект)
     * @return сохранённый объект или пустой Optional, если пользователь нажал «Отмена»
     */
    public static <R, C extends FormController<R>> Optional<R> show(Window owner, String fxmlFile, AppContext context,
                                                                    Consumer<C> setup) {
        FXMLLoader loader = ViewLoader.loader(fxmlFile, context);
        Parent content = ViewLoader.load(loader);
        C controller = loader.getController();
        setup.accept(controller);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(controller.getTitle());
        dialog.setHeaderText(controller.getTitle());
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        pane.setContent(content);
        pane.getStylesheets().add(ViewLoader.stylesheet());
        pane.getStyleClass().add("form-dialog");

        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(saveType, cancelType);

        Button saveButton = (Button) pane.lookupButton(saveType);
        saveButton.getStyleClass().add("button-primary");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!controller.save()) {
                event.consume();
                // Под формой появилось сообщение об ошибке: после перерасчёта разметки увеличиваем окно,
                // чтобы текст не обрезался
                Platform.runLater(() -> pane.getScene().getWindow().sizeToScene());
            }
        });

        return dialog.showAndWait()
                .filter(button -> button == saveType)
                .map(button -> controller.getResult());
    }
}
