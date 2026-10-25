package ru.mirea.autosalon.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import ru.mirea.autosalon.exception.ValidationException;
import ru.mirea.autosalon.ui.Alerts;
import ru.mirea.autosalon.util.Formats;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Общая часть форм: вывод ошибки под полями, подсветка неверного поля и разбор введённых значений.
 * <p>
 * Проверка идёт в два этапа:
 * <ol>
 *     <li>здесь — то, что можно проверить по самому полю: заполнено ли оно, число ли это, существует ли дата,
 *     не слишком ли длинная строка;</li>
 *     <li>в сервисе — правила предметной области: формат телефона и VIN, возраст, уникальность, расписание и т.д.</li>
 * </ol>
 * Любая ошибка показывается в errorLabel формы, окно при этом не закрывается.
 */
public abstract class AbstractFormController<R> implements FormController<R> {

    private static final String FIELD_ERROR_CLASS = "field-error";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);

    @FXML
    protected Label errorLabel;

    private final List<Control> invalidFields = new ArrayList<>();
    private R result;

    /** Проверяет поля, вызывает сервис и возвращает сохранённый объект. */
    protected abstract R doSave();

    @FXML
    protected void initialize() {
        // Метка ошибки занимает место в форме, только когда в ней есть текст
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
    }

    @Override
    public final boolean save() {
        clearErrors();
        try {
            result = doSave();
            return true;
        } catch (RuntimeException e) {
            errorLabel.setText(Alerts.kindOf(e) + ". " + Alerts.messageOf(e));
            return false;
        }
    }

    @Override
    public R getResult() {
        return result;
    }

    private void clearErrors() {
        invalidFields.forEach(field -> field.getStyleClass().remove(FIELD_ERROR_CLASS));
        invalidFields.clear();
        errorLabel.setText("");
    }

    /** Подсвечивает поле и прерывает сохранение с понятным сообщением. */
    protected ValidationException invalid(Control field, String message) {
        if (!field.getStyleClass().contains(FIELD_ERROR_CLASS)) {
            field.getStyleClass().add(FIELD_ERROR_CLASS);
        }
        invalidFields.add(field);
        field.requestFocus();
        return new ValidationException(message);
    }

    /** Обязательное текстовое поле: без пробелов по краям, не длиннее maxLength. */
    protected String requireText(TextInputControl field, String fieldName, int maxLength) {
        String text = optionalText(field, fieldName, maxLength);
        if (text == null) {
            throw invalid(field, "Поле «" + fieldName + "» обязательно для заполнения.");
        }
        return text;
    }

    /** Необязательное текстовое поле: пустое значение превращается в null. */
    protected String optionalText(TextInputControl field, String fieldName, int maxLength) {
        String text = field.getText() == null ? "" : field.getText().trim();
        if (text.length() > maxLength) {
            throw invalid(field, "Поле «" + fieldName + "»: не более " + maxLength + " символов (сейчас "
                    + text.length() + ").");
        }
        return text.isEmpty() ? null : text;
    }

    /** Целое число в диапазоне [min; max]. */
    protected int requireInt(TextField field, String fieldName, int min, int max) {
        String text = requireText(field, fieldName, 10);
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw invalid(field, "Поле «" + fieldName + "» должно быть целым числом (введено: \"" + text + "\").");
        }
        if (value < min || value > max) {
            throw invalid(field, "Поле «" + fieldName + "»: допустимы значения от " + min + " до " + max
                    + " (введено: " + value + ").");
        }
        return value;
    }

    /** Дата из DatePicker. Разбирается текст поля, чтобы поймать и несуществующие даты вроде 31.02.2026. */
    protected LocalDate requireDate(DatePicker picker, String fieldName) {
        String text = picker.getEditor().getText() == null ? "" : picker.getEditor().getText().trim();
        if (text.isEmpty()) {
            throw invalid(picker, "Поле «" + fieldName + "» обязательно для заполнения.");
        }
        try {
            return LocalDate.parse(text, Formats.DATE);
        } catch (DateTimeParseException e) {
            throw invalid(picker, "Поле «" + fieldName + "»: введите существующую дату в формате ДД.ММ.ГГГГ"
                    + " (введено: \"" + text + "\").");
        }
    }

    /** Время ЧЧ:ММ из редактируемого ComboBox. */
    protected LocalTime requireTime(ComboBox<String> box, String fieldName) {
        String text = box.getEditor().getText() == null ? "" : box.getEditor().getText().trim();
        if (text.isEmpty()) {
            throw invalid(box, "Поле «" + fieldName + "» обязательно для заполнения.");
        }
        try {
            return LocalTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw invalid(box, "Поле «" + fieldName + "»: введите время в формате ЧЧ:ММ, например 14:30"
                    + " (введено: \"" + text + "\").");
        }
    }

    /** Значение, выбранное в выпадающем списке (связанная сущность). */
    protected <T> T requireChoice(ComboBox<T> box, String fieldName) {
        T value = box.getValue();
        if (value == null) {
            throw invalid(box, "Выберите значение в поле «" + fieldName + "».");
        }
        return value;
    }
}
