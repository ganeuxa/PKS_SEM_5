package ru.mirea.autosalon.model;

import java.util.Objects;

/**
 * Базовый класс всех сущностей предметной области.
 * <p>
 * Хранит идентификатор (первичный ключ в базе данных) и задаёт общее правило
 * сравнения объектов: две сущности одного типа равны, если у них одинаковый ID.
 * Метод {@link #getDisplayName()} абстрактный: каждая сущность сама решает,
 * как кратко описать себя пользователю (полиморфизм).
 */
public abstract class BaseEntity {

    private Long id;

    protected BaseEntity() {
    }

    protected BaseEntity(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Краткое описание объекта для вывода в консоль (например, в списках выбора). */
    public abstract String getDisplayName();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        BaseEntity entity = (BaseEntity) other;
        return id != null && id.equals(entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
