package ru.mirea.autosalon.service;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
import ru.mirea.autosalon.util.DataExporter;
import ru.mirea.autosalon.util.ExportData;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Экспорт данных из PostgreSQL в файл. Формат файла определяет переданный {@link DataExporter}:
 * сервису не важно, Excel это или CSV (полиморфизм через интерфейс).
 */
public class ExportService {

    private final ClientRepository clientRepository;
    private final CarRepository carRepository;
    private final TestDriveRequestRepository requestRepository;
    private final StatisticsService statisticsService;
    private final Clock clock;

    public ExportService(ClientRepository clientRepository, CarRepository carRepository,
                         TestDriveRequestRepository requestRepository, StatisticsService statisticsService,
                         Clock clock) {
        this.clientRepository = clientRepository;
        this.carRepository = carRepository;
        this.requestRepository = requestRepository;
        this.statisticsService = statisticsService;
        this.clock = clock;
    }

    /** Читает все данные из БД и выгружает их в папку directory. Возвращает созданные файлы. */
    public List<Path> export(DataExporter exporter, Path directory) {
        List<Client> clients = clientRepository.findAll();
        List<Car> cars = carRepository.findAll();
        List<TestDriveRequest> requests = requestRepository.findAll();
        ExportData data = new ExportData(clients, cars, requests,
                statisticsService.calculate(clients, cars, requests), LocalDateTime.now(clock));
        return exporter.export(data, directory);
    }
}
