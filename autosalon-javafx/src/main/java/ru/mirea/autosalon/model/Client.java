package ru.mirea.autosalon.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Клиент автосалона — участник предметной области, который записывается на тест-драйв.
 * Соответствует таблице {@code clients}.
 */
public class Client extends BaseEntity {

    private String fullName;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private String licenseNumber;
    private LocalDateTime registeredAt;

    public Client() {
    }

    /** Новый клиент: ID и дату регистрации назначит база данных. */
    public Client(String fullName, String phone, String email, LocalDate birthDate, String licenseNumber) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
        this.licenseNumber = licenseNumber;
    }

    /** Клиент, прочитанный из базы данных. */
    public Client(Long id, String fullName, String phone, String email, LocalDate birthDate,
                  String licenseNumber, LocalDateTime registeredAt) {
        super(id);
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.birthDate = birthDate;
        this.licenseNumber = licenseNumber;
        this.registeredAt = registeredAt;
    }

    /** Возраст клиента (полных лет) на указанную дату. */
    public int getAge(LocalDate onDate) {
        return Period.between(birthDate, onDate).getYears();
    }

    /** Фамилия и инициалы, например «Иванов А.С.», для компактных таблиц. */
    public String getShortName() {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder shortName = new StringBuilder(parts[0]);
        if (parts.length > 1) {
            shortName.append(' ');
            for (int i = 1; i < parts.length; i++) {
                shortName.append(parts[i].charAt(0)).append('.');
            }
        }
        return shortName.toString();
    }

    @Override
    public String getDisplayName() {
        return fullName + ", " + phone;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String toString() {
        return "Client{id=" + getId() + ", fullName='" + fullName + "', phone='" + phone + "'}";
    }
}
