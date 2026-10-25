package ru.mirea.autosalon.controller;

/**
 * Контроллер формы в диалоговом окне (см. {@link ru.mirea.autosalon.ui.FormDialog}).
 *
 * @param <R> тип объекта, который сохраняет форма
 */
public interface FormController<R> {

    /** Заголовок окна, например «Новая заявка на тест-драйв». */
    String getTitle();

    /**
     * Проверяет введённые данные и сохраняет объект через сервис.
     *
     * @return true, если объект сохранён и окно можно закрыть; false, если форма показала ошибку
     */
    boolean save();

    /** Сохранённый объект (после успешного {@link #save()}). */
    R getResult();
}
