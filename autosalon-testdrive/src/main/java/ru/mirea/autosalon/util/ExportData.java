package ru.mirea.autosalon.util;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatistics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Снимок данных автосалона для экспорта: клиенты, автомобили, заявки и статистика.
 */
public class ExportData {

    private final List<Client> clients;
    private final List<Car> cars;
    private final List<TestDriveRequest> requests;
    private final TestDriveStatistics statistics;
    private final LocalDateTime generatedAt;

    public ExportData(List<Client> clients, List<Car> cars, List<TestDriveRequest> requests,
                      TestDriveStatistics statistics, LocalDateTime generatedAt) {
        this.clients = List.copyOf(clients);
        this.cars = List.copyOf(cars);
        this.requests = List.copyOf(requests);
        this.statistics = statistics;
        this.generatedAt = generatedAt;
    }

    public List<Client> getClients() {
        return clients;
    }

    public List<Car> getCars() {
        return cars;
    }

    public List<TestDriveRequest> getRequests() {
        return requests;
    }

    public TestDriveStatistics getStatistics() {
        return statistics;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
