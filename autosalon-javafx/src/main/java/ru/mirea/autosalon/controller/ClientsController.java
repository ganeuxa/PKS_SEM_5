package ru.mirea.autosalon.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import ru.mirea.autosalon.AppContext;
import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.service.ClientService;
import ru.mirea.autosalon.ui.Alerts;
import ru.mirea.autosalon.ui.FormDialog;
import ru.mirea.autosalon.ui.FxSupport;
import ru.mirea.autosalon.util.Formats;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Вкладка «Клиенты» (clients-tab.fxml): CRUD клиентов, поиск и заявки выбранного клиента (связанные данные).
 */
public class ClientsController {

    private final AppContext context;
    private final ClientService clientService;

    private final ObservableList<Client> clients = FXCollections.observableArrayList();
    private final FilteredList<Client> filteredClients = new FilteredList<>(clients);
    private final SortedList<Client> sortedClients = new SortedList<>(filteredClients);
    private final ObservableList<TestDriveRequest> clientRequests = FXCollections.observableArrayList();

    private Runnable onDataChanged = () -> {
    };

    @FXML
    private TextField searchField;
    @FXML
    private TableView<Client> clientTable;
    @FXML
    private TableColumn<Client, String> nameColumn;
    @FXML
    private TableColumn<Client, String> phoneColumn;
    @FXML
    private TableColumn<Client, String> emailColumn;
    @FXML
    private TableColumn<Client, LocalDate> birthDateColumn;
    @FXML
    private TableColumn<Client, Integer> ageColumn;
    @FXML
    private TableColumn<Client, String> licenseColumn;
    @FXML
    private TableColumn<Client, LocalDateTime> registeredColumn;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label countLabel;

    @FXML
    private Label requestsTitle;
    @FXML
    private TableView<TestDriveRequest> requestTable;
    @FXML
    private TableColumn<TestDriveRequest, Long> requestIdColumn;
    @FXML
    private TableColumn<TestDriveRequest, LocalDateTime> requestDateColumn;
    @FXML
    private TableColumn<TestDriveRequest, String> requestCarColumn;
    @FXML
    private TableColumn<TestDriveRequest, Integer> requestDurationColumn;
    @FXML
    private TableColumn<TestDriveRequest, TestDriveStatus> requestStatusColumn;

    public ClientsController(AppContext context) {
        this.context = context;
        this.clientService = context.clientService();
    }

