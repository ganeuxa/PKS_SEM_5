package ru.mirea.autosalon.repository;

import ru.mirea.autosalon.model.BaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Общий интерфейс хранилища сущностей: базовые операции CRUD
 * (Create, Read, Update, Delete).
 * <p>
 * Сервисы работают только с интерфейсами репозиториев и не знают, как именно хранятся данные.
 * В приложении используются JDBC-реализации (PostgreSQL), а в модульных тестах — реализации в памяти.
 *
 * @param <T> тип сущности
 */
public interface CrudRepository<T extends BaseEntity> {

    /** Сохраняет новую сущность и возвращает её с присвоенным базой данных ID. */
    T save(T entity);

    /** Обновляет существующую сущность. Возвращает false, если записи с таким ID нет. */
    boolean update(T entity);

    Optional<T> findById(long id);

    List<T> findAll();

    /** Удаляет запись. Возвращает false, если записи с таким ID нет. */
    boolean deleteById(long id);
}
