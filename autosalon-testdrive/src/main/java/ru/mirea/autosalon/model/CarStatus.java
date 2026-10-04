package ru.mirea.autosalon.model;

/**
 * Состояние автомобиля в автосалоне. Хранится в столбце {@code cars.status}
 * как имя константы (AVAILABLE, IN_SERVICE, SOLD).
 */
public enum CarStatus {

    AVAILABLE("В наличии"),
    IN_SERVICE("На обслуживании"),
    SOLD("Продан");

    private final String displayName;

    CarStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Записать на тест-драйв можно только автомобиль, который есть в наличии. */
    public boolean isAvailableForTestDrive() {
        return this == AVAILABLE;
    }
}
