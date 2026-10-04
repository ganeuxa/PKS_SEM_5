package ru.mirea.autosalon.repository.jdbc;

import ru.mirea.autosalon.repository.DatabaseTable;
import ru.mirea.autosalon.repository.TableData;
import ru.mirea.autosalon.util.DatabaseManager;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Чтение таблиц базы данных «как есть»: строки таблицы, описание столбцов и ограничения.
 * Столбцы определяются во время выполнения через ResultSetMetaData и DatabaseMetaData.
 */
public class DatabaseTableRepository extends AbstractJdbcRepository {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SCHEMA = "public";

    public DatabaseTableRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    /** Все строки таблицы (SELECT *). */
    public TableData loadContent(DatabaseTable table) {
        // Имя таблицы берётся из перечисления DatabaseTable (белый список), а не из ввода пользователя
        String sql = "SELECT * FROM " + table.getTableName() + " ORDER BY id";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(metaData.getColumnLabel(i));
            }
            List<List<String>> rows = new ArrayList<>();
            while (resultSet.next()) {
                List<String> row = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(formatValue(resultSet.getObject(i)));
                }
                rows.add(row);
            }
            return new TableData("Таблица " + table.getTableName() + " (" + table.getDescription() + ")",
                    columns, rows);
        } catch (SQLException e) {
            throw translateException("чтение таблицы " + table.getTableName(), e);
        }
    }

    /** Столбцы таблицы: тип, обязательность, первичный и внешние ключи (через DatabaseMetaData). */
    public TableData loadStructure(DatabaseTable table) {
        String tableName = table.getTableName();
        try (Connection connection = getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();

            Set<String> primaryKeyColumns = new HashSet<>();
            try (ResultSet keys = metaData.getPrimaryKeys(null, SCHEMA, tableName)) {
                while (keys.next()) {
                    primaryKeyColumns.add(keys.getString("COLUMN_NAME"));
                }
            }
            Map<String, String> foreignKeys = new HashMap<>();
            try (ResultSet keys = metaData.getImportedKeys(null, SCHEMA, tableName)) {
                while (keys.next()) {
                    foreignKeys.put(keys.getString("FKCOLUMN_NAME"),
                            keys.getString("PKTABLE_NAME") + "." + keys.getString("PKCOLUMN_NAME"));
                }
            }

            List<List<String>> rows = new ArrayList<>();
            try (ResultSet columns = metaData.getColumns(null, SCHEMA, tableName, null)) {
                while (columns.next()) {
                    String column = columns.getString("COLUMN_NAME");
                    List<String> keys = new ArrayList<>();
                    if (primaryKeyColumns.contains(column)) {
                        keys.add("PRIMARY KEY");
                    }
                    if (foreignKeys.containsKey(column)) {
                        keys.add("FOREIGN KEY -> " + foreignKeys.get(column));
                    }
                    String defaultValue = "YES".equals(columns.getString("IS_AUTOINCREMENT"))
                            ? "IDENTITY (автоувеличение)"
                            : columns.getString("COLUMN_DEF");
                    rows.add(List.of(
                            column,
                            formatType(columns.getString("TYPE_NAME"), columns.getInt("COLUMN_SIZE"),
                                    columns.getInt("DECIMAL_DIGITS")),
                            columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls ? "да" : "нет",
                            String.join(", ", keys),
                            defaultValue == null ? "" : defaultValue));
                }
            }
            return new TableData("Структура таблицы " + tableName,
                    List.of("Столбец", "Тип", "NOT NULL", "Ключ", "Значение по умолчанию"), rows);
        } catch (SQLException e) {
            throw translateException("чтение структуры таблицы " + tableName, e);
        }
    }

    /** Ограничения таблицы (PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK) из системного каталога PostgreSQL. */
    public TableData loadConstraints(DatabaseTable table) {
        String sql = """
                SELECT conname, contype, pg_get_constraintdef(oid) AS definition
                FROM pg_constraint
                WHERE conrelid = CAST(? AS regclass) AND contype IN ('p', 'f', 'u', 'c')
                ORDER BY CASE contype WHEN 'p' THEN 1 WHEN 'f' THEN 2 WHEN 'u' THEN 3 ELSE 4 END, conname
                """;
        List<List<String>> rows = queryForList(sql,
                statement -> statement.setString(1, table.getTableName()),
                resultSet -> List.of(
                        resultSet.getString("conname"),
                        constraintTypeName(resultSet.getString("contype")),
                        resultSet.getString("definition")),
                "чтение ограничений таблицы " + table.getTableName());
        return new TableData("Ограничения таблицы " + table.getTableName(),
                List.of("Ограничение", "Тип", "Определение"), rows);
    }

    private static String constraintTypeName(String code) {
        return switch (code) {
            case "p" -> "PRIMARY KEY";
            case "f" -> "FOREIGN KEY";
            case "u" -> "UNIQUE";
            case "c" -> "CHECK";
            default -> code;
        };
    }

    private static String formatType(String typeName, int size, int decimalDigits) {
        return switch (typeName) {
            case "int8" -> "BIGINT";
            case "int4" -> "INTEGER";
            case "int2" -> "SMALLINT";
            case "varchar" -> "VARCHAR(" + size + ")";
            case "bpchar" -> "CHAR(" + size + ")";
            case "numeric" -> "NUMERIC(" + size + "," + decimalDigits + ")";
            default -> typeName.toUpperCase();
        };
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Timestamp timestamp) {
            return TIMESTAMP_FORMAT.format(timestamp.toLocalDateTime());
        }
        return value.toString();
    }
}
