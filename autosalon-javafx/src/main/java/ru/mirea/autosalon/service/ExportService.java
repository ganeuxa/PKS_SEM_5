package ru.mirea.autosalon.service;

import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.util.CsvWriter;
import ru.mirea.autosalon.util.Formats;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * Экспорт в CSV того набора данных, который сейчас показан в таблице:
 * с учётом поиска, фильтров и сортировки. Путь к файлу выбирает пользователь.
 */
public class ExportService {

    private static final List<String> REQUEST_HEADERS = List.of("№ заявки", "Клиент", "Телефон клиента",
            "Автомобиль", "VIN", "Начало тест-драйва", "Окончание", "Длительность, мин", "Статус", "Комментарий",
            "Создана", "Изменена");

    private static final List<String> CLIENT_HEADERS = List.of("ID", "ФИО", "Телефон", "Email", "Дата рождения",
            "Возраст, лет", "Номер ВУ", "Дата регистрации");

    private final Clock clock;

    public ExportService(Clock clock) {
        this.clock = clock;
    }

    public void exportRequests(List<TestDriveRequest> requests, Path file) {
        List<List<String>> rows = requests.stream()
                .map(request -> Arrays.asList(
                        String.valueOf(request.getId()),
                        request.getClient().getFullName(),
                        request.getClient().getPhone(),
                        request.getCar().getTitle() + " " + request.getCar().getManufactureYear(),
                        request.getCar().getVin(),
                        Formats.dateTime(request.getScheduledAt()),
                        Formats.dateTime(request.getEndAt()),
                        String.valueOf(request.getDurationMinutes()),
                        request.getStatus().getDisplayName(),
                        request.getComment(),
                        Formats.dateTime(request.getCreatedAt()),
                        Formats.dateTime(request.getUpdatedAt())))
                .toList();
        CsvWriter.write(file, REQUEST_HEADERS, rows);
    }

    public void exportClients(List<Client> clients, Path file) {
        LocalDate today = LocalDate.now(clock);
        List<List<String>> rows = clients.stream()
                .map(client -> Arrays.asList(
                        String.valueOf(client.getId()),
                        client.getFullName(),
                        client.getPhone(),
                        client.getEmail(),
                        Formats.date(client.getBirthDate()),
                        String.valueOf(client.getAge(today)),
                        client.getLicenseNumber(),
                        Formats.dateTime(client.getRegisteredAt())))
                .toList();
        CsvWriter.write(file, CLIENT_HEADERS, rows);
    }
}
