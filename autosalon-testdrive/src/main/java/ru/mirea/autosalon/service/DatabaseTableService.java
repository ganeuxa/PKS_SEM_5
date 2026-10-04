package ru.mirea.autosalon.service;

import ru.mirea.autosalon.repository.DatabaseTable;
import ru.mirea.autosalon.repository.TableData;
import ru.mirea.autosalon.repository.jdbc.DatabaseTableRepository;

/**
 * Просмотр таблиц базы данных «как есть»: содержимое, столбцы и ограничения.
 */
public class DatabaseTableService {

    private final DatabaseTableRepository tableRepository;

    public DatabaseTableService(DatabaseTableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public TableData getContent(DatabaseTable table) {
        return tableRepository.loadContent(table);
    }

    public TableData getStructure(DatabaseTable table) {
        return tableRepository.loadStructure(table);
    }

    public TableData getConstraints(DatabaseTable table) {
        return tableRepository.loadConstraints(table);
    }
}
