package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.service.TestDriveSortOption;

/**
 * Меню «Сортировка заявок». Пункты создаются автоматически по константам enum TestDriveSortOption.
 */
public class SortMenu extends AbstractMenu {

    private final TestDriveRequestService requestService;

    public SortMenu(ConsoleInput input, TestDriveRequestService requestService) {
        super("СОРТИРОВКА ЗАЯВОК", input);
        this.requestService = requestService;
        for (TestDriveSortOption option : TestDriveSortOption.values()) {
            addItem(option.getDisplayName(), () -> sort(option));
        }
    }

    private void sort(TestDriveSortOption option) {
        int order = input.readIntInRange("Порядок: 1 - по возрастанию, 2 - по убыванию", 1, 2);
        System.out.println("Сортировка: " + option.getDisplayName() + (order == 1 ? ", по возрастанию" : ", по убыванию"));
        ConsoleView.printRequests(requestService.getAllSorted(option, order == 1));
    }
}
