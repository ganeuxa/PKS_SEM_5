package ru.mirea.autosalon.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ru.mirea.autosalon.AppContext;
import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.exception.ValidationException;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.service.RequestFilter;
import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.service.TestDriveSortOption;
import ru.mirea.autosalon.ui.Alerts;
import ru.mirea.autosalon.ui.Choice;
import ru.mirea.autosalon.ui.FormDialog;
import ru.mirea.autosalon.ui.FxSupport;
import ru.mirea.autosalon.ui.ViewLoader;
import ru.mirea.autosalon.util.Formats;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Вкладка «Заявки на тест-драйв» (requests-tab.fxml): таблица основной сущности, CRUD, смена статуса,
 * поиск, фильтры, сортировка и экспорт.
 * <p>
 * Данные проходят через цепочку коллекций JavaFX:
 * <pre>
 *   ObservableList (все заявки из БД) -> FilteredList (поиск и фильтры) -> SortedList (сортировка) -> TableView
 * </pre>
 * Когда меняется содержимое ObservableList или условие FilteredList, таблица перерисовывается сама.
 */
public class RequestsController {

    private final AppContext context;
    private final TestDriveRequestService requestService;

    /** Все заявки, загруженные из базы данных. */
    private final ObservableList<TestDriveRequest> requests = FXCollections.observableArrayList();
    private final FilteredList<TestDriveRequest> filteredRequests = new FilteredList<>(requests);
    private final SortedList<TestDriveRequest> sortedRequests = new SortedList<>(filteredRequests);

    private final Map<TestDriveSortOption, TableColumn<TestDriveRequest, ?>> sortColumns =
            new EnumMap<>(TestDriveSortOption.class);

    /** Что сделать после изменения данных (обновить связанные данные на других вкладках). */
    private Runnable onDataChanged = () -> {
    };

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Choice<TestDriveStatus>> statusFilter;
    @FXML
    private ComboBox<Choice<String>> brandFilter;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private CheckBox upcomingOnlyCheck;
    @FXML
    private Label filterErrorLabel;
    @FXML
    private ComboBox<TestDriveSortOption> sortBox;
    @FXML
    private CheckBox descendingCheck;

    @FXML
    private TableView<TestDriveRequest> requestTable;
    @FXML
    private TableColumn<TestDriveRequest, Long> idColumn;
    @FXML
    private TableColumn<TestDriveRequest, LocalDateTime> scheduledColumn;
    @FXML
    private TableColumn<TestDriveRequest, Integer> durationColumn;
    @FXML
    private TableColumn<TestDriveRequest, String> clientColumn;
    @FXML
    private TableColumn<TestDriveRequest, String> phoneColumn;
    @FXML
    private TableColumn<TestDriveRequest, String> carColumn;
    @FXML
    private TableColumn<TestDriveRequest, TestDriveStatus> statusColumn;
    @FXML
    private TableColumn<TestDriveRequest, LocalDateTime> createdColumn;

    @FXML
    private Button editButton;
    @FXML
    private Button statusButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label countLabel;

    @FXML
    private Label detailsTitle;
    @FXML
    private Label detailsClient;
    @FXML
    private Label detailsCar;
    @FXML
    private Label detailsSchedule;
    @FXML
    private Label detailsStatus;
    @FXML
    private Label detailsComment;
    @FXML
    private Label detailsHistory;

    public RequestsController(AppContext context) {
        this.context = context;
        this.requestService = context.requestService();
    }

