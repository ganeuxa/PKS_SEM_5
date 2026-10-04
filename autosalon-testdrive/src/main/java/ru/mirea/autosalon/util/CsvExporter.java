package ru.mirea.autosalon.util;

import ru.mirea.autosalon.exception.ExportException;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Экспорт в CSV: по одному файлу на таблицу (заявки, клиенты, автомобили).
 * Разделитель «;» и метка BOM в начале файла нужны, чтобы русский Excel сразу открыл файл правильно.
 */
public class CsvExporter implements DataExporter {

    private static final String SEPARATOR = ";";
    private static final char UTF8_BOM = '﻿';

    @Override
    public String getFormatName() {
        return "CSV (.csv)";
    }

    @Override
    public List<Path> export(ExportData data, Path directory) {
        String baseName = buildBaseFileName(data);
        try {
            Files.createDirectories(directory);
            List<Path> files = new ArrayList<>();
            files.add(write(directory.resolve(baseName + "_requests.csv"),
                    List.of("№ заявки", "Клиент", "Телефон клиента", "Автомобиль", "VIN", "Начало тест-драйва",
                            "Окончание", "Длительность, мин", "Статус", "Комментарий", "Создана", "Изменена"),
                    requestRows(data.getRequests())));
            files.add(write(directory.resolve(baseName + "_clients.csv"),
                    List.of("ID", "ФИО", "Телефон", "Email", "Дата рождения", "Номер ВУ", "Дата регистрации"),
                    clientRows(data.getClients())));
            files.add(write(directory.resolve(baseName + "_cars.csv"),
                    List.of("ID", "Марка", "Модель", "Год выпуска", "VIN", "Цвет", "Цена, руб.", "Статус"),
                    carRows(data.getCars())));
            return files;
        } catch (IOException e) {
            throw new ExportException("не удалось записать CSV-файл в папку " + directory.toAbsolutePath()
                    + " (" + e.getMessage() + ")", e);
        }
    }

    private List<List<String>> requestRows(List<TestDriveRequest> requests) {
        List<List<String>> rows = new ArrayList<>();
        for (TestDriveRequest request : requests) {
            rows.add(Arrays.asList(
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
                    Formats.dateTime(request.getUpdatedAt())));
        }
        return rows;
    }

    private List<List<String>> clientRows(List<Client> clients) {
        List<List<String>> rows = new ArrayList<>();
        for (Client client : clients) {
            rows.add(Arrays.asList(
                    String.valueOf(client.getId()),
                    client.getFullName(),
                    client.getPhone(),
                    client.getEmail(),
                    Formats.date(client.getBirthDate()),
                    client.getLicenseNumber(),
                    Formats.dateTime(client.getRegisteredAt())));
        }
        return rows;
    }

    private List<List<String>> carRows(List<Car> cars) {
        List<List<String>> rows = new ArrayList<>();
        for (Car car : cars) {
            rows.add(Arrays.asList(
                    String.valueOf(car.getId()),
                    car.getBrand(),
                    car.getModel(),
                    String.valueOf(car.getManufactureYear()),
                    car.getVin(),
                    car.getColor(),
                    car.getPrice().toPlainString(),
                    car.getStatus().getDisplayName()));
        }
        return rows;
    }

    private Path write(Path file, List<String> headers, List<List<String>> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(UTF8_BOM);
            writer.write(toCsvLine(headers));
            writer.newLine();
            for (List<String> row : rows) {
                writer.write(toCsvLine(row));
                writer.newLine();
            }
        }
        return file.toAbsolutePath();
    }

    private String toCsvLine(List<String> values) {
        return values.stream().map(CsvExporter::escape).collect(Collectors.joining(SEPARATOR));
    }

    /** Значение с разделителем, кавычками или переносом строки заключается в кавычки по правилам CSV. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
