package ru.mirea.autosalon;

import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.exception.DatabaseConnectionException;
import ru.mirea.autosalon.exception.InputClosedException;
import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
import ru.mirea.autosalon.repository.jdbc.DatabaseTableRepository;
import ru.mirea.autosalon.repository.jdbc.JdbcCarRepository;
import ru.mirea.autosalon.repository.jdbc.JdbcClientRepository;
import ru.mirea.autosalon.repository.jdbc.JdbcTestDriveRequestRepository;
import ru.mirea.autosalon.service.CarService;
import ru.mirea.autosalon.service.ClientService;
import ru.mirea.autosalon.service.DatabaseTableService;
import ru.mirea.autosalon.service.ExportService;
import ru.mirea.autosalon.service.StatisticsService;
import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.ui.CarMenu;
import ru.mirea.autosalon.ui.ClientMenu;
import ru.mirea.autosalon.ui.ConsoleInput;
import ru.mirea.autosalon.ui.DatabaseTablesMenu;
import ru.mirea.autosalon.ui.ExportMenu;
import ru.mirea.autosalon.ui.FilterMenu;
import ru.mirea.autosalon.ui.MainMenu;
import ru.mirea.autosalon.ui.SearchMenu;
import ru.mirea.autosalon.ui.SortMenu;
import ru.mirea.autosalon.ui.TestDriveRequestMenu;
import ru.mirea.autosalon.util.CsvExporter;
import ru.mirea.autosalon.util.DataExporter;
import ru.mirea.autosalon.util.DatabaseManager;
import ru.mirea.autosalon.util.ExcelExporter;

import java.time.Clock;
import java.util.List;

/**
 * Точка входа в консольную информационную систему «Автосалон: заявки на тест-драйв».
 * <p>
 * Main только создаёт объекты всех слоёв, связывает их и запускает главное меню:
 * Console UI -> Service -> Repository (JDBC) -> PostgreSQL.
 */
public class Main {

    public static void main(String[] args) {
        DatabaseManager databaseManager;
        try {
            databaseManager = DatabaseManager.fromConfig();
        } catch (AutosalonException e) {
            System.out.println("Ошибка конфигурации: " + e.getMessage());
            return;
        }
        Clock clock = Clock.systemDefaultZone();

        // Слой доступа к данным: репозитории на JDBC
        ClientRepository clientRepository = new JdbcClientRepository(databaseManager);
        CarRepository carRepository = new JdbcCarRepository(databaseManager);
        TestDriveRequestRepository requestRepository = new JdbcTestDriveRequestRepository(databaseManager);
        DatabaseTableRepository tableRepository = new DatabaseTableRepository(databaseManager);

        // Слой бизнес-логики: сервисы получают репозитории через конструктор
        ClientService clientService = new ClientService(clientRepository, requestRepository, clock);
        CarService carService = new CarService(carRepository, requestRepository, clock);
        TestDriveRequestService requestService =
                new TestDriveRequestService(requestRepository, clientRepository, carRepository, clock);
        StatisticsService statisticsService =
                new StatisticsService(clientRepository, carRepository, requestRepository, clock);
        ExportService exportService =
                new ExportService(clientRepository, carRepository, requestRepository, statisticsService, clock);
        DatabaseTableService tableService = new DatabaseTableService(tableRepository);

        // Слой пользовательского интерфейса: консольные меню
        ConsoleInput input = new ConsoleInput();
        List<DataExporter> exporters = List.of(new ExcelExporter(), new CsvExporter());
        MainMenu mainMenu = new MainMenu(input,
                new ClientMenu(input, clientService),
                new CarMenu(input, carService),
                new TestDriveRequestMenu(input, requestService, clientService, carService),
                new SearchMenu(input, requestService),
                new FilterMenu(input, requestService, clientService, carService),
                new SortMenu(input, requestService),
                statisticsService,
                new ExportMenu(input, exportService, exporters),
                new DatabaseTablesMenu(input, tableService));

        printConnectionStatus(databaseManager);
        try {
            mainMenu.run();
        } catch (InputClosedException e) {
            System.out.println();
            System.out.println("Ввод завершён.");
        }
        System.out.println("Работа программы завершена. До свидания!");
    }

    private static void printConnectionStatus(DatabaseManager databaseManager) {
        try {
            String serverVersion = databaseManager.checkConnection();
            System.out.println("Подключение к базе данных установлено: " + serverVersion
                    + " (" + databaseManager.getUrl() + ")");
        } catch (DatabaseConnectionException e) {
            System.out.println("ВНИМАНИЕ! Ошибка подключения к базе данных: " + e.getMessage());
            System.out.println("Программа продолжит работу, но операции с данными будут недоступны,"
                    + " пока подключение не восстановится.");
        }
    }
}
