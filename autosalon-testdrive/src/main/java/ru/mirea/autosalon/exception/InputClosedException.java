package ru.mirea.autosalon.exception;

/**
 * Поток ввода закрыт (например, пользователь нажал Ctrl+Z / Ctrl+D или ввод перенаправлен из файла
 * и файл закончился). Программа в этом случае корректно завершает работу.
 */
public class InputClosedException extends AutosalonException {

    public InputClosedException() {
        super("Поток ввода закрыт");
    }

    public InputClosedException(Throwable cause) {
        super("Поток ввода закрыт", cause);
    }
}
