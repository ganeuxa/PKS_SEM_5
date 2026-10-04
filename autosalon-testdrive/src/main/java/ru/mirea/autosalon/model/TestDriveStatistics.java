package ru.mirea.autosalon.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Итоговая статистика автосалона на момент {@link #getGeneratedAt()}.
 * Объект неизменяемый: значения рассчитывает StatisticsService, а консоль и экспорт только читают их.
 */
public class TestDriveStatistics {

    private final LocalDateTime generatedAt;
    private final Map<String, Object> indicators;
    private final Map<TestDriveStatus, Long> requestsByStatus;
    private final Map<String, Long> requestsByBrand;

    /**
     * @param indicators       показатели «название -> значение» в порядке вывода
     * @param requestsByStatus количество заявок по каждому статусу
     * @param requestsByBrand  количество заявок по маркам автомобилей (по убыванию)
     */
    public TestDriveStatistics(LocalDateTime generatedAt, Map<String, Object> indicators,
                               Map<TestDriveStatus, Long> requestsByStatus, Map<String, Long> requestsByBrand) {
        this.generatedAt = generatedAt;
        this.indicators = Collections.unmodifiableMap(new LinkedHashMap<>(indicators));
        this.requestsByStatus = Collections.unmodifiableMap(new EnumMap<>(requestsByStatus));
        this.requestsByBrand = Collections.unmodifiableMap(new LinkedHashMap<>(requestsByBrand));
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public Map<String, Object> getIndicators() {
        return indicators;
    }

    public Map<TestDriveStatus, Long> getRequestsByStatus() {
        return requestsByStatus;
    }

    public Map<String, Long> getRequestsByBrand() {
        return requestsByBrand;
    }
}
