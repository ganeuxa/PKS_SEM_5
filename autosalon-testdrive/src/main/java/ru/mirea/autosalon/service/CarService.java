package ru.mirea.autosalon.service;

import ru.mirea.autosalon.exception.BusinessException;
import ru.mirea.autosalon.exception.EntityNotFoundException;
import ru.mirea.autosalon.exception.ValidationException;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.repository.CarRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Бизнес-логика работы с автомобилями автосалона.
 */
public class CarService {

    public static final int MIN_MANUFACTURE_YEAR = 1990;
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000000");
    private static final int MAX_TEXT_LENGTH = 50;
    private static final int MAX_COLOR_LENGTH = 30;
    /** VIN: 17 символов, латинские буквы (кроме I, O, Q, чтобы не путать с 1 и 0) и цифры. */
    private static final Pattern VIN_PATTERN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    private final CarRepository carRepository;
    private final TestDriveRequestRepository requestRepository;
    private final Clock clock;

    public CarService(CarRepository carRepository, TestDriveRequestRepository requestRepository, Clock clock) {
        this.carRepository = carRepository;
        this.requestRepository = requestRepository;
        this.clock = clock;
    }

    public List<Car> getAll() {
        return carRepository.findAll();
    }

    /** Автомобили, на которые сейчас можно записаться на тест-драйв. */
    public List<Car> getAvailableForTestDrive() {
        return carRepository.findAll().stream()
                .filter(Car::isAvailableForTestDrive)
                .toList();
    }

    public Car getById(long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Автомобиль с ID " + id + " не найден"));
    }

    public Car create(Car car) {
        normalize(car);
        validate(car);
        checkVinIsUnique(car);
        Car saved = carRepository.save(car);
        return getById(saved.getId());
    }

    public Car update(Car car) {
        if (car.getId() == null) {
            throw new ValidationException("Не указан ID автомобиля, данные которого нужно изменить.");
        }
        getById(car.getId());
        normalize(car);
        validate(car);
        checkVinIsUnique(car);
        checkCanLeaveSale(car);
        carRepository.update(car);
        return getById(car.getId());
    }

    /** Бизнес-правило: нельзя удалить автомобиль, по которому есть заявки на тест-драйв. */
    public void delete(long id) {
        Car car = getById(id);
        List<TestDriveRequest> requests = requestRepository.findByCarId(id);
        if (!requests.isEmpty()) {
            throw new BusinessException("Нельзя удалить автомобиль " + car.getTitle() + ": по нему есть заявки"
                    + " на тест-драйв (" + requests.size() + " шт.). История тест-драйвов должна сохраняться.");
        }
        carRepository.deleteById(id);
    }

    /**
     * Бизнес-правило: автомобиль нельзя продать или отправить на обслуживание,
     * пока на него есть активные (новые или подтверждённые) заявки на тест-драйв.
     */
    private void checkCanLeaveSale(Car car) {
        if (car.isAvailableForTestDrive()) {
            return;
        }
        List<TestDriveRequest> activeRequests = requestRepository.findByCarId(car.getId()).stream()
                .filter(TestDriveRequest::isActive)
                .toList();
        if (!activeRequests.isEmpty()) {
            String ids = activeRequests.stream()
                    .map(request -> "№" + request.getId())
                    .collect(Collectors.joining(", "));
            throw new BusinessException("Нельзя перевести автомобиль в статус \"" + car.getStatus().getDisplayName()
                    + "\": на него есть активные заявки на тест-драйв (" + ids
                    + "). Сначала завершите или отмените их.");
        }
    }

    private void normalize(Car car) {
        car.setBrand(collapseSpaces(car.getBrand()));
        car.setModel(collapseSpaces(car.getModel()));
        car.setColor(collapseSpaces(car.getColor()));
        if (car.getVin() != null) {
            car.setVin(car.getVin().replaceAll("\\s+", "").toUpperCase(Locale.ROOT));
        }
    }

    private void validate(Car car) {
        requireText(car.getBrand(), "Марка автомобиля", MAX_TEXT_LENGTH);
        requireText(car.getModel(), "Модель автомобиля", MAX_TEXT_LENGTH);
        requireText(car.getColor(), "Цвет автомобиля", MAX_COLOR_LENGTH);

        int maxYear = LocalDate.now(clock).getYear() + 1;
        if (car.getManufactureYear() < MIN_MANUFACTURE_YEAR || car.getManufactureYear() > maxYear) {
            throw new ValidationException("Год выпуска должен быть в диапазоне от " + MIN_MANUFACTURE_YEAR
                    + " до " + maxYear + ".");
        }
        if (car.getVin() == null || car.getVin().isEmpty()) {
            throw new ValidationException("VIN автомобиля обязателен для заполнения.");
        }
        if (!VIN_PATTERN.matcher(car.getVin()).matches()) {
            throw new ValidationException("Некорректный VIN \"" + car.getVin() + "\": нужно ровно 17 символов,"
                    + " латинские буквы (кроме I, O, Q) и цифры.");
        }
        if (car.getPrice() == null || car.getPrice().signum() <= 0) {
            throw new ValidationException("Цена автомобиля должна быть больше нуля.");
        }
        if (car.getPrice().compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("Цена автомобиля слишком большая (максимум 1 000 000 000 руб.).");
        }
        if (car.getStatus() == null) {
            throw new ValidationException("Статус автомобиля обязателен.");
        }
    }

    /** Бизнес-правило: VIN уникален — двух автомобилей с одинаковым VIN не бывает. */
    private void checkVinIsUnique(Car car) {
        carRepository.findByVin(car.getVin())
                .filter(other -> !other.getId().equals(car.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Автомобиль с VIN " + car.getVin() + " уже есть в базе: "
                            + other.getTitle() + " (ID " + other.getId() + ").");
                });
    }

    private static void requireText(String value, String fieldName, int maxLength) {
        if (value == null || value.isEmpty()) {
            throw new ValidationException(fieldName + ": поле обязательно для заполнения.");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(fieldName + ": не более " + maxLength + " символов.");
        }
    }

    private static String collapseSpaces(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
