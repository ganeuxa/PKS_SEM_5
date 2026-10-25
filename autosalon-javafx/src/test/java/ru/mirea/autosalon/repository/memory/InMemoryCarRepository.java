package ru.mirea.autosalon.repository.memory;

import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.repository.CarRepository;

import java.util.Objects;
import java.util.Optional;

public class InMemoryCarRepository extends InMemoryCrudRepository<Car> implements CarRepository {

    @Override
    public Optional<Car> findByVin(String vin) {
        return storage.values().stream().filter(car -> Objects.equals(car.getVin(), vin)).findFirst();
    }
}
