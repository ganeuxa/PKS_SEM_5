package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.exception.BusinessException;
import ru.mirea.autosalon.exception.DataAccessException;
import ru.mirea.autosalon.exception.DatabaseConnectionException;
import ru.mirea.autosalon.exception.EntityNotFoundException;
import ru.mirea.autosalon.exception.ExportException;
import ru.mirea.autosalon.exception.InputClosedException;
import ru.mirea.autosalon.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Базовый класс всех меню. Реализует общий цикл: показать пункты -> прочитать выбор ->
 * выполнить действие -> вернуться в это же меню. Пункт 0 закрывает меню.
 * <p>
 * Подклассы добавляют свои пункты через {@link #addItem} и могут переопределить заголовок
 * и подпись пункта 0 (полиморфизм: так делает {@link MainMenu}).
 * Здесь же централизованно обрабатываются все ошибки, поэтому программа не завершается аварийно.
 */
public abstract class AbstractMenu {

    protected final ConsoleInput input;
    private final String title;
    private final List<MenuItem> items = new ArrayList<>();

    protected AbstractMenu(String title, ConsoleInput input) {
        this.title = title;
        this.input = input;
    }

    protected final void addItem(String itemTitle, Runnable action) {
        items.add(new MenuItem(itemTitle, action));
    }

    /** Подпись пункта 0. */
    protected String getExitTitle() {
        return "Назад";
    }

    protected void printHeader() {
        System.out.println();
        System.out.println("---------- " + title + " ----------");
    }

    public void run() {
        while (true) {
            printHeader();
            for (int i = 0; i < items.size(); i++) {
                System.out.println((i + 1) + ". " + items.get(i).getTitle());
            }
            System.out.println("0. " + getExitTitle());
            int choice = input.readMenuChoice(items.size());
            if (choice == 0) {
                return;
            }
            System.out.println();
            execute(items.get(choice - 1));
        }
    }

    /** Выполняет действие пункта меню и превращает любое исключение в понятное сообщение. */
    private void execute(MenuItem item) {
        try {
            item.getAction().run();
        } catch (InputClosedException e) {
            throw e;
        } catch (ValidationException e) {
            printError("Ошибка: некорректные данные. " + e.getMessage());
        } catch (BusinessException e) {
            printError("Нарушение бизнес-правила: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            printError("Ошибка: " + e.getMessage() + ".");
        } catch (DatabaseConnectionException e) {
            printError("Ошибка подключения к базе данных: " + e.getMessage());
        } catch (DataAccessException e) {
            printError("Ошибка выполнения SQL-запроса: " + e.getMessage());
        } catch (ExportException e) {
            printError("Ошибка экспорта: " + e.getMessage());
        } catch (RuntimeException e) {
            printError("Непредвиденная ошибка: " + e);
        }
    }

    private static void printError(String message) {
        System.out.println(message);
    }
}
