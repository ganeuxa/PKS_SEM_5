package ru.mirea.autosalon.exception;

/**
 * Не удалось подключиться к базе данных или соединение было потеряно:
 * сервер PostgreSQL не запущен, неверный логин/пароль, база данных не создана и т.п.
 */
public class DatabaseConnectionException extends DataAccessException {

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
