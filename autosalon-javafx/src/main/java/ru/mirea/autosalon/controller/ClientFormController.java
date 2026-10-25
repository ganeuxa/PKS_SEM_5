package ru.mirea.autosalon.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import ru.mirea.autosalon.AppContext;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.ui.FxSupport;

import java.time.LocalDate;

/**
 * Форма добавления и редактирования клиента (client-form.fxml).
 * Формат телефона, email и номера ВУ, возраст и уникальность проверяет {@link ru.mirea.autosalon.service.ClientService}.
 */
public class ClientFormController extends AbstractFormController<Client> {

    private static final int MAX_NAME_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 20;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_LICENSE_LENGTH = 14;

    private final AppContext context;

    /** Редактируемый клиент; null — создаётся новый. */
    private Client client;

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private DatePicker birthDatePicker;
    @FXML
    private TextField licenseField;

    public ClientFormController(AppContext context) {
        this.context = context;
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        FxSupport.setupDatePicker(birthDatePicker);
    }

    public void setClient(Client client) {
        this.client = client;
        if (client == null) {
            return;
        }
        fullNameField.setText(client.getFullName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
        birthDatePicker.setValue(client.getBirthDate());
        licenseField.setText(client.getLicenseNumber());
    }

    @Override
    public String getTitle() {
        return client == null ? "Новый клиент" : "Изменение клиента: " + client.getShortName();
    }

    @Override
    protected Client doSave() {
        String fullName = requireText(fullNameField, "ФИО", MAX_NAME_LENGTH);
        String phone = requireText(phoneField, "Телефон", MAX_PHONE_LENGTH);
        String email = optionalText(emailField, "Email", MAX_EMAIL_LENGTH);
        LocalDate birthDate = requireDate(birthDatePicker, "Дата рождения");
        String license = requireText(licenseField, "Номер водительского удостоверения", MAX_LICENSE_LENGTH);

        if (client == null) {
            return context.clientService().create(new Client(fullName, phone, email, birthDate, license));
        }
        // Изменяем копию: если сервис отклонит данные, объект в таблице останется прежним
        Client changed = new Client(client.getId(), fullName, phone, email, birthDate, license,
                client.getRegisteredAt());
        return context.clientService().update(changed);
    }
}
