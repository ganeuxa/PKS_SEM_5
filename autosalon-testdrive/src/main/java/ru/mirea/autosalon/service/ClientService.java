package ru.mirea.autosalon.service;

import ru.mirea.autosalon.exception.BusinessException;
import ru.mirea.autosalon.exception.EntityNotFoundException;
import ru.mirea.autosalon.exception.ValidationException;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.repository.ClientRepository;
import ru.mirea.autosalon.repository.TestDriveRequestRepository;
import ru.mirea.autosalon.util.Formats;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Бизнес-логика работы с клиентами: проверка данных, уникальность, правила удаления.
 */
public class ClientService {

    /** Тест-драйв доступен только совершеннолетним водителям. */
    public static final int MIN_AGE = 18;

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[А-ЯЁа-яёA-Za-z]+(?:[ -][А-ЯЁа-яёA-Za-z]+)*$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+7\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern LICENSE_PATTERN = Pattern.compile("^\\d{10}$");

    private final ClientRepository clientRepository;
    private final TestDriveRequestRepository requestRepository;
    private final Clock clock;

    public ClientService(ClientRepository clientRepository, TestDriveRequestRepository requestRepository,
                         Clock clock) {
        this.clientRepository = clientRepository;
        this.requestRepository = requestRepository;
        this.clock = clock;
    }

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    public Client getById(long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден"));
    }

    public Client create(Client client) {
        normalize(client);
        validate(client);
        checkUniqueness(client);
        Client saved = clientRepository.save(client);
        return getById(saved.getId());
    }

    public Client update(Client client) {
        if (client.getId() == null) {
            throw new ValidationException("Не указан ID клиента, данные которого нужно изменить.");
        }
        getById(client.getId());
        normalize(client);
        validate(client);
        checkUniqueness(client);
        clientRepository.update(client);
        return getById(client.getId());
    }

    /** Бизнес-правило: нельзя удалить клиента, у которого есть заявки на тест-драйв (история сохраняется). */
    public void delete(long id) {
        Client client = getById(id);
        List<TestDriveRequest> requests = requestRepository.findByClientId(id);
        if (!requests.isEmpty()) {
            throw new BusinessException("Нельзя удалить клиента \"" + client.getFullName() + "\": у него есть заявки"
                    + " на тест-драйв (" + requests.size() + " шт.). История заявок должна сохраняться.");
        }
        clientRepository.deleteById(id);
    }

    /** Приводит ввод к единому виду: лишние пробелы, телефон в формате +7XXXXXXXXXX, email в нижнем регистре. */
    private void normalize(Client client) {
        client.setFullName(collapseSpaces(client.getFullName()));
        client.setPhone(normalizePhone(client.getPhone()));
        String email = collapseSpaces(client.getEmail());
        client.setEmail(email == null || email.isEmpty() ? null : email.toLowerCase(Locale.ROOT));
        String license = client.getLicenseNumber();
        client.setLicenseNumber(license == null ? null : license.replaceAll("\\s+", ""));
    }

    /** 8 (916) 123-45-67, 7-916-123-45-67 и +7 916 123 45 67 превращаются в +79161234567. */
    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[\\s()-]", "");
        if (digits.matches("8\\d{10}")) {
            return "+7" + digits.substring(1);
        }
        if (digits.matches("7\\d{10}")) {
            return "+" + digits;
        }
        return digits;
    }

    private void validate(Client client) {
        String fullName = client.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            throw new ValidationException("ФИО клиента обязательно для заполнения.");
        }
        if (fullName.length() > MAX_NAME_LENGTH || !NAME_PATTERN.matcher(fullName).matches()) {
            throw new ValidationException("ФИО может содержать только буквы, пробелы и дефисы (не длиннее "
                    + MAX_NAME_LENGTH + " символов): \"" + fullName + "\".");
        }
        if (fullName.split(" ").length < 2) {
            throw new ValidationException("Укажите как минимум фамилию и имя клиента.");
        }

        String phone = client.getPhone();
        if (phone == null || phone.isEmpty()) {
            throw new ValidationException("Телефон клиента обязателен для заполнения.");
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new ValidationException("Некорректный телефон \"" + phone
                    + "\". Ожидается формат +7XXXXXXXXXX, например +79161234567.");
        }

        String email = client.getEmail();
        if (email != null && (email.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(email).matches())) {
            throw new ValidationException("Некорректный email \"" + email + "\".");
        }

        LocalDate birthDate = client.getBirthDate();
        LocalDate today = LocalDate.now(clock);
        if (birthDate == null) {
            throw new ValidationException("Дата рождения клиента обязательна для заполнения.");
        }
        if (birthDate.isAfter(today) || birthDate.isBefore(LocalDate.of(1900, 1, 1))) {
            throw new ValidationException("Некорректная дата рождения: " + Formats.date(birthDate) + ".");
        }
        int age = client.getAge(today);
        if (age < MIN_AGE) {
            throw new BusinessException("Клиенту должно быть не меньше " + MIN_AGE + " лет: тест-драйв доступен"
                    + " только совершеннолетним водителям (возраст клиента: " + age + ").");
        }

        String license = client.getLicenseNumber();
        if (license == null || license.isEmpty()) {
            throw new ValidationException("Номер водительского удостоверения обязателен для заполнения.");
        }
        if (!LICENSE_PATTERN.matcher(license).matches()) {
            throw new ValidationException("Номер водительского удостоверения должен состоять из 10 цифр,"
                    + " например 77 12 345678.");
        }
    }

    /** Бизнес-правило: телефон, email и номер водительского удостоверения не могут повторяться у разных клиентов. */
    private void checkUniqueness(Client client) {
        ensureNotTaken(clientRepository.findByPhone(client.getPhone()), client, "Телефон " + client.getPhone());
        if (client.getEmail() != null) {
            ensureNotTaken(clientRepository.findByEmail(client.getEmail()), client, "Email " + client.getEmail());
        }
        ensureNotTaken(clientRepository.findByLicenseNumber(client.getLicenseNumber()), client,
                "Номер водительского удостоверения " + client.getLicenseNumber());
    }

    private void ensureNotTaken(Optional<Client> existing, Client client, String what) {
        existing.filter(other -> !other.getId().equals(client.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(what + " уже указан у клиента \"" + other.getFullName()
                            + "\" (ID " + other.getId() + ").");
                });
    }

    private static String collapseSpaces(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }
}
