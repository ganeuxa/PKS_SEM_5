package ru.mirea.autosalon.exception;

/**
 * Запись с указанным ID не найдена в базе данных.
 */
public class EntityNotFoundException extends AutosalonException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
