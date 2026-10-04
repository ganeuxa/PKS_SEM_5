package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.repository.DatabaseTable;
import ru.mirea.autosalon.service.DatabaseTableService;

/**
 * Меню «Вывести таблицы базы данных»: содержимое таблиц и их структура (столбцы, ключи, ограничения).
 */
public class DatabaseTablesMenu extends AbstractMenu {

    private final DatabaseTableService tableService;

    public DatabaseTablesMenu(ConsoleInput input, DatabaseTableService tableService) {
        super("ТАБЛИЦЫ БАЗЫ ДАННЫХ", input);
        this.tableService = tableService;
        for (DatabaseTable table : DatabaseTable.values()) {
            addItem("Таблица " + table.getTableName() + " (" + table.getDescription() + ")",
                    () -> ConsoleView.printTableData(tableService.getContent(table)));
        }
        addItem("Все таблицы", this::printAllTables);
        addItem("Структура таблиц: столбцы, ключи и ограничения", this::printStructure);
    }

    private void printAllTables() {
        for (DatabaseTable table : DatabaseTable.values()) {
            ConsoleView.printTableData(tableService.getContent(table));
            System.out.println();
        }
    }

    private void printStructure() {
        for (DatabaseTable table : DatabaseTable.values()) {
            ConsoleView.printTableData(tableService.getStructure(table));
            ConsoleView.printConstraints(tableService.getConstraints(table));
            System.out.println();
        }
    }
}
