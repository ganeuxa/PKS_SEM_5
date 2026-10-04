package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.service.ClientService;

import java.time.LocalDate;

/**
 * Меню «Клиенты»: просмотр, добавление, изменение и удаление клиентов.
 */
public class ClientMenu extends AbstractMenu {

    private final ClientService clientService;

    public ClientMenu(ConsoleInput input, ClientService clientService) {
        super("КЛИЕНТЫ", input);
        this.clientService = clientService;
        addItem("Показать всех клиентов", this::showAll);
        addItem("Найти клиента по ID", this::showById);
        addItem("Добавить клиента", this::create);
        addItem("Изменить данные клиента", this::update);
        addItem("Удалить клиента", this::delete);
    }

    private void showAll() {
        ConsoleView.printClients(clientService.getAll());
    }

    private void showById() {
        long id = input.readId("Введите ID клиента");
        ConsoleView.printClientDetails(clientService.getById(id));
    }

    private void create() {
        System.out.println("Новый клиент. Поля, отмеченные *, обязательны.");
        String fullName = input.readLine("* ФИО (Фамилия Имя Отчество)");
        String phone = input.readLine("* Телефон (+7XXXXXXXXXX)");
        String email = input.readOptionalLine("Email (Enter - пропустить)");
        LocalDate birthDate = input.readDate("* Дата рождения");
        String licenseNumber = input.readLine("* Номер водительского удостоверения (10 цифр)");

        Client created = clientService.create(new Client(fullName, phone, email, birthDate, licenseNumber));
        System.out.println("Клиент добавлен.");
        ConsoleView.printClientDetails(created);
    }

    private void update() {
        long id = input.readId("Введите ID клиента");
        Client client = clientService.getById(id);
        ConsoleView.printClientDetails(client);
        System.out.println("Введите новые значения (Enter - оставить текущее):");
        client.setFullName(input.readLineOrKeep("ФИО", client.getFullName()));
        client.setPhone(input.readLineOrKeep("Телефон", client.getPhone()));
        client.setEmail(input.readOptionalLineOrKeep("Email", client.getEmail()));
        client.setBirthDate(input.readDateOrKeep("Дата рождения", client.getBirthDate()));
        client.setLicenseNumber(input.readLineOrKeep("Номер ВУ", client.getLicenseNumber()));

        Client updated = clientService.update(client);
        System.out.println("Данные клиента обновлены.");
        ConsoleView.printClientDetails(updated);
    }

    private void delete() {
        long id = input.readId("Введите ID клиента");
        Client client = clientService.getById(id);
        if (!input.confirm("Удалить клиента \"" + client.getDisplayName() + "\"?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        clientService.delete(id);
        System.out.println("Клиент удалён.");
    }
}
