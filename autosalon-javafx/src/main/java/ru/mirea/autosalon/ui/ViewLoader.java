package ru.mirea.autosalon.ui;

import javafx.fxml.FXMLLoader;
import ru.mirea.autosalon.AppContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;

/**
 * Загрузка FXML-файлов из папки resources/ru/mirea/autosalon/view.
 * <p>
 * FXMLLoader сам создаёт контроллер, указанный в атрибуте {@code fx:controller}. С помощью controllerFactory
 * мы передаём в конструктор контроллера {@link AppContext} с сервисами. Фабрика наследуется вложенными файлами
 * ({@code fx:include}), поэтому контроллеры вкладок тоже получают сервисы.
 */
public final class ViewLoader {

    private static final String VIEW_FOLDER = "/ru/mirea/autosalon/view/";
    private static final String STYLESHEET = "/ru/mirea/autosalon/css/app.css";

    private ViewLoader() {
    }

    public static FXMLLoader loader(String fxmlFile, AppContext context) {
        URL location = ViewLoader.class.getResource(VIEW_FOLDER + fxmlFile);
        if (location == null) {
            throw new IllegalStateException("Не найден файл интерфейса " + VIEW_FOLDER + fxmlFile);
        }
        FXMLLoader loader = new FXMLLoader(location);
        loader.setControllerFactory(type -> createController(type, context));
        return loader;
    }

    /** Загружает FXML и возвращает корневой узел интерфейса. */
    public static <T> T load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить интерфейс " + loader.getLocation(), e);
        }
    }

    /** Адрес файла стилей для Scene и диалоговых окон. */
    public static String stylesheet() {
        URL css = ViewLoader.class.getResource(STYLESHEET);
        if (css == null) {
            throw new IllegalStateException("Не найден файл стилей " + STYLESHEET);
        }
        return css.toExternalForm();
    }

    /** Контроллер с конструктором (AppContext) получает сервисы, остальные создаются конструктором без параметров. */
    private static Object createController(Class<?> type, AppContext context) {
        try {
            try {
                Constructor<?> constructor = type.getConstructor(AppContext.class);
                return constructor.newInstance(context);
            } catch (NoSuchMethodException e) {
                return type.getConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось создать контроллер " + type.getName(), e);
        }
    }
}
