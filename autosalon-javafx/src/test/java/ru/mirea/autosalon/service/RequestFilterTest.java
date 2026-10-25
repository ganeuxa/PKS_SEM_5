package ru.mirea.autosalon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.autosalon.exception.ValidationException;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.CarStatus;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.repository.memory.InMemoryCarRepository;
import ru.mirea.autosalon.repository.memory.InMemoryClientRepository;
import ru.mirea.autosalon.repository.memory.InMemoryTestDriveRequestRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Поиск и фильтры вкладки «Заявки»: условие собирается в TestDriveRequestService.buildFilter. */
class RequestFilterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private TestDriveRequestService service;
    private List<TestDriveRequest> requests;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new TestDriveRequestService(new InMemoryTestDriveRequestRepository(), new InMemoryClientRepository(),
                new InMemoryCarRepository(), clock);

        Client ivanov = client(1L, "Иванов Алексей Сергеевич", "+79161234501");
        Client smirnova = client(2L, "Смирнова Екатерина Андреевна", "+79262345602");
        Car camry = car(1L, "Toyota", "Camry", "XW7BF4FK10S123451");
        Car k5 = car(2L, "Kia", "K5", "XWEG3417BP0123453");

        requests = List.of(
                request(1L, ivanov, camry, NOW.minusDays(3), TestDriveStatus.COMPLETED, "Просил расчёт кредита"),
                request(2L, smirnova, k5, NOW.plusDays(1), TestDriveStatus.NEW, null),
                request(3L, ivanov, k5, NOW.plusDays(2), TestDriveStatus.CONFIRMED, "Нужно детское кресло"),
                request(4L, smirnova, camry, NOW.plusDays(4), TestDriveStatus.CANCELLED, null));
    }

    private List<Long> idsMatching(RequestFilter filter) {
        return requests.stream().filter(service.buildFilter(filter)).map(TestDriveRequest::getId).toList();
    }

    @Test
    void emptyFilterShowsEverything() {
        assertThat(idsMatching(RequestFilter.empty())).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void searchLooksIntoSeveralFields() {
        assertThat(idsMatching(new RequestFilter("иванов", null, null, null, null, false))).containsExactly(1L, 3L);
        // В телефоне ищутся только цифры, поэтому пробелы и скобки в строке поиска не мешают
        assertThat(idsMatching(new RequestFilter("(926) 234", null, null, null, null, false)))
                .containsExactly(2L, 4L);
        assertThat(idsMatching(new RequestFilter("000", null, null, null, null, false))).isEmpty();
        assertThat(idsMatching(new RequestFilter("camry", null, null, null, null, false))).containsExactly(1L, 4L);
        assertThat(idsMatching(new RequestFilter("КРЕСЛО", null, null, null, null, false))).containsExactly(3L);
    }

    @Test
    void filtersAreCombinedWithAnd() {
        assertThat(idsMatching(new RequestFilter(null, TestDriveStatus.NEW, null, null, null, false)))
                .containsExactly(2L);
        assertThat(idsMatching(new RequestFilter(null, null, "Kia", null, null, false))).containsExactly(2L, 3L);
        assertThat(idsMatching(new RequestFilter("смирнова", null, "Toyota", null, null, false)))
                .containsExactly(4L);
    }

    @Test
    void periodAndUpcomingFilters() {
        LocalDate today = NOW.toLocalDate();
        assertThat(idsMatching(new RequestFilter(null, null, null, today.plusDays(1), today.plusDays(2), false)))
                .containsExactly(2L, 3L);
        // Отменённая заявка №4 в будущем, но она не активна, поэтому не считается предстоящей
        assertThat(idsMatching(new RequestFilter(null, null, null, null, null, true))).containsExactly(2L, 3L);
    }

    @Test
    void periodStartMustNotBeAfterEnd() {
        LocalDate today = NOW.toLocalDate();
        assertThatThrownBy(() -> service.buildFilter(
                new RequestFilter(null, null, null, today.plusDays(5), today, false)))
                .isInstanceOf(ValidationException.class);
    }

    private static Client client(long id, String name, String phone) {
        return new Client(id, name, phone, null, LocalDate.of(1990, 1, 1), "77000000" + id + "0", NOW);
    }

    private static Car car(long id, String brand, String model, String vin) {
        return new Car(id, brand, model, 2024, vin, "Белый", new BigDecimal("3000000"), CarStatus.AVAILABLE);
    }

    private static TestDriveRequest request(long id, Client client, Car car, LocalDateTime at, TestDriveStatus status,
                                            String comment) {
        return new TestDriveRequest(id, client, car, at, 60, status, comment, NOW.minusDays(5), NOW.minusDays(5));
    }
}
