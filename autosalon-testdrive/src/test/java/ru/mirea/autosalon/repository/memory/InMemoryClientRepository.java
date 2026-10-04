package ru.mirea.autosalon.repository.memory;

import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.repository.ClientRepository;

import java.util.Objects;
import java.util.Optional;

public class InMemoryClientRepository extends InMemoryCrudRepository<Client> implements ClientRepository {

    @Override
    public Optional<Client> findByPhone(String phone) {
        return storage.values().stream().filter(client -> Objects.equals(client.getPhone(), phone)).findFirst();
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return storage.values().stream().filter(client -> Objects.equals(client.getEmail(), email)).findFirst();
    }

    @Override
    public Optional<Client> findByLicenseNumber(String licenseNumber) {
        return storage.values().stream()
                .filter(client -> Objects.equals(client.getLicenseNumber(), licenseNumber))
                .findFirst();
    }
}
