package ru.mirea.autosalon.service;

import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.util.Formats;

import java.util.Comparator;

/**
 * Способы сортировки заявок. Каждая константа хранит свой Comparator,
 * поэтому сервис сортирует одинаково для любого выбранного варианта.
 */
public enum TestDriveSortOption {

    BY_SCHEDULED_AT("По дате и времени тест-драйва",
            Comparator.comparing(TestDriveRequest::getScheduledAt)),
    BY_CREATED_AT("По дате создания заявки",
            Comparator.comparing(TestDriveRequest::getCreatedAt)),
    BY_CLIENT_NAME("По ФИО клиента",
            Comparator.comparing(request -> request.getClient().getFullName(), Formats.russianOrder())),
    BY_CAR("По марке и модели автомобиля",
            Comparator.comparing(request -> request.getCar().getTitle(), Formats.russianOrder())),
    BY_STATUS("По статусу заявки (в порядке жизненного цикла)",
            Comparator.comparing(TestDriveRequest::getStatus)),
    BY_DURATION("По длительности тест-драйва",
            Comparator.comparingInt(TestDriveRequest::getDurationMinutes));

    private final String displayName;
    private final Comparator<TestDriveRequest> comparator;

    TestDriveSortOption(String displayName, Comparator<TestDriveRequest> comparator) {
        this.displayName = displayName;
        // При равных значениях упорядочиваем по ID, чтобы результат сортировки был стабильным
        this.comparator = comparator.thenComparing(TestDriveRequest::getId);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Comparator<TestDriveRequest> getComparator() {
        return comparator;
    }
}
