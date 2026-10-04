package ru.mirea.autosalon.repository;

/**
 * Таблицы базы данных, которые можно вывести на экран.
 * <p>
 * Имя таблицы нельзя передать в PreparedStatement как параметр «?», поэтому оно подставляется в SQL
 * только из этого перечисления (белый список), а не из ввода пользователя.
 */
public enum DatabaseTable {

    CLIENTS("clients", "клиенты"),
    CARS("cars", "автомобили"),
    TEST_DRIVE_REQUESTS("test_drive_requests", "заявки на тест-драйв");

    private final String tableName;
    private final String description;

    DatabaseTable(String tableName, String description) {
        this.tableName = tableName;
        this.description = description;
    }

    public String getTableName() {
        return tableName;
    }

    public String getDescription() {
        return description;
    }
}
