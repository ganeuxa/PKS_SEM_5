package ru.mirea.autosalon.repository;

import java.util.List;

/**
 * Содержимое таблицы в «сыром» виде: заголовок, названия столбцов и строки значений.
 * Используется для пункта меню «Вывести таблицы базы данных».
 */
public class TableData {

    private final String title;
    private final List<String> columns;
    private final List<List<String>> rows;

    public TableData(String title, List<String> columns, List<List<String>> rows) {
        this.title = title;
        this.columns = List.copyOf(columns);
        this.rows = List.copyOf(rows);
    }

    public String getTitle() {
        return title;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<String>> getRows() {
        return rows;
    }
}
