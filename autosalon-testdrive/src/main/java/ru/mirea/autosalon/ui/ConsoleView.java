package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.model.BaseEntity;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatistics;
import ru.mirea.autosalon.repository.TableData;
import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.util.Formats;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Вывод клиентов, автомобилей, заявок, статистики и таблиц БД в консоль.
 */
public final class ConsoleView {

    private static final int COMMENT_WIDTH = 24;
    private static final int INDICATOR_WIDTH = 58;

    private ConsoleView() {
    }

    // ------------------------------------------------------------------ Клиенты

    public static void printClients(List<Client> clients) {
        if (clients.isEmpty()) {
            System.out.println("Клиенты не найдены.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (Client client : clients) {
            rows.add(row(String.valueOf(client.getId()), client.getFullName(), client.getPhone(),
                    Formats.orDash(client.getEmail()), Formats.date(client.getBirthDate()),
                    client.getLicenseNumber()));
        }
        TablePrinter.print(List.of("ID", "ФИО", "Телефон", "Email", "Дата рожд.", "Номер ВУ"), rows);
        System.out.println("Всего клиентов: " + clients.size());
    }

    public static void printClientDetails(Client client) {
        int age = client.getAge(LocalDate.now());
        System.out.println("Клиент ID " + client.getId());
        printField("ФИО", client.getFullName());
        printField("Телефон", client.getPhone());
        printField("Email", Formats.orDash(client.getEmail()));
        printField("Дата рождения", Formats.date(client.getBirthDate()) + " (" + age + " "
                + Formats.plural(age, "год", "года", "лет") + ")");
        printField("Номер ВУ", client.getLicenseNumber());
        printField("Зарегистрирован", Formats.dateTime(client.getRegisteredAt()));
    }

    // ------------------------------------------------------------------ Автомобили

    public static void printCars(List<Car> cars) {
        if (cars.isEmpty()) {
            System.out.println("Автомобили не найдены.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (Car car : cars) {
            rows.add(row(String.valueOf(car.getId()), car.getBrand(), car.getModel(),
                    String.valueOf(car.getManufactureYear()), car.getColor(), Formats.money(car.getPrice()),
                    car.getVin(), car.getStatus().getDisplayName()));
        }
        TablePrinter.print(List.of("ID", "Марка", "Модель", "Год", "Цвет", "Цена, руб.", "VIN", "Статус"), rows);
        System.out.println("Всего автомобилей: " + cars.size());
    }

    public static void printCarDetails(Car car) {
        System.out.println("Автомобиль ID " + car.getId());
        printField("Марка и модель", car.getTitle());
        printField("Год выпуска", String.valueOf(car.getManufactureYear()));
        printField("VIN", car.getVin());
        printField("Цвет", car.getColor());
        printField("Цена", Formats.money(car.getPrice()) + " руб.");
        printField("Статус", car.getStatus().getDisplayName());
    }

    // ------------------------------------------------------------------ Заявки на тест-драйв

    public static void printRequests(List<TestDriveRequest> requests) {
        if (requests.isEmpty()) {
            System.out.println("Заявки не найдены.");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        for (TestDriveRequest request : requests) {
            rows.add(row(String.valueOf(request.getId()), request.getClient().getShortName(),
                    request.getCar().getTitle(),
                    Formats.dateTime(request.getScheduledAt()) + "-" + Formats.time(request.getEndAt()),
                    request.getStatus().getDisplayName(),
                    TablePrinter.truncate(Formats.orDash(request.getComment()), COMMENT_WIDTH)));
        }
        TablePrinter.print(List.of("ID", "Клиент", "Автомобиль", "Дата и время", "Статус", "Комментарий"), rows);
        System.out.println("Найдено заявок: " + requests.size());
    }

    public static void printRequestDetails(TestDriveRequest request) {
        Client client = request.getClient();
        Car car = request.getCar();
        System.out.println("Заявка на тест-драйв №" + request.getId());
        printField("Клиент", client.getFullName() + " (ID " + client.getId() + "), тел. " + client.getPhone());
        printField("Автомобиль", car.getTitle() + " " + car.getManufactureYear() + ", " + car.getColor()
                + ", VIN " + car.getVin() + " (ID " + car.getId() + ")");
        printField("Тест-драйв", Formats.dateTime(request.getScheduledAt()) + " - "
                + Formats.time(request.getEndAt()) + " (" + request.getDurationMinutes() + " мин.)");
        printField("Статус", request.getStatus().getDisplayName());
        printField("Комментарий", Formats.orDash(request.getComment()));
        printField("Создана", Formats.dateTime(request.getCreatedAt()));
        printField("Изменена", Formats.dateTime(request.getUpdatedAt()));
        printField("Смена статуса", TestDriveRequestService.describeAllowedTransitions(request.getStatus()));
    }

    // ------------------------------------------------------------------ Общее

    /**
     * Короткий список для выбора: «[ID] описание». Метод принимает любые сущности (клиентов, автомобили),
     * а описание каждая из них формирует сама через getDisplayName() — это полиморфизм.
     */
    public static void printShortList(List<? extends BaseEntity> entities) {
        if (entities.isEmpty()) {
            System.out.println("  (список пуст)");
            return;
        }
        for (BaseEntity entity : entities) {
            System.out.println("  [" + entity.getId() + "] " + entity.getDisplayName());
        }
    }

    /** Ограничения выводятся списком: их определения длинные и не помещаются в столбец таблицы. */
    public static void printConstraints(TableData constraints) {
        System.out.println(constraints.getTitle() + ":");
        for (List<String> constraint : constraints.getRows()) {
            System.out.println("  " + constraint.get(0) + " [" + constraint.get(1) + "]: " + constraint.get(2));
        }
    }

    public static void printTableData(TableData table) {
        System.out.println(table.getTitle() + ":");
        if (table.getRows().isEmpty()) {
            System.out.println("  (нет строк)");
            return;
        }
        TablePrinter.print(table.getColumns(), table.getRows());
        System.out.println("Строк: " + table.getRows().size());
    }

    public static void printStatistics(TestDriveStatistics statistics) {
        System.out.println("==================== СТАТИСТИКА АВТОСАЛОНА ====================");
        System.out.println("Данные на " + Formats.dateTime(statistics.getGeneratedAt()));
        System.out.println();
        for (Map.Entry<String, Object> indicator : statistics.getIndicators().entrySet()) {
            System.out.println(withDots(indicator.getKey()) + " " + formatIndicator(indicator.getValue()));
        }
        System.out.println();
        System.out.println("Заявки по маркам автомобилей:");
        for (Map.Entry<String, Long> brand : statistics.getRequestsByBrand().entrySet()) {
            System.out.printf("  %-10s %3d  %s%n", brand.getKey(), brand.getValue(),
                    "#".repeat(brand.getValue().intValue()));
        }
    }

    private static String formatIndicator(Object value) {
        return value instanceof Double number ? Formats.decimal(number) : String.valueOf(value);
    }

    private static String withDots(String name) {
        return name + " " + ".".repeat(Math.max(2, INDICATOR_WIDTH - name.length()));
    }

    private static void printField(String name, String value) {
        System.out.printf("  %-16s %s%n", name + ":", value);
    }

    /** Arrays.asList допускает null, в отличие от List.of. */
    private static List<String> row(String... cells) {
        return Arrays.asList(cells);
    }
}
