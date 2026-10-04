package ru.mirea.autosalon.service;

import ru.mirea.autosalon.exception.BusinessException;
import ru.mirea.autosalon.exception.EntityNotFoundException;
import ru.mirea.autosalon.exception.ValidationException;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
import ru.mirea.autosalon.util.Formats;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Бизнес-логика заявок на тест-драйв: создание, изменение, смена статуса, удаление,
 * поиск, фильтрация и сортировка. Все бизнес-правила проверяются здесь, а не в меню.
 */
public class TestDriveRequestService {

    /** Часы работы автосалона: тест-драйв должен начаться и закончиться в это время. */
    public static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(21, 0);
    public static final int MIN_DURATION_MINUTES = 15;
    public static final int MAX_DURATION_MINUTES = 120;
    /** Записаться можно не дальше чем на 60 дней вперёд. */
    public static final int MAX_DAYS_AHEAD = 60;
    /** У одного клиента не больше 3 активных (новых или подтверждённых) заявок. */
    public static final int MAX_ACTIVE_REQUESTS_PER_CLIENT = 3;
    public static final int MAX_COMMENT_LENGTH = 500;

    private static final Comparator<TestDriveRequest> BY_SCHEDULE =
            TestDriveSortOption.BY_SCHEDULED_AT.getComparator();

    private final TestDriveRequestRepository requestRepository;
    private final ClientRepository clientRepository;
    private final CarRepository carRepository;
    private final Clock clock;

