package ru.mirea.autosalon.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.autosalon.exception.BusinessException;
import ru.mirea.autosalon.exception.EntityNotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestDriveRequestServiceTest {

    /** «Сейчас» для всех тестов: понедельник 05.10.2026, 10:00. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private InMemoryClientRepository clientRepository;
    private InMemoryCarRepository carRepository;
    private TestDriveRequestService service;
    private Client client;
    private Car car;

    @BeforeEach
    void setUp() {
        clientRepository = new InMemoryClientRepository();
        carRepository = new InMemoryCarRepository();
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new TestDriveRequestService(new InMemoryTestDriveRequestRepository(), clientRepository,
                carRepository, clock);
        client = clientRepository.save(new Client("Иванов Иван Иванович", "+79160000001", null,
                LocalDate.of(1990, 1, 1), "7700000001"));
        car = carRepository.save(new Car("Toyota", "Camry", 2024, "XW7BF4FK10S123451", "Белый",
                new BigDecimal("3850000"), CarStatus.AVAILABLE));
    }

    private static LocalDateTime tomorrowAt(int hour, int minute) {
        return NOW.toLocalDate().plusDays(1).atTime(hour, minute);
    }

    private Client anotherClient() {
        return clientRepository.save(new Client("Петров Пётр Петрович", "+79160000002", null,
                LocalDate.of(1985, 5, 5), "7700000002"));
    }

    @Test
    void newRequestGetsStatusNew() {
        TestDriveRequest request = service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 60,
                "  Сравнить комплектации ");

        assertNotNull(request.getId());
        assertEquals(TestDriveStatus.NEW, request.getStatus());
        assertEquals("Сравнить комплектации", request.getComment());
    }

    @Test
    void clientAndCarMustExist() {
        assertThrows(EntityNotFoundException.class,
                () -> service.create(999, car.getId(), tomorrowAt(11, 0), 60, null));
        assertThrows(EntityNotFoundException.class,
                () -> service.create(client.getId(), 999, tomorrowAt(11, 0), 60, null));
    }

    @Test
    void carMustBeAvailable() {
        car.setStatus(CarStatus.SOLD);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 60, null));
        assertTrue(error.getMessage().contains("Продан"));
    }

    @Test
    void testDriveCannotBeInThePast() {
        assertThrows(BusinessException.class,
                () -> service.create(client.getId(), car.getId(), NOW.minusHours(1), 60, null));
    }

    @Test
    void testDriveMustFitWorkingHours() {
        assertThrows(BusinessException.class,
                () -> service.create(client.getId(), car.getId(), tomorrowAt(20, 30), 60, null));
        assertThrows(BusinessException.class,
                () -> service.create(client.getId(), car.getId(), tomorrowAt(8, 30), 30, null));
        assertDoesNotThrow(() -> service.create(client.getId(), car.getId(), tomorrowAt(20, 0), 60, null));
    }

    @Test
    void durationMustBeFrom15To120Minutes() {
        assertThrows(ValidationException.class,
                () -> service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 10, null));
        assertThrows(ValidationException.class,
                () -> service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 150, null));
    }

    @Test
    void carCannotBeBookedTwiceAtTheSameTime() {
        service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 60, null);
        Client other = anotherClient();

        assertThrows(BusinessException.class,
                () -> service.create(other.getId(), car.getId(), tomorrowAt(11, 30), 30, null));
        assertDoesNotThrow(() -> service.create(other.getId(), car.getId(), tomorrowAt(12, 0), 30, null));
    }

    @Test
    void clientCanHaveAtMostThreeActiveRequests() {
        service.create(client.getId(), car.getId(), tomorrowAt(10, 0), 30, null);
        service.create(client.getId(), car.getId(), tomorrowAt(12, 0), 30, null);
        service.create(client.getId(), car.getId(), tomorrowAt(14, 0), 30, null);

        assertThrows(BusinessException.class,
                () -> service.create(client.getId(), car.getId(), tomorrowAt(16, 0), 30, null));
    }

    @Test
    void statusChangesFollowLifecycle() {
        TestDriveRequest request = service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 60, null);

        assertThrows(BusinessException.class, () -> service.changeStatus(request.getId(), TestDriveStatus.COMPLETED));
        assertEquals(TestDriveStatus.CONFIRMED,
                service.changeStatus(request.getId(), TestDriveStatus.CONFIRMED).getStatus());
    }

    @Test
    void testDriveCannotBeCompletedBeforeItStarts() {
        TestDriveRequest request = service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 60, null);
        service.changeStatus(request.getId(), TestDriveStatus.CONFIRMED);

        assertThrows(BusinessException.class, () -> service.changeStatus(request.getId(), TestDriveStatus.COMPLETED));
    }

    @Test
    void closedRequestCannotBeEdited() {
        TestDriveRequest request = service.create(client.getId(), car.getId(), tomorrowAt(11, 0), 60, null);
        service.changeStatus(request.getId(), TestDriveStatus.CANCELLED);

        assertThrows(BusinessException.class,
                () -> service.update(request.getId(), car.getId(), tomorrowAt(15, 0), 30, null));
    }

    @Test
    void filtersAndSortingUseAllRequests() {
        service.create(client.getId(), car.getId(), tomorrowAt(15, 0), 30, null);
        service.create(anotherClient().getId(), car.getId(), tomorrowAt(10, 0), 30, null);

        List<TestDriveRequest> byTime = service.getAllSorted(TestDriveSortOption.BY_SCHEDULED_AT, true);
        assertEquals(tomorrowAt(10, 0), byTime.get(0).getScheduledAt());
        assertEquals(2, service.filterByStatus(TestDriveStatus.NEW).size());
        assertEquals(1, service.filterByClient(client.getId()).size());
        assertThrows(ValidationException.class,
                () -> service.filterByPeriod(NOW.toLocalDate().plusDays(5), NOW.toLocalDate()));
    }
}
