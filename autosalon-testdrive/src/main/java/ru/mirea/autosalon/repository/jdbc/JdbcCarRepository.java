package ru.mirea.autosalon.repository.jdbc;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.CarStatus;
import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация хранилища автомобилей (таблица {@code cars}).
 */
public class JdbcCarRepository extends AbstractJdbcRepository implements CarRepository {

    private static final String SELECT_CARS =
            "SELECT id, brand, model, manufacture_year, vin, color, price, status FROM cars";

    public JdbcCarRepository(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override
    public Car save(Car car) {
        String sql = "INSERT INTO cars (brand, model, manufacture_year, vin, color, price, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"})) {
            statement.setString(1, car.getBrand());
            statement.setString(2, car.getModel());
            statement.setInt(3, car.getManufactureYear());
            statement.setString(4, car.getVin());
            statement.setString(5, car.getColor());
            statement.setBigDecimal(6, car.getPrice());
            statement.setString(7, car.getStatus().name());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    car.setId(generatedKeys.getLong("id"));
                }
            }
            return car;
        } catch (SQLException e) {
            throw translateException("добавление автомобиля", e);
        }
    }

    @Override
    public boolean update(Car car) {
        String sql = "UPDATE cars SET brand = ?, model = ?, manufacture_year = ?, vin = ?, color = ?, price = ?, "
                + "status = ? WHERE id = ?";
        int updatedRows = executeUpdate(sql, statement -> {
            statement.setString(1, car.getBrand());
            statement.setString(2, car.getModel());
            statement.setInt(3, car.getManufactureYear());
            statement.setString(4, car.getVin());
            statement.setString(5, car.getColor());
            statement.setBigDecimal(6, car.getPrice());
            statement.setString(7, car.getStatus().name());
            statement.setLong(8, car.getId());
        }, "изменение автомобиля");
        return updatedRows > 0;
    }

    @Override
    public Optional<Car> findById(long id) {
        return queryForOptional(SELECT_CARS + " WHERE id = ?",
                statement -> statement.setLong(1, id),
                resultSet -> mapCar(resultSet, ""),
                "поиск автомобиля по ID");
    }

    @Override
    public List<Car> findAll() {
        return queryForList(SELECT_CARS + " ORDER BY id", NO_PARAMETERS,
                resultSet -> mapCar(resultSet, ""),
                "получение списка автомобилей");
    }

    @Override
    public boolean deleteById(long id) {
        int deletedRows = executeUpdate("DELETE FROM cars WHERE id = ?",
                statement -> statement.setLong(1, id),
                "удаление автомобиля");
        return deletedRows > 0;
    }

    @Override
    public Optional<Car> findByVin(String vin) {
        return queryForOptional(SELECT_CARS + " WHERE vin = ?",
                statement -> statement.setString(1, vin),
                resultSet -> mapCar(resultSet, ""),
                "поиск автомобиля по VIN");
    }

    /**
     * Создаёт объект Car из текущей строки ResultSet.
     *
     * @param prefix префикс имён столбцов (car_ в запросах с JOIN, пустая строка — в запросах к таблице cars)
     */
    static Car mapCar(ResultSet resultSet, String prefix) throws SQLException {
        return new Car(
                resultSet.getLong(prefix + "id"),
                resultSet.getString(prefix + "brand"),
                resultSet.getString(prefix + "model"),
                resultSet.getInt(prefix + "manufacture_year"),
                resultSet.getString(prefix + "vin"),
                resultSet.getString(prefix + "color"),
                resultSet.getBigDecimal(prefix + "price"),
                CarStatus.valueOf(resultSet.getString(prefix + "status")));
    }
}
