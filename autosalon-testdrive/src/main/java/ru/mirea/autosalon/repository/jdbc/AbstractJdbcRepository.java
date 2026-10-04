package ru.mirea.autosalon.repository.jdbc;

import ru.mirea.autosalon.exception.DataAccessException;
import ru.mirea.autosalon.exception.DatabaseConnectionException;
import ru.mirea.autosalon.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Базовый класс JDBC-репозиториев. Содержит общий порядок работы с JDBC:
 * <ol>
 *     <li>получить {@link Connection} у {@link DatabaseManager};</li>
 *     <li>подготовить {@link PreparedStatement} с параметрами «?»;</li>
 *     <li>подставить значения параметров методами setXxx();</li>
 *     <li>выполнить запрос и прочитать {@link ResultSet};</li>
 *     <li>закрыть все ресурсы — это делает try-with-resources, даже если произошла ошибка.</li>
 * </ol>
 * Значения никогда не склеиваются со строкой SQL, поэтому SQL-инъекция невозможна.
 */
public abstract class AbstractJdbcRepository {

    /** Подставляет значения параметров в подготовленный запрос. */
    @FunctionalInterface
    protected interface ParameterSetter {
        void setParameters(PreparedStatement statement) throws SQLException;
    }

    /** Превращает текущую строку ResultSet в объект. */
    @FunctionalInterface
    protected interface RowMapper<T> {
        T mapRow(ResultSet resultSet) throws SQLException;
    }

    /** Для запросов без параметров. */
    protected static final ParameterSetter NO_PARAMETERS = statement -> {
    };

    private final DatabaseManager databaseManager;

    protected AbstractJdbcRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    protected Connection getConnection() {
        return databaseManager.getConnection();
    }

    /** Выполняет SELECT и возвращает все найденные строки в виде списка объектов. */
    protected <T> List<T> queryForList(String sql, ParameterSetter parameters, RowMapper<T> mapper,
                                       String operation) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            parameters.setParameters(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(mapper.mapRow(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw translateException(operation, e);
        }
    }

    /** Выполняет SELECT, который возвращает не больше одной строки. */
    protected <T> Optional<T> queryForOptional(String sql, ParameterSetter parameters, RowMapper<T> mapper,
                                               String operation) {
        List<T> result = queryForList(sql, parameters, mapper, operation);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /** Выполняет INSERT, UPDATE или DELETE и возвращает число изменённых строк. */
    protected int executeUpdate(String sql, ParameterSetter parameters, String operation) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            parameters.setParameters(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw translateException(operation, e);
        }
    }

    /**
     * Переводит SQLException в исключение приложения с понятным описанием.
     * Код SQLState описывает класс ошибки: 08xxx — проблемы соединения, 23xxx — нарушение ограничений БД.
     */
    protected DataAccessException translateException(String operation, SQLException e) {
        String sqlState = e.getSQLState() == null ? "" : e.getSQLState();
        if (sqlState.startsWith("08")) {
            return new DatabaseConnectionException("соединение с базой данных потеряно (" + operation + "). "
                    + e.getMessage(), e);
        }
        String reason = switch (sqlState) {
            case "23505" -> "нарушено ограничение уникальности (UNIQUE)";
            case "23503" -> "нарушено ограничение внешнего ключа (FOREIGN KEY)";
            case "23514" -> "нарушено ограничение допустимых значений (CHECK)";
            case "23502" -> "не заполнено обязательное поле (NOT NULL)";
            case "42P01" -> "таблица не найдена, выполните скрипт sql/02_schema_and_data.sql";
            case "42703" -> "столбец не найден, структура БД не совпадает с ожидаемой";
            default -> "ошибка SQL";
        };
        return new DataAccessException(operation + ": " + reason + " [SQLState " + sqlState + "]. "
                + e.getMessage(), e);
    }
}