    @FXML
    private void initialize() {
        setupColumns();
        clientTable.setItems(sortedClients);
        sortedClients.comparatorProperty().bind(clientTable.comparatorProperty());
        clientTable.getSortOrder().add(nameColumn);
        requestTable.setItems(clientRequests);

        clientTable.setRowFactory(table -> {
            TableRow<Client> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEdit();
                }
            });
            return row;
        });

        var noSelection = clientTable.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        clientTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> showClientRequests(selected));

        searchField.textProperty().addListener(
                (observable, oldValue, text) -> filteredClients.setPredicate(clientService.buildSearch(text)));
        filteredClients.addListener((ListChangeListener<Client>) change -> updateCount());
        showClientRequests(null);
        updateCount();
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = Objects.requireNonNull(onDataChanged);
    }

    /** Перечитывает клиентов из базы данных, сохраняя выбранную строку. */
    public void reload() {
        Long selectedId = selectedClient().map(Client::getId).orElse(null);
        try {
            clients.setAll(clientService.getAll());
            select(selectedId);
            showClientRequests(selectedClient().orElse(null));
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Загрузка клиентов", e);
        }
    }

    @FXML
    private void handleAdd() {
        FormDialog.<Client, ClientFormController>show(window(), "client-form.fxml", context,
                        form -> form.setClient(null))
                .ifPresent(this::afterSave);
    }

    @FXML
    private void handleEdit() {
        selectedClient().ifPresent(client ->
                FormDialog.<Client, ClientFormController>show(window(), "client-form.fxml", context,
                                form -> form.setClient(client))
                        .ifPresent(this::afterSave));
    }

    @FXML
    private void handleDelete() {
        Optional<Client> selected = selectedClient();
        if (selected.isEmpty()) {
            return;
        }
        Client client = selected.get();
        boolean confirmed = Alerts.confirm(window(), "Удаление клиента",
                "Удалить клиента «" + client.getFullName() + "»?",
                "Телефон " + client.getPhone() + ", ВУ " + client.getLicenseNumber() + ".\n"
                        + "Клиента, у которого есть заявки на тест-драйв, удалить нельзя: история заявок сохраняется.",
                "Удалить");
        if (!confirmed) {
            return;
        }
        try {
            clientService.delete(client.getId());
            reload();
            onDataChanged.run();
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Удаление клиента", e);
        }
    }

    @FXML
    private void handleRefresh() {
        reload();
    }

    @FXML
    private void handleExport() {
        if (sortedClients.isEmpty()) {
            Alerts.showInfo(window(), "Экспорт", "Нет данных для экспорта", "В таблице нет ни одного клиента.");
            return;
        }
        File file = FxSupport.csvFileChooser("Экспорт клиентов в CSV", "clients.csv").showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            context.exportService().exportClients(List.copyOf(sortedClients), file.toPath());
            Alerts.showInfo(window(), "Экспорт", "Экспорт выполнен",
                    "Выгружено клиентов: " + sortedClients.size() + ".\nФайл: " + file.getAbsolutePath());
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Экспорт клиентов", e);
        }
    }

    private void afterSave(Client saved) {
        reload();
        select(saved.getId());
        onDataChanged.run();
    }

    /** Заявки выбранного клиента: связь clients 1 — N test_drive_requests. */
    private void showClientRequests(Client client) {
        if (client == null) {
            requestsTitle.setText("Заявки клиента: выберите клиента в таблице");
            clientRequests.clear();
            return;
        }
        try {
            List<TestDriveRequest> requests = context.requestService().filterByClient(client.getId());
            clientRequests.setAll(requests);
            long active = requests.stream().filter(TestDriveRequest::isActive).count();
            requestsTitle.setText("Заявки клиента " + client.getShortName() + ": всего " + requests.size()
                    + ", активных " + active);
        } catch (AutosalonException e) {
            clientRequests.clear();
            requestsTitle.setText("Заявки клиента: не удалось загрузить (" + Alerts.kindOf(e).toLowerCase() + ")");
        }
    }

    private void setupColumns() {
        nameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getFullName()));
        nameColumn.setComparator(Formats.russianOrder());
        phoneColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPhone()));
        emailColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getEmail()));
        birthDateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getBirthDate()));
        birthDateColumn.setCellFactory(FxSupport.formattedCell(Formats::date));
        ageColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAge(LocalDate.now())));
        licenseColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getLicenseNumber()));
        registeredColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getRegisteredAt()));
        registeredColumn.setCellFactory(FxSupport.formattedCell(Formats::dateTime));

        requestIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        requestDateColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getScheduledAt()));
        requestDateColumn.setCellFactory(FxSupport.formattedCell(Formats::dateTime));
        requestCarColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCar().getTitle()));
        requestDurationColumn.setCellValueFactory(
                cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getDurationMinutes()));
        requestStatusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        requestStatusColumn.setCellFactory(FxSupport.statusCell());
    }

    private void updateCount() {
        countLabel.setText("Показано клиентов: " + filteredClients.size() + " из " + clients.size());
    }

    private Optional<Client> selectedClient() {
        return Optional.ofNullable(clientTable.getSelectionModel().getSelectedItem());
    }

    private void select(Long id) {
        if (id == null) {
            return;
        }
        sortedClients.stream()
                .filter(client -> client.getId().equals(id))
                .findFirst()
                .ifPresent(client -> {
                    clientTable.getSelectionModel().select(client);
                    clientTable.scrollTo(client);
                });
    }

    private Window window() {
        return clientTable.getScene() == null ? null : clientTable.getScene().getWindow();
    }
}
