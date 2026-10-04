package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.service.StatisticsService;

/**
 * Главное меню информационной системы автосалона.
 */
public class MainMenu extends AbstractMenu {

    private static final String LINE = "=".repeat(52);

    public MainMenu(ConsoleInput input, ClientMenu clientMenu, CarMenu carMenu, TestDriveRequestMenu requestMenu,
                    SearchMenu searchMenu, FilterMenu filterMenu, SortMenu sortMenu,
                    StatisticsService statisticsService, ExportMenu exportMenu, DatabaseTablesMenu tablesMenu) {
        super("ГЛАВНОЕ МЕНЮ", input);
        addItem("Клиенты", clientMenu::run);
        addItem("Автомобили", carMenu::run);
        addItem("Заявки на тест-драйв", requestMenu::run);
        addItem("Поиск заявок", searchMenu::run);
        addItem("Фильтрация заявок", filterMenu::run);
        addItem("Сортировка заявок", sortMenu::run);
        addItem("Статистика", () -> ConsoleView.printStatistics(statisticsService.collect()));
        addItem("Экспорт данных (Excel, CSV)", exportMenu::run);
        addItem("Вывести таблицы базы данных", tablesMenu::run);
    }

    @Override
    protected void printHeader() {
        System.out.println();
        System.out.println(LINE);
        System.out.println("   АВТОСАЛОН: ЗАЯВКИ НА ТЕСТ-ДРАЙВ АВТОМОБИЛЕЙ");
        System.out.println(LINE);
    }

    @Override
    protected String getExitTitle() {
        return "Выход";
    }
}
