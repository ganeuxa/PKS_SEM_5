package ru.mirea.autosalon.exception;

/**
 * Ошибка экспорта данных в файл, например файл открыт в Excel или нет прав на запись в папку.
 */
public class ExportException extends AutosalonException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
