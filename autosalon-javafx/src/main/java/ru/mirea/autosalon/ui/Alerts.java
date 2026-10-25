package ru.mirea.autosalon.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.exception.BusinessException;
import ru.mirea.autosalon.exception.DataAccessException;
import ru.mirea.autosalon.exception.DatabaseConnectionException;
import ru.mirea.autosalon.exception.EntityNotFoundException;
import ru.mirea.autosalon.exception.ExportException;
import ru.mirea.autosalon.exception.ValidationException;

/**
 * Диалоговые окна JavaFX: ошибка, подтверждение, информация.
 * <p>
 * Здесь же исключения превращаются в понятный пользователю текст: вид ошибки определяется по классу
 * исключения, а подробности берутся из его сообщения. Stack trace пользователю не показывается, он пишется в лог.
 */
public final class Alerts {

    private static final Logger LOG = LoggerFactory.getLogger(Alerts.class);

    private Alerts() {
    }

    /**
     * @param owner  окно, поверх которого показать сообщение (может быть null)
     * @param action что пытался сделать пользователь, например «Удаление клиента»
     */
    public static void showError(Window owner, String action, Throwable error) {
        Alert alert = create(Alert.AlertType.ERROR, owner, action, kindOf(error), messageOf(error));
        alert.showAndWait();
    }

    public static void showInfo(Window owner, String title, String header, String text) {
        create(Alert.AlertType.INFORMATION, owner, title, header, text).showAndWait();
    }

    /** Запрашивает подтверждение. Возвращает true, только если пользователь нажал кнопку действия. */
    public static boolean confirm(Window owner, String title, String header, String text, String actionText) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, owner, title, header, text);
        ButtonType action = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(action, cancel);
        alert.getDialogPane().lookupButton(action).getStyleClass().add("button-danger");
        return alert.showAndWait().filter(button -> button == action).isPresent();
    }

    /** Вид ошибки для заголовка сообщения. */
    public static String kindOf(Throwable error) {
        if (error instanceof ValidationException) {
            return "Некорректные данные";
        }
        if (error instanceof BusinessException) {
            return "Нарушение бизнес-правила";
        }
        if (error instanceof EntityNotFoundException) {
            return "Запись не найдена";
        }
        if (error instanceof DatabaseConnectionException) {
            return "Нет подключения к базе данных";
        }
        if (error instanceof DataAccessException) {
            return "Ошибка при работе с базой данных";
        }
        if (error instanceof ExportException) {
            return "Ошибка экспорта";
        }
        return "Непредвиденная ошибка";
    }

    /** Текст ошибки для пользователя. Непредвиденные ошибки дополнительно записываются в лог со stack trace. */
    public static String messageOf(Throwable error) {
        if (!(error instanceof AutosalonException)) {
            LOG.error("Непредвиденная ошибка", error);
            String details = error.getMessage() == null ? "" : ": " + error.getMessage();
            return error.getClass().getSimpleName() + details
                    + ". Подробности записаны в журнал приложения.";
        }
        return capitalize(error.getMessage());
    }

    private static Alert create(Alert.AlertType type, Window owner, String title, String header, String text) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        // Label с переносом строк вместо contentText: длинные сообщения не обрезаются
        Label content = new Label(text);
        content.setWrapText(true);
        content.setMaxWidth(520);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        return alert;
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
