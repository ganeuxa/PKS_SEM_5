package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.service.TestDriveRequestService;

/**
 * Меню «Поиск заявок»: поиск по введённому тексту или дате (выполняется SQL-запросами в репозитории).
 */
public class SearchMenu extends AbstractMenu {

    private final TestDriveRequestService requestService;

    public SearchMenu(ConsoleInput input, TestDriveRequestService requestService) {
        super("ПОИСК ЗАЯВОК", input);
        this.requestService = requestService;
        addItem("По ФИО клиента (или его части)", this::searchByClientName);
        addItem("По телефону клиента (или его части)", this::searchByClientPhone);
        addItem("По марке или модели автомобиля", this::searchByCar);
        addItem("По дате тест-драйва", this::searchByDate);
    }

    private void searchByClientName() {
        String text = input.readLine("Введите ФИО или его часть");
        ConsoleView.printRequests(requestService.searchByClientName(text));
    }

    private void searchByClientPhone() {
        String text = input.readLine("Введите номер телефона или его часть");
        ConsoleView.printRequests(requestService.searchByClientPhone(text));
    }

    private void searchByCar() {
        String text = input.readLine("Введите марку или модель (например, Toyota или Camry)");
        ConsoleView.printRequests(requestService.searchByCar(text));
    }

    private void searchByDate() {
        ConsoleView.printRequests(requestService.searchByDate(input.readDate("Дата тест-драйва")));
    }
}
