package ru.mirea.autosalon.exception;

/**
 * Ошибка выполнения SQL-запроса. Репозитории перехватывают {@link java.sql.SQLException}
 * и выбрасывают это исключение, поэтому сервисы и меню не зависят от JDBC.
 */
public class DataAccessException extends AutosalonException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
