package ru.mirea.autosalon.repository;

import ru.mirea.autosalon.model.TestDriveRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Хранилище заявок на тест-драйв. Заявки возвращаются вместе с данными клиента и автомобиля.
 */
public interface TestDriveRequestRepository extends CrudRepository<TestDriveRequest> {

    List<TestDriveRequest> findByClientId(long clientId);

    List<TestDriveRequest> findByCarId(long carId);

    /** Поиск по части ФИО клиента без учёта регистра. */
    List<TestDriveRequest> searchByClientName(String namePart);

    /** Поиск по части номера телефона клиента. */
    List<TestDriveRequest> searchByClientPhone(String phonePart);

    /** Поиск по части марки или модели автомобиля без учёта регистра. */
    List<TestDriveRequest> searchByCar(String brandOrModelPart);

    /** Заявки, тест-драйв по которым назначен на указанную дату. */
    List<TestDriveRequest> findByDate(LocalDate date);
}