    /** Вызывается FXMLLoader после того, как все поля с @FXML заполнены элементами из FXML. */
    @FXML
    private void initialize() {
        setupColumns();
        setupFilters();
        setupSorting();

        requestTable.setItems(sortedRequests);
        // Встроенная сортировка TableView: при щелчке по заголовку столбца меняется comparator таблицы,
        // а SortedList пересортировывает строки по этому comparator
        sortedRequests.comparatorProperty().bind(requestTable.comparatorProperty());

        requestTable.setRowFactory(table -> {
            TableRow<TestDriveRequest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    handleEdit();
                }
            });
            return row;
        });

        var noSelection = requestTable.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        statusButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);
        requestTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> showDetails(selected));

        filteredRequests.addListener((ListChangeListener<TestDriveRequest>) change -> updateCount());
        showDetails(null);
        updateCount();
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = Objects.requireNonNull(onDataChanged);
    }

    // ------------------------------------------------------------------ Загрузка данных

    /** Перечитывает заявки из базы данных, сохраняя выбранную строку. */
    public void reload() {
        Long selectedId = selectedRequest().map(TestDriveRequest::getId).orElse(null);
        try {
            requests.setAll(requestService.getAll());
            reloadBrands();
            select(selectedId);
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Загрузка заявок", e);
        }
    }

    private void reloadBrands() {
        Choice<String> current = brandFilter.getValue();
        List<Choice<String>> brands = new ArrayList<>();
        brands.add(Choice.any("Все марки"));
        context.carService().getAll().stream()
                .map(Car::getBrand)
                .distinct()
                .sorted(Formats.russianOrder())
                .forEach(brand -> brands.add(new Choice<>(brand, brand)));
        brandFilter.getItems().setAll(brands);
        brandFilter.setValue(brands.stream()
                .filter(choice -> current != null && Objects.equals(choice.value(), current.value()))
                .findFirst()
                .orElse(brands.get(0)));
    }

    // ------------------------------------------------------------------ Кнопки

    @FXML
    private void handleAdd() {
        try {
            FormDialog.<TestDriveRequest, RequestFormController>show(window(), "request-form.fxml", context,
                            form -> form.setRequest(null))
                    .ifPresent(this::afterSave);
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Новая заявка", e);
        }
    }

    @FXML
    private void handleEdit() {
        Optional<TestDriveRequest> selected = selectedRequest();
        if (selected.isEmpty()) {
            return;
        }
        try {
            // Закрытую заявку нельзя редактировать: сообщаем об этом до открытия формы
            requestService.checkEditable(selected.get());
            FormDialog.<TestDriveRequest, RequestFormController>show(window(), "request-form.fxml", context,
                            form -> form.setRequest(selected.get()))
                    .ifPresent(this::afterSave);
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Изменение заявки", e);
        }
    }

    @FXML
    private void handleChangeStatus() {
        Optional<TestDriveRequest> selected = selectedRequest();
        if (selected.isEmpty()) {
            return;
        }
        TestDriveRequest request = selected.get();
        TestDriveStatus current = request.getStatus();
        if (current.getAllowedTransitions().isEmpty()) {
            Alerts.showInfo(window(), "Смена статуса", "Заявка №" + request.getId() + " закрыта",
                    "Статус \"" + current.getDisplayName() + "\" окончательный. "
                            + TestDriveRequestService.describeAllowedTransitions(current));
            return;
        }

        List<Choice<TestDriveStatus>> options = current.getAllowedTransitions().stream()
                .map(status -> new Choice<>(status.getDisplayName(), status))
                .toList();
        ChoiceDialog<Choice<TestDriveStatus>> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.initOwner(window());
        dialog.setTitle("Смена статуса");
        dialog.setHeaderText("Заявка №" + request.getId() + ": " + request.getClient().getShortName() + ", "
                + request.getCar().getTitle() + "\nТекущий статус: " + current.getDisplayName());
        dialog.setContentText("Новый статус:");
        dialog.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        // Стандартные кнопки ChoiceDialog подписаны по-английски
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText("Сменить статус");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Отмена");

        dialog.showAndWait().ifPresent(choice -> {
            try {
                afterSave(requestService.changeStatus(request.getId(), choice.value()));
            } catch (AutosalonException e) {
                Alerts.showError(window(), "Смена статуса", e);
            }
        });
    }

    @FXML
    private void handleDelete() {
        Optional<TestDriveRequest> selected = selectedRequest();
        if (selected.isEmpty()) {
            return;
        }
        TestDriveRequest request = selected.get();
        boolean confirmed = Alerts.confirm(window(), "Удаление заявки",
                "Удалить заявку №" + request.getId() + "?",
                request.getClient().getFullName() + ", " + request.getCar().getTitle() + ", "
                        + Formats.dateTime(request.getScheduledAt()) + " (" + request.getStatus().getDisplayName()
                        + ").\nЗаявка будет удалена из базы данных без возможности восстановления.",
                "Удалить");
        if (!confirmed) {
            return;
        }
        try {
            requestService.delete(request.getId());
            reload();
            onDataChanged.run();
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Удаление заявки", e);
        }
    }

    @FXML
    private void handleRefresh() {
        reload();
    }

    /** Экспорт ровно того, что сейчас видно в таблице: с учётом поиска, фильтров и сортировки. */
    @FXML
    private void handleExport() {
        if (sortedRequests.isEmpty()) {
            Alerts.showInfo(window(), "Экспорт", "Нет данных для экспорта",
                    "В таблице нет ни одной заявки. Измените условия поиска или фильтры.");
            return;
        }
        File file = FxSupport.csvFileChooser("Экспорт заявок в CSV", "test_drive_requests.csv")
                .showSaveDialog(window());
        if (file == null) {
            return;
        }
        try {
            context.exportService().exportRequests(List.copyOf(sortedRequests), file.toPath());
            Alerts.showInfo(window(), "Экспорт", "Экспорт выполнен",
                    "Выгружено заявок: " + sortedRequests.size() + ".\nФайл: " + file.getAbsolutePath());
        } catch (AutosalonException e) {
            Alerts.showError(window(), "Экспорт заявок", e);
        }
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        brandFilter.getSelectionModel().selectFirst();
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        upcomingOnlyCheck.setSelected(false);
        applyFilter();
    }

    private void afterSave(TestDriveRequest saved) {
        reload();
        select(saved.getId());
        onDataChanged.run();
    }

    // ------------------------------------------------------------------ Настройка таблицы

    private void setupColumns() {
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        scheduledColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getScheduledAt()));
        scheduledColumn.setCellFactory(FxSupport.formattedCell(Formats::dateTime));
        durationColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getDurationMinutes()));
        clientColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getClient().getFullName()));
        clientColumn.setComparator(Formats.russianOrder());
        phoneColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getClient().getPhone()));
        carColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCar().getTitle()));
        carColumn.setComparator(Formats.russianOrder());
        // Статус сравнивается как enum, то есть в порядке жизненного цикла: Новая, Подтверждена, Завершена...
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        statusColumn.setCellFactory(FxSupport.statusCell());
        createdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCreatedAt()));
        createdColumn.setCellFactory(FxSupport.formattedCell(Formats::dateTime));
    }

    private void setupFilters() {
        List<Choice<TestDriveStatus>> statuses = new ArrayList<>();
        statuses.add(Choice.any("Все статусы"));
        for (TestDriveStatus status : TestDriveStatus.values()) {
            statuses.add(new Choice<>(status.getDisplayName(), status));
        }
        statusFilter.getItems().setAll(statuses);
        statusFilter.getSelectionModel().selectFirst();
        brandFilter.getItems().setAll(List.of(Choice.any("Все марки")));
        brandFilter.getSelectionModel().selectFirst();
        filterErrorLabel.managedProperty().bind(filterErrorLabel.textProperty().isNotEmpty());
        filterErrorLabel.visibleProperty().bind(filterErrorLabel.textProperty().isNotEmpty());
        FxSupport.setupDatePicker(fromDatePicker);
        FxSupport.setupDatePicker(toDatePicker);

        // Таблица фильтруется сразу при вводе текста или выборе значения
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        brandFilter.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        fromDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        toDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> applyFilter());
        upcomingOnlyCheck.selectedProperty().addListener((observable, oldValue, newValue) -> applyFilter());
    }

    /** Условие фильтрации собирает сервис, контроллер только передаёт его в FilteredList. */
    private void applyFilter() {
        RequestFilter filter = new RequestFilter(
                searchField.getText(),
                statusFilter.getValue() == null ? null : statusFilter.getValue().value(),
                brandFilter.getValue() == null ? null : brandFilter.getValue().value(),
                fromDatePicker.getValue(),
                toDatePicker.getValue(),
                upcomingOnlyCheck.isSelected());
        try {
            filteredRequests.setPredicate(requestService.buildFilter(filter));
            filterErrorLabel.setText("");
            toDatePicker.getStyleClass().remove("field-error");
        } catch (ValidationException e) {
            filterErrorLabel.setText(e.getMessage());
            if (!toDatePicker.getStyleClass().contains("field-error")) {
                toDatePicker.getStyleClass().add("field-error");
            }
        }
    }

    private void setupSorting() {
        sortColumns.put(TestDriveSortOption.BY_SCHEDULED_AT, scheduledColumn);
        sortColumns.put(TestDriveSortOption.BY_CREATED_AT, createdColumn);
        sortColumns.put(TestDriveSortOption.BY_CLIENT_NAME, clientColumn);
        sortColumns.put(TestDriveSortOption.BY_CAR, carColumn);
        sortColumns.put(TestDriveSortOption.BY_STATUS, statusColumn);
        sortColumns.put(TestDriveSortOption.BY_DURATION, durationColumn);

        sortBox.getItems().setAll(TestDriveSortOption.values());
        sortBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TestDriveSortOption option) {
                return option == null ? "" : option.getDisplayName();
            }

            @Override
            public TestDriveSortOption fromString(String text) {
                return null;
            }
        });
        sortBox.valueProperty().addListener((observable, oldValue, newValue) -> applySort());
        descendingCheck.selectedProperty().addListener((observable, oldValue, newValue) -> applySort());
        sortBox.setValue(TestDriveSortOption.BY_SCHEDULED_AT);
    }

    /**
     * Сортировка из выпадающего списка задаёт порядок сортировки TableView: выбранный столбец и направление.
     * То же самое пользователь может сделать щелчком по заголовку столбца (Shift+щелчок — по нескольким столбцам).
     */
    private void applySort() {
        TestDriveSortOption option = sortBox.getValue();
        if (option == null) {
            return;
        }
        TableColumn<TestDriveRequest, ?> column = sortColumns.get(option);
        column.setSortType(descendingCheck.isSelected()
                ? TableColumn.SortType.DESCENDING
                : TableColumn.SortType.ASCENDING);
        requestTable.getSortOrder().setAll(List.of(column));
        requestTable.sort();
    }

    // ------------------------------------------------------------------ Подробности о выбранной заявке

    /** Связанные данные: клиент и автомобиль заявки показываются полностью, без обращения к БД вручную. */
    private void showDetails(TestDriveRequest request) {
        if (request == null) {
            detailsTitle.setText("Выберите заявку в таблице");
            List.of(detailsClient, detailsCar, detailsSchedule, detailsStatus, detailsComment, detailsHistory)
                    .forEach(label -> label.setText("—"));
            return;
        }
        Client client = request.getClient();
        Car car = request.getCar();
        detailsTitle.setText("Заявка №" + request.getId());
        detailsClient.setText(client.getFullName() + "\nТел.: " + client.getPhone() + ", email: "
                + Formats.orDash(client.getEmail()) + "\nВУ: " + client.getLicenseNumber() + ", возраст: "
                + client.getAge(LocalDate.now()));
        detailsCar.setText(car.getTitle() + ", " + car.getManufactureYear() + ", " + car.getColor()
                + "\nVIN: " + car.getVin() + "\nЦена: " + Formats.money(car.getPrice()) + " руб., "
                + car.getStatus().getDisplayName());
        detailsSchedule.setText(Formats.dateTime(request.getScheduledAt()) + " – " + Formats.time(request.getEndAt())
                + " (" + request.getDurationMinutes() + " мин)");
        detailsStatus.setText(request.getStatus().getDisplayName() + ". "
                + TestDriveRequestService.describeAllowedTransitions(request.getStatus()));
        detailsComment.setText(Formats.orDash(request.getComment()));
        detailsHistory.setText("Создана " + Formats.dateTime(request.getCreatedAt()) + ", изменена "
                + Formats.dateTime(request.getUpdatedAt()));
    }

    private void updateCount() {
        countLabel.setText("Показано заявок: " + filteredRequests.size() + " из " + requests.size());
    }

    private Optional<TestDriveRequest> selectedRequest() {
        return Optional.ofNullable(requestTable.getSelectionModel().getSelectedItem());
    }

    private void select(Long id) {
        if (id == null) {
            return;
        }
        sortedRequests.stream()
                .filter(request -> request.getId().equals(id))
                .findFirst()
                .ifPresent(request -> {
                    requestTable.getSelectionModel().select(request);
                    requestTable.scrollTo(request);
                });
    }

    private Window window() {
        return requestTable.getScene() == null ? null : requestTable.getScene().getWindow();
    }
}
