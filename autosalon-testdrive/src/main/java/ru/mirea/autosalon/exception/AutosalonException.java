package ru.mirea.autosalon.exception;

/**
 * Базовое исключение приложения. От него наследуются все собственные исключения,
 * поэтому их можно перехватывать как вместе, так и по отдельности.
 * Исключение непроверяемое (RuntimeException): его не нужно объявлять в throws каждого метода.
 */
public class AutosalonException extends RuntimeException {

    public AutosalonException(String message) {
        super(message);
    }

    public AutosalonException(String message, Throwable cause) {
        super(message, cause);
    }
}
