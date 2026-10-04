package ru.mirea.autosalon.util;

import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.exception.DatabaseConnectionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Выдаёт подключения (Connection) к базе данных PostgreSQL через JDBC.
 * <p>
 * Параметры подключения читаются из файла {@code db.properties}: сначала из ресурсов приложения,
 * затем, если в папке запуска есть свой файл {@code db.properties}, его значения переопределяют их.
 * Каждый репозиторий берёт новое подключение на одну операцию и закрывает его в try-with-resources.
 */
public class DatabaseManager {

    private static final String CONFIG_FILE = "db.properties";
    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
    }

    /** Создаёт DatabaseManager по настройкам из db.properties. */
    public static DatabaseManager fromConfig() {
        Properties properties = new Properties();
        try (InputStream resource = DatabaseManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (resource != null) {
                properties.load(new InputStreamReader(resource, StandardCharsets.UTF_8));
            }
            Path externalConfig = Path.of(CONFIG_FILE);
            if (Files.isRegularFile(externalConfig)) {
                try (Reader reader = Files.newBufferedReader(externalConfig, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
        } catch (IOException e) {
            throw new AutosalonException("не удалось прочитать файл " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
        return new DatabaseManager(
                requireProperty(properties, "db.url"),
                requireProperty(properties, "db.user"),
                properties.getProperty("db.password", ""));
    }

    private static String requireProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new AutosalonException("в файле " + CONFIG_FILE + " не задан параметр " + key);
        }
        return value.trim();
    }

    /**
     * Открывает новое подключение. Вызывающий код обязан закрыть его (try-with-resources).
     *
     * @throws DatabaseConnectionException если сервер недоступен или неверны параметры подключения
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseConnectionException(describeConnectionProblem(e), e);
        }
    }

    /**
     * Понятное описание ошибки подключения по коду SQLState.
     * PostgreSQL с русской локалью в Windows присылает сообщения об ошибках входа в кодировке cp1251
     * ещё до согласования кодировки, поэтому такой текст нечитаем и заменяется кодом ошибки.
     */
    private String describeConnectionProblem(SQLException e) {
        String sqlState = e.getSQLState() == null ? "" : e.getSQLState();
        String reason = switch (sqlState) {
            case "28P01", "28000" -> "неверное имя пользователя или пароль, проверьте db.user и db.password в "
                    + CONFIG_FILE;
            case "3D000" -> "база данных не найдена, создайте её скриптом sql/01_create_database.sql";
            case "08001", "08004", "08006" -> "сервер PostgreSQL недоступен, проверьте, что он запущен,"
                    + " а адрес и порт в db.url указаны верно";
            default -> "проверьте, что сервер PostgreSQL запущен, база данных создана, а параметры в "
                    + CONFIG_FILE + " указаны верно";
        };
        String message = e.getMessage();
        String details = message == null || message.indexOf('�') >= 0 ? "SQLState " + sqlState : message;
        return "не удалось подключиться к " + url + " (пользователь " + user + "): " + reason
                + ". Подробности: " + details;
    }

    /** Проверяет подключение и возвращает название и версию сервера БД. */
    public String checkConnection() {
        try (Connection connection = getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new DatabaseConnectionException("ошибка при проверке подключения: " + e.getMessage(), e);
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }
}
