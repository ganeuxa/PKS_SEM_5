package ru.mirea.autosalon;

import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
import ru.mirea.autosalon.repository.jdbc.JdbcCarRepository;
import ru.mirea.autosalon.repository.jdbc.JdbcClientRepository;
import ru.mirea.autosalon.repository.jdbc.JdbcTestDriveRequestRepository;
import ru.mirea.autosalon.service.CarService;
import ru.mirea.autosalon.service.ClientService;
import ru.mirea.autosalon.service.ExportService;
import ru.mirea.autosalon.service.StatisticsService;
import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.util.DatabaseManager;

import java.time.Clock;

/**
 * Объекты всех слоёв приложения, связанные между собой:
 * {@code DatabaseManager -> Jdbc*Repository -> *Service}.
 * <p>
 * Создаётся один раз при запуске и передаётся в конструкторы контроллеров JavaFX
 * (см. {@link ru.mirea.autosalon.ui.ViewLoader}). Контроллеры получают только сервисы
 * и не работают с репозиториями и SQL напрямую.
 */
public record AppContext(DatabaseManager databaseManager,
                         ClientService clientService,
                         CarService carService,
                         TestDriveRequestService requestService,
                         StatisticsService statisticsService,
                         ExportService exportService) {

    /** Читает database.properties и создаёт репозитории и сервисы. Подключение к БД здесь ещё не открывается. */
    public static AppContext create() {
        DatabaseManager databaseManager = DatabaseManager.fromConfig();
        Clock clock = Clock.systemDefaultZone();

        ClientRepository clientRepository = new JdbcClientRepository(databaseManager);
        CarRepository carRepository = new JdbcCarRepository(databaseManager);
        TestDriveRequestRepository requestRepository = new JdbcTestDriveRequestRepository(databaseManager);

        return new AppContext(
                databaseManager,
                new ClientService(clientRepository, requestRepository, clock),
                new CarService(carRepository, requestRepository, clock),
                new TestDriveRequestService(requestRepository, clientRepository, carRepository, clock),
                new StatisticsService(clientRepository, carRepository, requestRepository, clock),
                new ExportService(clock));
    }
}
