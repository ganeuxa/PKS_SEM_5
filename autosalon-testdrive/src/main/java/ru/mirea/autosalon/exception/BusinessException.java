package ru.mirea.autosalon.exception;

/**
 * Нарушение бизнес-правила предметной области, например:
 * запись на тест-драйв проданного автомобиля или запрещённый переход статуса заявки.
 */
public class BusinessException extends AutosalonException {

    public BusinessException(String message) {
        super(message);
    }
}
