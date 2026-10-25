package ru.mirea.autosalon;

import javafx.application.Application;

/**
 * Точка входа для запуска из IntelliJ IDEA (кнопка ▶ рядом с main) и из jar.
 * <p>
 * Если запускать напрямую класс-наследник {@link Application} без module-path, Java выдаёт ошибку
 * «JavaFX runtime components are missing». Отдельный класс с main, который не наследует Application,
 * обходит эту проверку. Через Maven приложение запускается командой {@code mvn javafx:run}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(AutosalonApp.class, args);
    }
}
