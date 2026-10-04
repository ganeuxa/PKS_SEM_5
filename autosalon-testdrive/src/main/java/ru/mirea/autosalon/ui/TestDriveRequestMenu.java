package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.service.CarService;
import ru.mirea.autosalon.service.ClientService;
import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.util.Formats;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Меню «Заявки на тест-драйв»: CRUD-операции и смена статуса заявки.
 */
public class TestDriveRequestMenu extends AbstractMenu {

    private static final List<TestDriveStatus> STATUSES = List.of(TestDriveStatus.values());

    private final TestDriveRequestService requestService;
    private final ClientService clientService;
    private final CarService carService;

    public TestDriveRequestMenu(ConsoleInput input, TestDriveRequestService requestService,
                                ClientService clientService, CarService carService) {
        super("ЗАЯВКИ НА ТЕСТ-ДРАЙВ", input);
        this.requestService = requestService;
        this.clientService = clientService;
        this.carService = carService;
        addItem("Показать все заявки", this::showAll);
        addItem("Найти заявку по ID", this::showById);
        addItem("Создать заявку на тест-драйв", this::create);
        addItem("Изменить заявку (автомобиль, время, комментарий)", this::update);
        addItem("Изменить статус заявки", this::changeStatus);
        addItem("Удалить заявку", this::delete);
    }

    private void showAll() {
        ConsoleView.printRequests(requestService.getAll());
    }

    private void showById() {
        long id = input.readId("Введите ID заявки");
        ConsoleView.printRequestDetails(requestService.getById(id));
    }

    private void create() {
        System.out.println("Клиенты:");
        ConsoleView.printShortList(clientService.getAll());
        long clientId = input.readId("ID клиента");

        System.out.println("Автомобили, доступные для тест-драйва:");
        ConsoleView.printShortList(carService.getAvailableForTestDrive());
        long carId = input.readId("ID автомобиля");

        printScheduleRules();
        LocalDateTime scheduledAt = input.readDateTime("Дата и время тест-драйва");
        int duration = input.readInt("Длительность, мин");
        String comment = input.readOptionalLine("Комментарий (Enter - без комментария)");

        TestDriveRequest created = requestService.create(clientId, carId, scheduledAt, duration, comment);
        System.out.println("Заявка создана.");
        ConsoleView.printRequestDetails(created);
    }

    private void update() {
        long id = input.readId("Введите ID заявки");
        TestDriveRequest request = requestService.getById(id);
        ConsoleView.printRequestDetails(request);
        requestService.checkEditable(request);

        System.out.println("Автомобили, доступные для тест-драйва:");
        ConsoleView.printShortList(carService.getAvailableForTestDrive());
        printScheduleRules();
        System.out.println("Введите новые значения (Enter - оставить текущее):");
        long carId = input.readIdOrKeep("ID автомобиля", request.getCar().getId());
        LocalDateTime scheduledAt = input.readDateTimeOrKeep("Дата и время", request.getScheduledAt());
        int duration = input.readIntOrKeep("Длительность, мин", request.getDurationMinutes());
        String comment = input.readOptionalLineOrKeep("Комментарий", request.getComment());

        TestDriveRequest updated = requestService.update(id, carId, scheduledAt, duration, comment);
        System.out.println("Заявка изменена.");
        ConsoleView.printRequestDetails(updated);
    }

    private void changeStatus() {
        long id = input.readId("Введите ID заявки");
        TestDriveRequest request = requestService.getById(id);
        TestDriveStatus currentStatus = request.getStatus();
        System.out.println(request.getDisplayName());
        System.out.println("Текущий статус: " + currentStatus.getDisplayName() + ". "
                + TestDriveRequestService.describeAllowedTransitions(currentStatus));

        TestDriveStatus newStatus = input.choose("Новый статус", STATUSES,
                status -> status.getDisplayName() + (currentStatus.canTransitionTo(status) ? "  (допустимо)" : ""));
        TestDriveRequest updated = requestService.changeStatus(id, newStatus);
        System.out.println("Статус заявки №" + id + " изменён: \"" + currentStatus.getDisplayName() + "\" -> \""
                + updated.getStatus().getDisplayName() + "\".");
    }

    private void delete() {
        long id = input.readId("Введите ID заявки");
        TestDriveRequest request = requestService.getById(id);
        if (!input.confirm("Удалить заявку №" + id + " (" + request.getClient().getShortName() + ", "
                + request.getCar().getTitle() + ", " + Formats.dateTime(request.getScheduledAt()) + ")?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        requestService.delete(id);
        System.out.println("Заявка удалена.");
    }

    private void printScheduleRules() {
        System.out.println("Автосалон работает с " + TestDriveRequestService.OPENING_TIME + " до "
                + TestDriveRequestService.CLOSING_TIME + ". Длительность тест-драйва: от "
                + TestDriveRequestService.MIN_DURATION_MINUTES + " до " + TestDriveRequestService.MAX_DURATION_MINUTES
                + " минут.");
    }
}
