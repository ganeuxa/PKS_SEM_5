package ru.mirea.autosalon.ui;

import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;
import ru.mirea.autosalon.model.BaseEntity;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.util.Formats;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;

/**
 * Общие настройки элементов JavaFX, чтобы не повторять один и тот же код в разных контроллерах.
 */
public final class FxSupport {

    private FxSupport() {
    }

    /**
     * Ячейка таблицы, которая показывает значение в нужном формате (дата, деньги и т.д.).
     * В столбце хранится само значение (LocalDateTime, Integer), поэтому сортировка идёт по значению, а не по тексту.
     */
    public static <S, V> Callback<TableColumn<S, V>, TableCell<S, V>> formattedCell(Function<V, String> format) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(V value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : format.apply(value));
            }
        };
    }

    /** Ячейка со статусом заявки: цветная метка, цвет задаётся в CSS классом status-new, status-confirmed и т.д. */
    public static <S> Callback<TableColumn<S, TestDriveStatus>, TableCell<S, TestDriveStatus>> statusCell() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(TestDriveStatus status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeIf(styleClass -> styleClass.startsWith("status-"));
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status.getDisplayName());
                    getStyleClass().add("status-" + status.name().toLowerCase(Locale.ROOT).replace('_', '-'));
                }
            }
        };
    }

    /** Как показывать сущность в ComboBox: через её getDisplayName(). */
    public static <T extends BaseEntity> StringConverter<T> entityConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(T entity) {
                return entity == null ? "" : entity.getDisplayName();
            }

            @Override
            public T fromString(String text) {
                throw new UnsupportedOperationException("Объект выбирается из списка, а не вводится текстом");
            }
        };
    }

    /**
     * DatePicker в формате ДД.ММ.ГГГГ. Неверный текст не приводит к исключению в интерфейсе:
     * значение просто не принимается, а ошибку показывает проверка формы.
     */
    public static void setupDatePicker(DatePicker picker) {
        picker.setPromptText("ДД.ММ.ГГГГ");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return Formats.date(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(text.trim(), Formats.DATE);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
    }

    /** Диалог сохранения CSV: путь выбирает пользователь, по умолчанию — домашняя папка. */
    public static FileChooser csvFileChooser(String title, String defaultFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(defaultFileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (разделитель «;»)", "*.csv"));
        File home = new File(System.getProperty("user.home"));
        if (home.isDirectory()) {
            chooser.setInitialDirectory(home);
        }
        return chooser;
    }
}
