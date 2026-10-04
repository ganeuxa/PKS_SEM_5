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
import ru.mirea.autosalon.repository.memory.InMemoryClientRepository;
import ru.mirea.autosalon.repository.memory.InMemoryTestDriveRequestRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    private InMemoryClientRepository clientRepository;
    private InMemoryTestDriveRequestRepository requestRepository;
    private ClientService service;

    @BeforeEach
    void setUp() {
        clientRepository = new InMemoryClientRepository();
        requestRepository = new InMemoryTestDriveRequestRepository();
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ClientService(clientRepository, requestRepository, clock);
    }

    private static Client client(String fullName, String phone, LocalDate birthDate, String license) {
        return new Client(fullName, phone, null, birthDate, license);
    }

    @Test
    void createNormalizesInput() {
        Client created = service.create(new Client("  Иванов   Иван Иванович ", "8 (916) 123-45-67",
                "Ivan@Mail.RU", LocalDate.of(1990, 1, 1), "77 12 345678"));

        assertEquals("Иванов Иван Иванович", created.getFullName());
        assertEquals("+79161234567", created.getPhone());
        assertEquals("ivan@mail.ru", created.getEmail());
        assertEquals("7712345678", created.getLicenseNumber());
    }

    @Test
    void nameIsRequired() {
        assertThrows(ValidationException.class,
                () -> service.create(client("  ", "+79161234567", LocalDate.of(1990, 1, 1), "7712345678")));
    }

    @Test
    void phoneMustHaveRussianFormat() {
        assertThrows(ValidationException.class,
                () -> service.create(client("Иванов Иван", "12345", LocalDate.of(1990, 1, 1), "7712345678")));
    }

    @Test
    void clientMustBeAdult() {
        LocalDate seventeenYearsAgo = TODAY.minusYears(17);
        assertThrows(BusinessException.class,
                () -> service.create(client("Иванов Иван", "+79161234567", seventeenYearsAgo, "7712345678")));
    }

    @Test
    void phoneMustBeUnique() {
        service.create(client("Иванов Иван", "+79161234567", LocalDate.of(1990, 1, 1), "7712345678"));
        assertThrows(BusinessException.class,
                () -> service.create(client("Петров Пётр", "+79161234567", LocalDate.of(1991, 2, 2), "7712345679")));
    }

    @Test
    void clientWithRequestsCannotBeDeleted() {
        Client client = service.create(client("Иванов Иван", "+79161234567", LocalDate.of(1990, 1, 1), "7712345678"));
        Car car = new Car(1L, "Toyota", "Camry", 2024, "XW7BF4FK10S123451", "Белый", new BigDecimal("3850000"),
                CarStatus.AVAILABLE);
        requestRepository.save(new TestDriveRequest(client, car, TODAY.plusDays(1).atTime(12, 0), 60, null));

        assertThrows(BusinessException.class, () -> service.delete(client.getId()));
    }

    @Test
    void unknownClientIsNotFound() {
        assertThrows(EntityNotFoundException.class, () -> service.getById(42));
    }
}
