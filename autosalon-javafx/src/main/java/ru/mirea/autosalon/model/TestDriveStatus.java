package ru.mirea.autosalon.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Статус заявки на тест-драйв. Хранится в столбце {@code test_drive_requests.status}.
 * <p>
 * Жизненный цикл заявки:
 * <pre>
 *   NEW ──> CONFIRMED ──> COMPLETED
 *    │          ├───────> NO_SHOW
 *    └──────────┴───────> CANCELLED
 * </pre>
 * Из статусов COMPLETED, NO_SHOW и CANCELLED перейти никуда нельзя: заявка закрыта.
 */
public enum TestDriveStatus {

    NEW("Новая"),
    CONFIRMED("Подтверждена"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена"),
    NO_SHOW("Неявка клиента");

    private final String displayName;

    TestDriveStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Активная заявка ещё ожидает тест-драйва: она занимает автомобиль и время клиента. */
    public boolean isActive() {
        return this == NEW || this == CONFIRMED;
    }

    /** Закрытая заявка: тест-драйв состоялся, отменён или клиент не пришёл. */
    public boolean isFinal() {
        return !isActive();
    }

    /** Статусы, в которые разрешено перевести заявку из текущего статуса. */
    public Set<TestDriveStatus> getAllowedTransitions() {
        return switch (this) {
            case NEW -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(COMPLETED, NO_SHOW, CANCELLED);
            case COMPLETED, CANCELLED, NO_SHOW -> EnumSet.noneOf(TestDriveStatus.class);
        };
    }

    public boolean canTransitionTo(TestDriveStatus target) {
        return getAllowedTransitions().contains(target);
    }
}
