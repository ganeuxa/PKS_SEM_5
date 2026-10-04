package ru.mirea.autosalon.repository.memory;

import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class InMemoryTestDriveRequestRepository extends InMemoryCrudRepository<TestDriveRequest>
        implements TestDriveRequestRepository {

    @Override
    public TestDriveRequest save(TestDriveRequest request) {
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(request.getCreatedAt());
        return super.save(request);
    }

    @Override
    public List<TestDriveRequest> findByClientId(long clientId) {
        return find(request -> request.getClient().getId() == clientId);
    }

    @Override
    public List<TestDriveRequest> findByCarId(long carId) {
        return find(request -> request.getCar().getId() == carId);
    }

    @Override
    public List<TestDriveRequest> searchByClientName(String namePart) {
        return find(request -> containsIgnoreCase(request.getClient().getFullName(), namePart));
    }

    @Override
    public List<TestDriveRequest> searchByClientPhone(String phonePart) {
        return find(request -> request.getClient().getPhone().contains(phonePart));
    }

    @Override
    public List<TestDriveRequest> searchByCar(String brandOrModelPart) {
        return find(request -> containsIgnoreCase(request.getCar().getTitle(), brandOrModelPart));
    }

    @Override
    public List<TestDriveRequest> findByDate(LocalDate date) {
        return find(request -> request.getScheduledAt().toLocalDate().equals(date));
    }

    private List<TestDriveRequest> find(Predicate<TestDriveRequest> condition) {
        return storage.values().stream().filter(condition).toList();
    }

    private static boolean containsIgnoreCase(String text, String part) {
        return text.toLowerCase(Locale.ROOT).contains(part.toLowerCase(Locale.ROOT));
    }
}
