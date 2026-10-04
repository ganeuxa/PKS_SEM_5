package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.CarStatus;
import ru.mirea.autosalon.service.CarService;

import java.math.BigDecimal;
import java.util.List;

/**
 * Меню «Автомобили»: просмотр, добавление, изменение и удаление автомобилей.
 */
public class CarMenu extends AbstractMenu {

    private static final List<CarStatus> STATUSES = List.of(CarStatus.values());

    private final CarService carService;

    public CarMenu(ConsoleInput input, CarService carService) {
        super("АВТОМОБИЛИ", input);
        this.carService = carService;
        addItem("Показать все автомобили", this::showAll);
        addItem("Показать автомобили, доступные для тест-драйва", this::showAvailable);
        addItem("Найти автомобиль по ID", this::showById);
        addItem("Добавить автомобиль", this::create);
        addItem("Изменить данные или статус автомобиля", this::update);
        addItem("Удалить автомобиль", this::delete);
    }

    private void showAll() {
        ConsoleView.printCars(carService.getAll());
    }

    private void showAvailable() {
        ConsoleView.printCars(carService.getAvailableForTestDrive());
    }

    private void showById() {
        long id = input.readId("Введите ID автомобиля");
        ConsoleView.printCarDetails(carService.getById(id));
    }

    private void create() {
        System.out.println("Новый автомобиль. Все поля обязательны.");
        String brand = input.readLine("Марка");
        String model = input.readLine("Модель");
        int year = input.readInt("Год выпуска");
        String vin = input.readLine("VIN (17 символов)");
        String color = input.readLine("Цвет");
        BigDecimal price = input.readMoney("Цена, руб.");
        CarStatus status = input.choose("Статус", STATUSES, CarStatus::getDisplayName);

        Car created = carService.create(new Car(brand, model, year, vin, color, price, status));
        System.out.println("Автомобиль добавлен.");
        ConsoleView.printCarDetails(created);
    }

    private void update() {
        long id = input.readId("Введите ID автомобиля");
        Car car = carService.getById(id);
        ConsoleView.printCarDetails(car);
        System.out.println("Введите новые значения (Enter - оставить текущее):");
        car.setBrand(input.readLineOrKeep("Марка", car.getBrand()));
        car.setModel(input.readLineOrKeep("Модель", car.getModel()));
        car.setManufactureYear(input.readIntOrKeep("Год выпуска", car.getManufactureYear()));
        car.setVin(input.readLineOrKeep("VIN", car.getVin()));
        car.setColor(input.readLineOrKeep("Цвет", car.getColor()));
        car.setPrice(input.readMoneyOrKeep("Цена, руб.", car.getPrice()));
        car.setStatus(input.chooseOrKeep("Статус", STATUSES, CarStatus::getDisplayName, car.getStatus()));

        Car updated = carService.update(car);
        System.out.println("Данные автомобиля обновлены.");
        ConsoleView.printCarDetails(updated);
    }

    private void delete() {
        long id = input.readId("Введите ID автомобиля");
        Car car = carService.getById(id);
        if (!input.confirm("Удалить автомобиль \"" + car.getDisplayName() + "\"?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        carService.delete(id);
        System.out.println("Автомобиль удалён.");
    }
}
