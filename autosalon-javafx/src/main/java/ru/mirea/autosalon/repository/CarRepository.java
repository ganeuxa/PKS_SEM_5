package ru.mirea.autosalon.repository;

import ru.mirea.autosalon.model.Car;

import java.util.Optional;

/**
 * Хранилище автомобилей автосалона.
 */
public interface CarRepository extends CrudRepository<Car> {

    Optional<Car> findByVin(String vin);
}