    public TestDriveRequestService(TestDriveRequestRepository requestRepository, ClientRepository clientRepository,
                                   CarRepository carRepository, Clock clock) {
        this.requestRepository = requestRepository;
        this.clientRepository = clientRepository;
        this.carRepository = carRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ CRUD

    public List<TestDriveRequest> getAll() {
        return requestRepository.findAll();
    }

    public TestDriveRequest getById(long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Заявка на тест-драйв с ID " + id + " не найдена"));
    }

    public TestDriveRequest create(long clientId, long carId, LocalDateTime scheduledAt, int durationMinutes,
                                   String comment) {
        Client client = findClient(clientId);
        Car car = findCar(carId);
        String normalizedComment = normalizeComment(comment);
        checkCarAvailable(car);
        checkSchedule(scheduledAt, durationMinutes);
        checkActiveRequestsLimit(client);
        checkNoTimeConflicts(client, car, scheduledAt, durationMinutes, null);

        TestDriveRequest request = new TestDriveRequest(client, car, scheduledAt, durationMinutes, normalizedComment);
        TestDriveRequest saved = requestRepository.save(request);
        return getById(saved.getId());
    }

    /** Изменяет автомобиль, время, длительность и комментарий. Клиента в заявке поменять нельзя. */
    public TestDriveRequest update(long id, long carId, LocalDateTime scheduledAt, int durationMinutes,
                                   String comment) {
        TestDriveRequest request = getById(id);
        checkEditable(request);
        if (scheduledAt == null) {
            throw new ValidationException("Дата и время тест-драйва обязательны.");
        }
        Car car = request.getCar().getId() == carId ? request.getCar() : findCar(carId);
        String normalizedComment = normalizeComment(comment);

        boolean carChanged = !car.getId().equals(request.getCar().getId());
        boolean timeChanged = !scheduledAt.equals(request.getScheduledAt())
                || durationMinutes != request.getDurationMinutes();
        if (carChanged) {
            checkCarAvailable(car);
        }
        if (timeChanged) {
            checkSchedule(scheduledAt, durationMinutes);
        }
        if (carChanged || timeChanged) {
            checkNoTimeConflicts(request.getClient(), car, scheduledAt, durationMinutes, request.getId());
        }

        request.setCar(car);
        request.setScheduledAt(scheduledAt);
        request.setDurationMinutes(durationMinutes);
        request.setComment(normalizedComment);
        requestRepository.update(request);
        return getById(id);
    }

    /** Бизнес-правило: закрытую заявку (завершена, отменена, неявка) редактировать нельзя. */
    public void checkEditable(TestDriveRequest request) {
        if (request.getStatus().isFinal()) {
            throw new BusinessException("Заявку №" + request.getId() + " в статусе \""
                    + request.getStatus().getDisplayName() + "\" изменить нельзя: она уже закрыта.");
        }
    }

    /**
     * Меняет статус заявки с проверкой жизненного цикла:
     * NEW -> CONFIRMED / CANCELLED, CONFIRMED -> COMPLETED / NO_SHOW / CANCELLED.
     */
    public TestDriveRequest changeStatus(long id, TestDriveStatus newStatus) {
        if (newStatus == null) {
            throw new ValidationException("Новый статус заявки не указан.");
        }
        TestDriveRequest request = getById(id);
        TestDriveStatus currentStatus = request.getStatus();
        if (currentStatus == newStatus) {
            throw new BusinessException("Заявка №" + id + " уже находится в статусе \""
                    + currentStatus.getDisplayName() + "\".");
        }
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new BusinessException("Недопустимый переход статуса: \"" + currentStatus.getDisplayName()
                    + "\" -> \"" + newStatus.getDisplayName() + "\". " + describeAllowedTransitions(currentStatus));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        boolean closesTestDrive = newStatus == TestDriveStatus.COMPLETED || newStatus == TestDriveStatus.NO_SHOW;
        if (closesTestDrive && now.isBefore(request.getScheduledAt())) {
            throw new BusinessException("Отметить заявку как \"" + newStatus.getDisplayName() + "\" можно только"
                    + " после начала тест-драйва (" + Formats.dateTime(request.getScheduledAt()) + ").");
        }
        if (newStatus == TestDriveStatus.CONFIRMED && !request.getScheduledAt().isAfter(now)) {
            throw new BusinessException("Нельзя подтвердить заявку: время тест-драйва ("
                    + Formats.dateTime(request.getScheduledAt()) + ") уже прошло. Такую заявку можно только отменить.");
        }
        request.setStatus(newStatus);
        requestRepository.update(request);
        return getById(id);
    }

    public void delete(long id) {
        getById(id);
        requestRepository.deleteById(id);
    }

    public static String describeAllowedTransitions(TestDriveStatus status) {
        if (status.getAllowedTransitions().isEmpty()) {
            return "Заявка закрыта, менять её статус нельзя.";
        }
        return "Из статуса \"" + status.getDisplayName() + "\" можно перейти в: "
                + status.getAllowedTransitions().stream()
                .map(TestDriveStatus::getDisplayName)
                .collect(Collectors.joining(", ")) + ".";
    }

    // ------------------------------------------------------------------ Поиск (SQL-запросы в репозитории)

    public List<TestDriveRequest> searchByClientName(String namePart) {
        return requestRepository.searchByClientName(requireSearchText(namePart));
    }

    public List<TestDriveRequest> searchByClientPhone(String phonePart) {
        String digits = phonePart == null ? "" : phonePart.replaceAll("[^0-9+]", "");
        if (digits.isEmpty()) {
            throw new ValidationException("Введите хотя бы одну цифру номера телефона.");
        }
        return requestRepository.searchByClientPhone(digits);
    }

    public List<TestDriveRequest> searchByCar(String brandOrModelPart) {
        return requestRepository.searchByCar(requireSearchText(brandOrModelPart));
    }

    public List<TestDriveRequest> searchByDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("Не указана дата для поиска.");
        }
        return requestRepository.findByDate(date);
    }

    // ------------------------------------------------------------------ Фильтрация (Stream API)

    public List<TestDriveRequest> filterByStatus(TestDriveStatus status) {
        return filter(request -> request.getStatus() == status);
    }

    public List<TestDriveRequest> filterByClient(long clientId) {
        findClient(clientId);
        return filter(request -> request.getClient().getId() == clientId);
    }

    public List<TestDriveRequest> filterByCar(long carId) {
        findCar(carId);
        return filter(request -> request.getCar().getId() == carId);
    }

    /** Заявки, тест-драйв по которым назначен на период с from по to включительно. */
    public List<TestDriveRequest> filterByPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Не указаны границы периода.");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("Начало периода (" + Formats.date(from) + ") не может быть позже конца ("
                    + Formats.date(to) + ").");
        }
        return filter(request -> {
            LocalDate date = request.getScheduledAt().toLocalDate();
            return !date.isBefore(from) && !date.isAfter(to);
        });
    }

    /** Предстоящие тест-драйвы: активные заявки, время которых ещё не наступило. */
    public List<TestDriveRequest> filterUpcoming() {
        LocalDateTime now = LocalDateTime.now(clock);
        return filter(request -> request.isActive() && request.getScheduledAt().isAfter(now));
    }

    private List<TestDriveRequest> filter(Predicate<TestDriveRequest> condition) {
        return requestRepository.findAll().stream()
                .filter(condition)
                .sorted(BY_SCHEDULE)
                .toList();
    }

    // ------------------------------------------------------------------ Сортировка (Comparator)

    public List<TestDriveRequest> getAllSorted(TestDriveSortOption option, boolean ascending) {
        Comparator<TestDriveRequest> comparator = ascending ? option.getComparator() : option.getComparator().reversed();
        return requestRepository.findAll().stream()
                .sorted(comparator)
                .toList();
    }

    // ------------------------------------------------------------------ Бизнес-правила

    /** Бизнес-правило: нельзя указать в заявке несуществующего клиента. */
    private Client findClient(long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + clientId + " не найден"));
    }

    /** Бизнес-правило: нельзя указать в заявке несуществующий автомобиль. */
    private Car findCar(long carId) {
        return carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Автомобиль с ID " + carId + " не найден"));
    }

    /** Бизнес-правило: на тест-драйв можно записать только автомобиль в статусе «В наличии». */
    private void checkCarAvailable(Car car) {
        if (!car.isAvailableForTestDrive()) {
            throw new BusinessException("Автомобиль " + car.getTitle() + " (ID " + car.getId() + ") недоступен"
                    + " для тест-драйва: статус \"" + car.getStatus().getDisplayName() + "\".");
        }
    }

    /** Бизнес-правило: тест-драйв в будущем, не дальше 60 дней, в часы работы салона, длительность 15–120 мин. */
    private void checkSchedule(LocalDateTime scheduledAt, int durationMinutes) {
        if (scheduledAt == null) {
            throw new ValidationException("Дата и время тест-драйва обязательны.");
        }
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            throw new ValidationException("Длительность тест-драйва должна быть от " + MIN_DURATION_MINUTES
                    + " до " + MAX_DURATION_MINUTES + " минут (указано: " + durationMinutes + ").");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!scheduledAt.isAfter(now)) {
            throw new BusinessException("Нельзя записаться на тест-драйв в прошлом: "
                    + Formats.dateTime(scheduledAt) + ".");
        }
        if (scheduledAt.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new BusinessException("Записаться на тест-драйв можно не более чем на " + MAX_DAYS_AHEAD
                    + " дней вперёд (до " + Formats.date(now.toLocalDate().plusDays(MAX_DAYS_AHEAD)) + ").");
        }
        LocalDateTime endAt = scheduledAt.plusMinutes(durationMinutes);
        boolean startsTooEarly = scheduledAt.toLocalTime().isBefore(OPENING_TIME);
        boolean endsTooLate = !endAt.toLocalDate().equals(scheduledAt.toLocalDate())
                || endAt.toLocalTime().isAfter(CLOSING_TIME);
        if (startsTooEarly || endsTooLate) {
            throw new BusinessException("Тест-драйв должен проходить в часы работы автосалона: с "
                    + OPENING_TIME + " до " + CLOSING_TIME + " (указано: " + Formats.time(scheduledAt) + " - "
                    + Formats.time(endAt) + ").");
        }
    }

    /** Бизнес-правило: у клиента не больше {@value #MAX_ACTIVE_REQUESTS_PER_CLIENT} активных заявок. */
    private void checkActiveRequestsLimit(Client client) {
        long activeCount = requestRepository.findByClientId(client.getId()).stream()
                .filter(TestDriveRequest::isActive)
                .count();
        if (activeCount >= MAX_ACTIVE_REQUESTS_PER_CLIENT) {
            throw new BusinessException("У клиента \"" + client.getFullName() + "\" уже " + activeCount
                    + " активные заявки (максимум " + MAX_ACTIVE_REQUESTS_PER_CLIENT + "). Завершите или отмените"
                    + " одну из них, прежде чем создавать новую.");
        }
    }

    /**
     * Бизнес-правило: автомобиль не может участвовать в двух тест-драйвах одновременно,
     * и клиент не может быть записан на два тест-драйва в одно и то же время.
     */
    private void checkNoTimeConflicts(Client client, Car car, LocalDateTime start, int durationMinutes,
                                      Long excludedRequestId) {
        LocalDateTime end = start.plusMinutes(durationMinutes);
        findConflict(requestRepository.findByCarId(car.getId()), start, end, excludedRequestId)
                .ifPresent(conflict -> {
                    throw new BusinessException("Автомобиль " + car.getTitle() + " уже занят в это время: заявка №"
                            + conflict.getId() + " (" + Formats.dateTime(conflict.getScheduledAt()) + " - "
                            + Formats.time(conflict.getEndAt()) + ").");
                });
        findConflict(requestRepository.findByClientId(client.getId()), start, end, excludedRequestId)
                .ifPresent(conflict -> {
                    throw new BusinessException("Клиент уже записан на другой тест-драйв в это время: заявка №"
                            + conflict.getId() + " (" + conflict.getCar().getTitle() + ", "
                            + Formats.dateTime(conflict.getScheduledAt()) + ").");
                });
    }

    private static Optional<TestDriveRequest> findConflict(List<TestDriveRequest> requests, LocalDateTime start,
                                                           LocalDateTime end, Long excludedRequestId) {
        return requests.stream()
                .filter(TestDriveRequest::isActive)
                .filter(request -> !Objects.equals(request.getId(), excludedRequestId))
                .filter(request -> request.overlaps(start, end))
                .findFirst();
    }

    private static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw new ValidationException("Комментарий не может быть длиннее " + MAX_COMMENT_LENGTH + " символов.");
        }
        return trimmed;
    }

    private static String requireSearchText(String text) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Строка поиска не может быть пустой.");
        }
        return text.trim();
    }
}
