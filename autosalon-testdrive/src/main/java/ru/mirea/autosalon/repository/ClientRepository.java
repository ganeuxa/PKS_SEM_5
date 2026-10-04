package ru.mirea.autosalon.repository;

import ru.mirea.autosalon.model.Client;

import java.util.Optional;

/**
 * Хранилище клиентов автосалона.
 */
public interface ClientRepository extends CrudRepository<Client> {

    Optional<Client> findByPhone(String phone);

    Optional<Client> findByEmail(String email);

    Optional<Client> findByLicenseNumber(String licenseNumber);
}
