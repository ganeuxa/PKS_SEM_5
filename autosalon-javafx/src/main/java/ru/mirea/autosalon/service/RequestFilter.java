package ru.mirea.autosalon.service;

import ru.mirea.autosalon.model.TestDriveStatus;

import java.time.LocalDate;

/**
 * Условия поиска и фильтрации заявок, которые пользователь задал на панели фильтров.
 * Пустое значение (null или пустая строка) означает «условие не задано».
 *
 * @param searchText   текст поиска: ищется в ФИО и телефоне клиента, марке, модели и VIN автомобиля, комментарии
 * @param status       статус заявки
 * @param brand        марка автомобиля
 * @param from         начало периода тест-драйва (включительно)
 * @param to           конец периода тест-драйва (включительно)
 * @param upcomingOnly только предстоящие тест-драйвы (активные заявки, время которых ещё не наступило)
 */
public record RequestFilter(String searchText, TestDriveStatus status, String brand, LocalDate from, LocalDate to,
                            boolean upcomingOnly) {

    /** Фильтр без условий: показываются все заявки. */
    public static RequestFilter empty() {
        return new RequestFilter(null, null, null, null, null, false);
    }
}
