package ru.mirea.autosalon.ui;

/**
 * Пункт выпадающего списка фильтра: подпись для пользователя и значение.
 * Значение null означает «без фильтра» (например, «Все статусы»).
 * ComboBox показывает пункт через toString(), поэтому toString() возвращает подпись.
 */
public record Choice<T>(String label, T value) {

    public static <T> Choice<T> any(String label) {
        return new Choice<>(label, null);
    }

    @Override
    public String toString() {
        return label;
    }
}
