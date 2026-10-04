package ru.mirea.autosalon.repository.jdbc;

import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация хранилища клиентов (таблица {@code clients}).
 */
public class JdbcClientRepository extends AbstractJdbcRepository implements ClientRepository {

    private static final String SELECT_CLIENTS =
            "SELECT id, full_name, phone, email, birth_date, license_number, registered_at FROM clients";

    public JdbcClientRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    public Client save(Client client) {
        String sql = "INSERT INTO clients (full_name, phone, email, birth_date, license_number) "
                + "VALUES (?, ?, ?, ?, ?)";
        // Просим драйвер вернуть значения, которые сгенерировала база: ID и дату регистрации
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id", "registered_at"})) {
            statement.setString(1, client.getFullName());
            statement.setString(2, client.getPhone());
            statement.setString(3, client.getEmail());
            statement.setObject(4, client.getBirthDate());
            statement.setString(5, client.getLicenseNumber());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    client.setId(generatedKeys.getLong("id"));
                    client.setRegisteredAt(generatedKeys.getObject("registered_at", LocalDateTime.class));
                }
            }
            return client;
        } catch (SQLException e) {
            throw translateException("добавление клиента", e);
        }
    }

    @Override
    public boolean update(Client client) {
        String sql = "UPDATE clients SET full_name = ?, phone = ?, email = ?, birth_date = ?, license_number = ? "
                + "WHERE id = ?";
        int updatedRows = executeUpdate(sql, statement -> {
            statement.setString(1, client.getFullName());
            statement.setString(2, client.getPhone());
            statement.setString(3, client.getEmail());
            statement.setObject(4, client.getBirthDate());
            statement.setString(5, client.getLicenseNumber());
            statement.setLong(6, client.getId());
        }, "изменение клиента");
        return updatedRows > 0;
    }

    @Override
    public Optional<Client> findById(long id) {
        return queryForOptional(SELECT_CLIENTS + " WHERE id = ?",
                statement -> statement.setLong(1, id),
                resultSet -> mapClient(resultSet, ""),
                "поиск клиента по ID");
    }

    @Override
    public List<Client> findAll() {
        return queryForList(SELECT_CLIENTS + " ORDER BY id", NO_PARAMETERS,
                resultSet -> mapClient(resultSet, ""),
                "получение списка клиентов");
    }

    @Override
    public boolean deleteById(long id) {
        int deletedRows = executeUpdate("DELETE FROM clients WHERE id = ?",
                statement -> statement.setLong(1, id),
                "удаление клиента");
        return deletedRows > 0;
    }

    @Override
    public Optional<Client> findByPhone(String phone) {
        return queryForOptional(SELECT_CLIENTS + " WHERE phone = ?",
                statement -> statement.setString(1, phone),
                resultSet -> mapClient(resultSet, ""),
                "поиск клиента по телефону");
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return queryForOptional(SELECT_CLIENTS + " WHERE email = ?",
                statement -> statement.setString(1, email),
                resultSet -> mapClient(resultSet, ""),
                "поиск клиента по email");
    }

    @Override
    public Optional<Client> findByLicenseNumber(String licenseNumber) {
        return queryForOptional(SELECT_CLIENTS + " WHERE license_number = ?",
                statement -> statement.setString(1, licenseNumber),
                resultSet -> mapClient(resultSet, ""),
                "поиск клиента по номеру водительского удостоверения");
    }

    /**
     * Создаёт объект Client из текущей строки ResultSet.
     *
     * @param prefix префикс имён столбцов: в запросах с JOIN столбцы клиента называются client_id,
     *               client_full_name и т.д., а в запросах к одной таблице clients префикс пустой
     */
    static Client mapClient(ResultSet resultSet, String prefix) throws SQLException {
        return new Client(
                resultSet.getLong(prefix + "id"),
                resultSet.getString(prefix + "full_name"),
                resultSet.getString(prefix + "phone"),
                resultSet.getString(prefix + "email"),
                resultSet.getObject(prefix + "birth_date", LocalDate.class),
                resultSet.getString(prefix + "license_number"),
                resultSet.getObject(prefix + "registered_at", LocalDateTime.class));
    }
}
