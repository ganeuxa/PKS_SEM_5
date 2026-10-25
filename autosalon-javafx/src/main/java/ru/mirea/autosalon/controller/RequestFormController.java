package ru.mirea.autosalon.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.mirea.autosalon.AppContext;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.service.TestDriveRequestService;
import ru.mirea.autosalon.ui.FxSupport;
import ru.mirea.autosalon.util.Formats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Форма добавления и редактирования заявки на тест-драйв (request-form.fxml).
 * <p>
 * Клиент и автомобиль выбираются из выпадающих списков (ComboBox), а не вводятся по ID.
 * В список автомобилей попадают только машины «В наличии»: записать на тест-драйв проданную нельзя.
 */
public class RequestFormController extends AbstractFormController<TestDriveRequest> {

    private static final int TIME_STEP_MINUTES = 15;
    private static final String DEFAULT_TIME = "10:00";
    private static final String DEFAULT_DURATION = "30";

    private final AppContext context;

    /** Редактируемая заявка; null — создаётся новая. */
    private TestDriveRequest request;

    @FXML
    private ComboBox<Client> clientBox;
    @FXML
    private ComboBox<Car> carBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> timeBox;
    @FXML
    private TextField durationField;
    @FXML
    private TextArea commentArea;
    @FXML
    private Label rulesLabel;

    public RequestFormController(AppContext context) {
        this.context = context;
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        clientBox.setConverter(FxSupport.entityConverter());
        carBox.setConverter(FxSupport.entityConverter());
        FxSupport.setupDatePicker(datePicker);
        timeBox.getItems().setAll(timeSlots());
        rulesLabel.setText("Часы работы салона: " + TestDriveRequestService.OPENING_TIME + "–"
                + TestDriveRequestService.CLOSING_TIME + ". Длительность: " + TestDriveRequestService.MIN_DURATION_MINUTES
                + "–" + TestDriveRequestService.MAX_DURATION_MINUTES + " мин. Запись не более чем на "
                + TestDriveRequestService.MAX_DAYS_AHEAD + " дней вперёд, у клиента не больше "
                + TestDriveRequestService.MAX_ACTIVE_REQUESTS_PER_CLIENT + " активных заявок.");
    }

    /** Заполняет форму. Вызывается до показа окна. */
    public void setRequest(TestDriveRequest request) {
        this.request = request;
        clientBox.getItems().setAll(context.clientService().getAll());

        List<Car> cars = new ArrayList<>(context.carService().getAvailableForTestDrive());
        if (request == null) {
            carBox.getItems().setAll(cars);
            datePicker.setValue(LocalDate.now().plusDays(1));
            timeBox.getEditor().setText(DEFAULT_TIME);
            durationField.setText(DEFAULT_DURATION);
            return;
        }

        // Текущий автомобиль заявки оставляем в списке, даже если его статус уже изменился
        if (!cars.contains(request.getCar())) {
            cars.add(0, request.getCar());
        }
        carBox.getItems().setAll(cars);
        clientBox.setValue(request.getClient());
        clientBox.setDisable(true);
        carBox.setValue(request.getCar());
        datePicker.setValue(request.getScheduledAt().toLocalDate());
        timeBox.getEditor().setText(Formats.time(request.getScheduledAt()));
        durationField.setText(String.valueOf(request.getDurationMinutes()));
        commentArea.setText(request.getComment());
    }

    @Override
    public String getTitle() {
        return request == null ? "Новая заявка на тест-драйв" : "Изменение заявки №" + request.getId();
    }

    @Override
    protected TestDriveRequest doSave() {
        Client client = requireChoice(clientBox, "Клиент");
        Car car = requireChoice(carBox, "Автомобиль");
        LocalDate date = requireDate(datePicker, "Дата тест-драйва");
        LocalTime time = requireTime(timeBox, "Время начала");
        int duration = requireInt(durationField, "Длительность, мин",
                TestDriveRequestService.MIN_DURATION_MINUTES, TestDriveRequestService.MAX_DURATION_MINUTES);
        String comment = optionalText(commentArea, "Комментарий", TestDriveRequestService.MAX_COMMENT_LENGTH);
        LocalDateTime scheduledAt = date.atTime(time);

        // Бизнес-правила (расписание, занятость автомобиля, лимит заявок) проверяет сервис
        TestDriveRequestService service = context.requestService();
        if (request == null) {
            return service.create(client.getId(), car.getId(), scheduledAt, duration, comment);
        }
        return service.update(request.getId(), car.getId(), scheduledAt, duration, comment);
    }

    /** Варианты времени начала с шагом 15 минут в часы работы салона. Можно ввести и своё время. */
    private static List<String> timeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime time = TestDriveRequestService.OPENING_TIME;
        LocalTime last = TestDriveRequestService.CLOSING_TIME.minusMinutes(TestDriveRequestService.MIN_DURATION_MINUTES);
        while (!time.isAfter(last)) {
            slots.add(time.toString());
            time = time.plusMinutes(TIME_STEP_MINUTES);
        }
        return slots;
    }
}
