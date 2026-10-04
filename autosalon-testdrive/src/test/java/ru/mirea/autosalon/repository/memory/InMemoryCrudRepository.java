package ru.mirea.autosalon.repository.memory;

import ru.mirea.autosalon.model.BaseEntity;
import ru.mirea.autosalon.repository.CrudRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Хранилище в памяти для модульных тестов: сервисы проверяются без базы данных,
 * потому что зависят только от интерфейсов репозиториев.
 */
abstract class InMemoryCrudRepository<T extends BaseEntity> implements CrudRepository<T> {

    protected final Map<Long, T> storage = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public T save(T entity) {
        entity.setId(nextId++);
        storage.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public boolean update(T entity) {
        if (!storage.containsKey(entity.getId())) {
            return false;
        }
        storage.put(entity.getId(), entity);
        return true;
    }

    @Override
    public Optional<T> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean deleteById(long id) {
        return storage.remove(id) != null;
    }
}
