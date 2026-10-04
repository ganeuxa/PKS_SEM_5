package ru.mirea.autosalon.repository.jdbc;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
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
 * JDBC-реализация хранилища заявок на тест-драйв (таблица {@code test_drive_requests}).
 * Заявки читаются одним запросом с JOIN вместе с данными клиента и автомобиля.
 */
public class JdbcTestDriveRequestRepository extends AbstractJdbcRepository implements TestDriveRequestRepository {

    /** Заявка + клиент (client_id -> clients.id) + автомобиль (car_id -> cars.id). */
    private static final String SELECT_REQUESTS = """
            SELECT r.id, r.scheduled_at, r.duration_minutes, r.status, r.comment, r.created_at, r.updated_at,
                   c.id AS client_id, c.full_name AS client_full_name, c.phone AS client_phone,
                   c.email AS client_email, c.birth_date AS client_birth_date,
                   c.license_number AS client_license_number, c.registered_at AS client_registered_at,
                   car.id AS car_id, car.brand AS car_brand, car.model AS car_model,
                   car.manufacture_year AS car_manufacture_year, car.vin AS car_vin, car.color AS car_color,
                   car.price AS car_price, car.status AS car_status
            FROM test_drive_requests r
                     JOIN clients c ON c.id = r.client_id
                     JOIN cars car ON car.id = r.car_id
            """;

    private static final String ORDER_BY_SCHEDULE = " ORDER BY r.scheduled_at, r.id";

    public JdbcTestDriveRequestRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    public TestDriveRequest save(TestDriveRequest request) {
        String sql = "INSERT INTO test_drive_requests (client_id, car_id, scheduled_at, duration_minutes, status, "
                + "comment) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql,
                     new String[]{"id", "created_at", "updated_at"})) {
            statement.setLong(1, request.getClient().getId());
            statement.setLong(2, request.getCar().getId());
            statement.setObject(3, request.getScheduledAt());
            statement.setInt(4, request.getDurationMinutes());
            statement.setString(5, request.getStatus().name());
            statement.setString(6, request.getComment());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    request.setId(generatedKeys.getLong("id"));
                    request.setCreatedAt(generatedKeys.getObject("created_at", LocalDateTime.class));
                    request.setUpdatedAt(generatedKeys.getObject("updated_at", LocalDateTime.class));
                }
            }
            return request;
        } catch (SQLException e) {
            throw translateException("создание заявки на тест-драйв", e);
        }
    }

    @Override
    public boolean update(TestDriveRequest request) {
        String sql = """
                UPDATE test_drive_requests
                SET client_id = ?, car_id = ?, scheduled_at = ?, duration_minutes = ?, status = ?, comment = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
        int updatedRows = executeUpdate(sql, statement -> {
            statement.setLong(1, request.getClient().getId());
            statement.setLong(2, request.getCar().getId());
            statement.setObject(3, request.getScheduledAt());
            statement.setInt(4, request.getDurationMinutes());
            statement.setString(5, request.getStatus().name());
            statement.setString(6, request.getComment());
            statement.setLong(7, request.getId());
        }, "изменение заявки на тест-драйв");
        return updatedRows > 0;
    }

    @Override
    public Optional<TestDriveRequest> findById(long id) {
        return queryForOptional(SELECT_REQUESTS + " WHERE r.id = ?",
                statement -> statement.setLong(1, id),
                this::mapRequest,
                "поиск заявки по ID");
    }

    @Override
    public List<TestDriveRequest> findAll() {
        return queryForList(SELECT_REQUESTS + " ORDER BY r.id", NO_PARAMETERS, this::mapRequest,
                "получение списка заявок");
    }

    @Override
    public boolean deleteById(long id) {
        int deletedRows = executeUpdate("DELETE FROM test_drive_requests WHERE id = ?",
                statement -> statement.setLong(1, id),
                "удаление заявки на тест-драйв");
        return deletedRows > 0;
    }

    @Override
    public List<TestDriveRequest> findByClientId(long clientId) {
        return queryForList(SELECT_REQUESTS + " WHERE r.client_id = ?" + ORDER_BY_SCHEDULE,
                statement -> statement.setLong(1, clientId),
                this::mapRequest,
                "получение заявок клиента");
    }

    @Override
    public List<TestDriveRequest> findByCarId(long carId) {
        return queryForList(SELECT_REQUESTS + " WHERE r.car_id = ?" + ORDER_BY_SCHEDULE,
                statement -> statement.setLong(1, carId),
                this::mapRequest,
                "получение заявок по автомобилю");
    }

    @Override
    public List<TestDriveRequest> searchByClientName(String namePart) {
        return queryForList(SELECT_REQUESTS + " WHERE LOWER(c.full_name) LIKE LOWER(?) ESCAPE '\\'"
                        + ORDER_BY_SCHEDULE,
                statement -> statement.setString(1, containsPattern(namePart)),
                this::mapRequest,
                "поиск заявок по ФИО клиента");
    }

    @Override
    public List<TestDriveRequest> searchByClientPhone(String phonePart) {
        return queryForList(SELECT_REQUESTS + " WHERE c.phone LIKE ? ESCAPE '\\'" + ORDER_BY_SCHEDULE,
                statement -> statement.setString(1, containsPattern(phonePart)),
                this::mapRequest,
                "поиск заявок по телефону клиента");
    }

    @Override
    public List<TestDriveRequest> searchByCar(String brandOrModelPart) {
        return queryForList(SELECT_REQUESTS + " WHERE LOWER(car.brand || ' ' || car.model) LIKE LOWER(?) ESCAPE '\\'"
                        + ORDER_BY_SCHEDULE,
                statement -> statement.setString(1, containsPattern(brandOrModelPart)),
                this::mapRequest,
                "поиск заявок по автомобилю");
    }

    @Override
    public List<TestDriveRequest> findByDate(LocalDate date) {
        return queryForList(SELECT_REQUESTS + " WHERE r.scheduled_at >= ? AND r.scheduled_at < ?" + ORDER_BY_SCHEDULE,
                statement -> {
                    statement.setObject(1, date.atStartOfDay());
                    statement.setObject(2, date.plusDays(1).atStartOfDay());
                },
                this::mapRequest,
                "поиск заявок по дате тест-драйва");
    }

    /**
     * Шаблон для LIKE «содержит текст»: %текст%. Символы % и _ из ввода пользователя экранируются,
     * чтобы они искались как обычные символы, а не работали как подстановочные знаки.
     */
    private static String containsPattern(String text) {
        String escaped = text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private TestDriveRequest mapRequest(ResultSet resultSet) throws SQLException {
        Client client = JdbcClientRepository.mapClient(resultSet, "client_");
        Car car = JdbcCarRepository.mapCar(resultSet, "car_");
        return new TestDriveRequest(
                resultSet.getLong("id"),
                client,
                car,
                resultSet.getObject("scheduled_at", LocalDateTime.class),
                resultSet.getInt("duration_minutes"),
                TestDriveStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("comment"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class));
    }
}
