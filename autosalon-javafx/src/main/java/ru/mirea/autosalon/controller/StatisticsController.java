package ru.mirea.autosalon.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import ru.mirea.autosalon.AppContext;
import ru.mirea.autosalon.exception.AutosalonException;
import ru.mirea.autosalon.model.TestDriveStatistics;
import ru.mirea.autosalon.model.TestDriveStatus;
import ru.mirea.autosalon.ui.Alerts;
import ru.mirea.autosalon.util.Formats;

import java.util.List;
import java.util.Map;

/**
 * Вкладка «Статистика» (statistics-tab.fxml). Показатели рассчитывает
 * {@link ru.mirea.autosalon.service.StatisticsService} (Stream API), контроллер только выводит их:
 * карточки с главными показателями, таблица всех показателей и два графика.
 */
public class StatisticsController {

    /** Показатели, которые выводятся крупными карточками вверху вкладки. */
    private static final List<String> CARD_INDICATORS = List.of(
            "Всего заявок на тест-драйв",
            "Активных заявок (новые и подтверждённые)",
            "Предстоящих тест-драйвов",
            "Доля состоявшихся тест-драйвов среди закрытых заявок, %",
            "Средняя длительность тест-драйва, мин",
            "Всего клиентов");

    private final AppContext context;

    @FXML
    private Label generatedAtLabel;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private TableView<Map.Entry<String, Object>> indicatorTable;
    @FXML
    private TableColumn<Map.Entry<String, Object>, String> indicatorNameColumn;
    @FXML
    private TableColumn<Map.Entry<String, Object>, String> indicatorValueColumn;
    @FXML
    private PieChart statusChart;
    @FXML
    private BarChart<String, Number> brandChart;

    public StatisticsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        indicatorNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getKey()));
        indicatorValueColumn.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(formatValue(cell.getValue().getValue())));
        indicatorTable.setSortPolicy(table -> false);
        brandChart.setAnimated(false);
        statusChart.setAnimated(false);
    }

    /** Пересчитывает статистику по актуальным данным из базы. */
    public void reload() {
        TestDriveStatistics statistics;
        try {
            statistics = context.statisticsService().collect();
        } catch (AutosalonException e) {
            Alerts.showError(cardsPane.getScene() == null ? null : cardsPane.getScene().getWindow(),
                    "Расчёт статистики", e);
            return;
        }
        generatedAtLabel.setText("Рассчитано " + Formats.dateTime(statistics.getGeneratedAt()));
        showCards(statistics.getIndicators());
        indicatorTable.getItems().setAll(statistics.getIndicators().entrySet());
        showStatusChart(statistics.getRequestsByStatus());
        showBrandChart(statistics.getRequestsByBrand());
    }

    @FXML
    private void handleRefresh() {
        reload();
    }

    private void showCards(Map<String, Object> indicators) {
        cardsPane.getChildren().clear();
        for (String name : CARD_INDICATORS) {
            Label value = new Label(formatValue(indicators.get(name)));
            value.getStyleClass().add("stat-value");
            Label caption = new Label(name);
            caption.getStyleClass().add("stat-caption");
            caption.setWrapText(true);
            VBox card = new VBox(value, caption);
            card.getStyleClass().add("stat-card");
            cardsPane.getChildren().add(card);
        }
    }

    private void showStatusChart(Map<TestDriveStatus, Long> requestsByStatus) {
        statusChart.getData().clear();
        requestsByStatus.forEach((status, count) -> {
            if (count > 0) {
                statusChart.getData().add(new PieChart.Data(status.getDisplayName() + " (" + count + ")", count));
            }
        });
    }

    private void showBrandChart(Map<String, Long> requestsByBrand) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Заявок на тест-драйв");
        requestsByBrand.forEach((brand, count) -> series.getData().add(new XYChart.Data<>(brand, count)));
        brandChart.getData().setAll(List.of(series));
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof Double number) {
            return Formats.decimal(number);
        }
        return String.valueOf(value);
    }
}
