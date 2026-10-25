package ru.mirea.autosalon.model;

import java.math.BigDecimal;

/**
 * Автомобиль автосалона, на котором можно пройти тест-драйв.
 * Соответствует таблице {@code cars}.
 */
public class Car extends BaseEntity {

    private String brand;
    private String model;
    private int manufactureYear;
    private String vin;
    private String color;
    private BigDecimal price;
    private CarStatus status;

    public Car() {
    }

    /** Новый автомобиль: ID назначит база данных. */
    public Car(String brand, String model, int manufactureYear, String vin, String color,
               BigDecimal price, CarStatus status) {
        this.brand = brand;
        this.model = model;
        this.manufactureYear = manufactureYear;
        this.vin = vin;
        this.color = color;
        this.price = price;
        this.status = status;
    }

    /** Автомобиль, прочитанный из базы данных. */
    public Car(Long id, String brand, String model, int manufactureYear, String vin, String color,
               BigDecimal price, CarStatus status) {
        this(brand, model, manufactureYear, vin, color, price, status);
        setId(id);
    }

    /** Марка и модель, например «Toyota Camry». */
    public String getTitle() {
        return brand + " " + model;
    }

    public boolean isAvailableForTestDrive() {
        return status != null && status.isAvailableForTestDrive();
    }

    @Override
    public String getDisplayName() {
        return getTitle() + " " + manufactureYear + " (" + color + ") - " + status.getDisplayName();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(int manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public CarStatus getStatus() {
        return status;
    }

    public void setStatus(CarStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Car{id=" + getId() + ", " + getTitle() + ", vin='" + vin + "', status=" + status + "}";
    }
}
