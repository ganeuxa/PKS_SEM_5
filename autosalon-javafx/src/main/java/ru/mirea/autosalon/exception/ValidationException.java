package ru.mirea.autosalon.exception;

/**
 * Некорректные данные объекта: не заполнено обязательное поле или значение имеет неверный формат.
 * Частный случай нарушения бизнес-правил («нельзя сохранить объект с некорректными данными»).
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(message);
    }
}
