package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.service.CarService;
import ru.mirea.autosalon.service.ClientService;
import ru.mirea.autosalon.service.TestDriveRequestService;

import java.time.LocalDate;
import java.util.List;

/**
 * Меню «Фильтрация заявок»: отбор заявок по заданному условию (Stream API в сервисе).
 */
public class FilterMenu extends AbstractMenu {

    private final TestDriveRequestService requestService;
    private final ClientService clientService;
    private final CarService carService;

    public FilterMenu(ConsoleInput input, TestDriveRequestService requestService, ClientService clientService,
                      CarService carService) {
        super("ФИЛЬТРАЦИЯ ЗАЯВОК", input);
        this.requestService = requestService;
        this.clientService = clientService;
        this.carService = carService;
        addItem("По статусу заявки", this::filterByStatus);
        addItem("По клиенту", this::filterByClient);
        addItem("По автомобилю", this::filterByCar);
        addItem("По периоду (диапазону дат тест-драйва)", this::filterByPeriod);
        addItem("Только предстоящие тест-драйвы", this::filterUpcoming);
    }

    private void filterByStatus() {
        TestDriveStatus status = input.choose("Статус", List.of(TestDriveStatus.values()),
                TestDriveStatus::getDisplayName);
        ConsoleView.printRequests(requestService.filterByStatus(status));
    }

    private void filterByClient() {
        System.out.println("Клиенты:");
        ConsoleView.printShortList(clientService.getAll());
        long clientId = input.readId("ID клиента");
        ConsoleView.printRequests(requestService.filterByClient(clientId));
    }

    private void filterByCar() {
        System.out.println("Автомобили:");
        ConsoleView.printShortList(carService.getAll());
        long carId = input.readId("ID автомобиля");
        ConsoleView.printRequests(requestService.filterByCar(carId));
    }

    private void filterByPeriod() {
        LocalDate from = input.readDate("С даты");
        LocalDate to = input.readDate("По дату (включительно)");
        ConsoleView.printRequests(requestService.filterByPeriod(from, to));
    }

    private void filterUpcoming() {
        ConsoleView.printRequests(requestService.filterUpcoming());
    }
}
