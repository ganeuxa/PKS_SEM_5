package ru.mirea.autosalon.model;

import ru.mirea.autosalon.util.Formats;

import java.time.LocalDateTime;

/**
 * Заявка на тест-драйв автомобиля — основная сущность системы.
 * Соответствует таблице {@code test_drive_requests}; связана с клиентом
 * ({@code client_id -> clients.id}) и автомобилем ({@code car_id -> cars.id}).
 */
public class TestDriveRequest extends BaseEntity {

    private Client client;
    private Car car;
    private LocalDateTime scheduledAt;
    private int durationMinutes;
    private TestDriveStatus status;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestDriveRequest() {
    }

    /** Новая заявка: всегда создаётся в статусе {@link TestDriveStatus#NEW}. */
    public TestDriveRequest(Client client, Car car, LocalDateTime scheduledAt, int durationMinutes, String comment) {
        this.client = client;
        this.car = car;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.comment = comment;
        this.status = TestDriveStatus.NEW;
    }

    /** Заявка, прочитанная из базы данных. */
    public TestDriveRequest(Long id, Client client, Car car, LocalDateTime scheduledAt, int durationMinutes,
                            TestDriveStatus status, String comment, LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        super(id);
        this.client = client;
        this.car = car;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.status = status;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Время окончания тест-драйва. */
    public LocalDateTime getEndAt() {
        return scheduledAt.plusMinutes(durationMinutes);
    }

    public boolean isActive() {
        return status != null && status.isActive();
    }

    /** Пересекается ли тест-драйв с интервалом [start; end). Встречи «встык» не пересекаются. */
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return scheduledAt.isBefore(end) && start.isBefore(getEndAt());
    }

    @Override
    public String getDisplayName() {
        return "Заявка №" + getId() + ": " + client.getShortName() + ", " + car.getTitle()
                + ", " + Formats.dateTime(scheduledAt);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public TestDriveStatus getStatus() {
        return status;
    }

    public void setStatus(TestDriveStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TestDriveRequest{id=" + getId() + ", client=" + (client == null ? null : client.getId())
                + ", car=" + (car == null ? null : car.getId()) + ", scheduledAt=" + scheduledAt
                + ", status=" + status + "}";
    }
}
