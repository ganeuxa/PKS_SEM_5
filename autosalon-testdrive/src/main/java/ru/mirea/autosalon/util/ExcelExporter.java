package ru.mirea.autosalon.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.autosalon.exception.ExportException;
import ru.mirea.autosalon.model.Car;
import ru.mirea.autosalon.model.Client;
import ru.mirea.autosalon.model.TestDriveRequest;
import ru.mirea.autosalon.model.TestDriveStatistics;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Экспорт в Excel (.xlsx) с помощью библиотеки Apache POI.
 * Книга содержит листы «Заявки на тест-драйв», «Клиенты», «Автомобили» и «Статистика».
 */
public class ExcelExporter implements DataExporter {

    /** Максимальная ширина столбца: 60 символов (POI измеряет ширину в 1/256 символа). */
    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    @Override
    public String getFormatName() {
        return "Excel (.xlsx)";
    }

    @Override
    public List<Path> export(ExportData data, Path directory) {
        Path file = directory.resolve(buildBaseFileName(data) + ".xlsx").toAbsolutePath();
        try {
            Files.createDirectories(directory);
            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream output = Files.newOutputStream(file)) {
                Styles styles = new Styles(workbook);
                writeRequests(workbook.createSheet("Заявки на тест-драйв"), data, styles);
                writeClients(workbook.createSheet("Клиенты"), data, styles);
                writeCars(workbook.createSheet("Автомобили"), data, styles);
                writeStatistics(workbook.createSheet("Статистика"), data.getStatistics(), styles);
                workbook.write(output);
            }
        } catch (IOException e) {
            throw new ExportException("не удалось записать файл " + file + " (" + e.getMessage()
                    + "). Возможно, файл открыт в другой программе или нет прав на запись.", e);
        }
        return List.of(file);
    }

    private void writeRequests(Sheet sheet, ExportData data, Styles styles) {
        String[] headers = {"№ заявки", "Клиент", "Телефон клиента", "Автомобиль", "VIN", "Начало тест-драйва",
                "Окончание", "Длительность, мин", "Статус", "Комментарий", "Создана", "Изменена"};
        writeHeader(sheet, headers, styles);
        int rowIndex = 1;
        for (TestDriveRequest request : data.getRequests()) {
            Row row = sheet.createRow(rowIndex++);
            setNumber(row, 0, request.getId(), styles.integer);
            setText(row, 1, request.getClient().getFullName(), styles.text);
            setText(row, 2, request.getClient().getPhone(), styles.text);
            setText(row, 3, request.getCar().getTitle() + " " + request.getCar().getManufactureYear(), styles.text);
            setText(row, 4, request.getCar().getVin(), styles.text);
            setDateTime(row, 5, request.getScheduledAt(), styles.dateTime);
            setDateTime(row, 6, request.getEndAt(), styles.dateTime);
            setNumber(row, 7, request.getDurationMinutes(), styles.integer);
            setText(row, 8, request.getStatus().getDisplayName(), styles.text);
            setText(row, 9, request.getComment(), styles.text);
            setDateTime(row, 10, request.getCreatedAt(), styles.dateTime);
            setDateTime(row, 11, request.getUpdatedAt(), styles.dateTime);
        }
        finishTable(sheet, headers.length, rowIndex);
    }

    private void writeClients(Sheet sheet, ExportData data, Styles styles) {
        Map<Long, Long> requestsPerClient = data.getRequests().stream()
                .collect(Collectors.groupingBy(request -> request.getClient().getId(), Collectors.counting()));
        LocalDate today = data.getGeneratedAt().toLocalDate();
        String[] headers = {"ID", "ФИО", "Телефон", "Email", "Дата рождения", "Возраст, лет", "Номер ВУ",
                "Дата регистрации", "Заявок на тест-драйв"};
        writeHeader(sheet, headers, styles);
        int rowIndex = 1;
        for (Client client : data.getClients()) {
            Row row = sheet.createRow(rowIndex++);
            setNumber(row, 0, client.getId(), styles.integer);
            setText(row, 1, client.getFullName(), styles.text);
            setText(row, 2, client.getPhone(), styles.text);
            setText(row, 3, client.getEmail(), styles.text);
            setDate(row, 4, client.getBirthDate(), styles.date);
            setNumber(row, 5, client.getAge(today), styles.integer);
            setText(row, 6, client.getLicenseNumber(), styles.text);
            setDateTime(row, 7, client.getRegisteredAt(), styles.dateTime);
            setNumber(row, 8, requestsPerClient.getOrDefault(client.getId(), 0L), styles.integer);
        }
        finishTable(sheet, headers.length, rowIndex);
    }

    private void writeCars(Sheet sheet, ExportData data, Styles styles) {
        Map<Long, Long> requestsPerCar = data.getRequests().stream()
                .collect(Collectors.groupingBy(request -> request.getCar().getId(), Collectors.counting()));
        String[] headers = {"ID", "Марка", "Модель", "Год выпуска", "VIN", "Цвет", "Цена, руб.", "Статус",
                "Заявок на тест-драйв"};
        writeHeader(sheet, headers, styles);
        int rowIndex = 1;
        for (Car car : data.getCars()) {
            Row row = sheet.createRow(rowIndex++);
            setNumber(row, 0, car.getId(), styles.integer);
            setText(row, 1, car.getBrand(), styles.text);
            setText(row, 2, car.getModel(), styles.text);
            setNumber(row, 3, car.getManufactureYear(), styles.integer);
            setText(row, 4, car.getVin(), styles.text);
            setText(row, 5, car.getColor(), styles.text);
            setMoney(row, 6, car.getPrice(), styles.money);
            setText(row, 7, car.getStatus().getDisplayName(), styles.text);
            setNumber(row, 8, requestsPerCar.getOrDefault(car.getId(), 0L), styles.integer);
        }
        finishTable(sheet, headers.length, rowIndex);
    }

    private void writeStatistics(Sheet sheet, TestDriveStatistics statistics, Styles styles) {
        Row titleRow = sheet.createRow(0);
        setText(titleRow, 0, "Статистика автосалона на " + Formats.dateTime(statistics.getGeneratedAt()),
                styles.title);

        int rowIndex = 2;
        Row header = sheet.createRow(rowIndex++);
        setText(header, 0, "Показатель", styles.header);
        setText(header, 1, "Значение", styles.header);
        for (Map.Entry<String, Object> indicator : statistics.getIndicators().entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            setText(row, 0, indicator.getKey(), styles.text);
            Object value = indicator.getValue();
            if (value instanceof Double number) {
                setNumber(row, 1, number, styles.decimal);
            } else if (value instanceof Number number) {
                setNumber(row, 1, number.doubleValue(), styles.integer);
            } else {
                setText(row, 1, String.valueOf(value), styles.text);
            }
        }

        rowIndex++;
        Row brandHeader = sheet.createRow(rowIndex++);
        setText(brandHeader, 0, "Марка автомобиля", styles.header);
        setText(brandHeader, 1, "Заявок на тест-драйв", styles.header);
        for (Map.Entry<String, Long> brand : statistics.getRequestsByBrand().entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            setText(row, 0, brand.getKey(), styles.text);
            setNumber(row, 1, brand.getValue(), styles.integer);
        }
        autoSizeColumns(sheet, 2);
    }

    private void writeHeader(Sheet sheet, String[] headers, Styles styles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            setText(row, i, headers[i], styles.header);
        }
    }

    /** Закрепляет строку заголовка, включает автофильтр и подбирает ширину столбцов. */
    private void finishTable(Sheet sheet, int columnCount, int rowCount) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rowCount - 1, 0), 0, columnCount - 1));
        autoSizeColumns(sheet, columnCount);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Запас под жирный шрифт заголовка и кнопку автофильтра, но не шире MAX_COLUMN_WIDTH
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 5 * 256, MAX_COLUMN_WIDTH));
        }
    }

    private static void setText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void setNumber(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private static void setDate(Row row, int column, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private static void setDateTime(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    /** Стили ячеек создаются один раз на книгу: Excel ограничивает их количество. */
    private static final class Styles {

        private final CellStyle header;
        private final CellStyle title;
        private final CellStyle text;
        private final CellStyle integer;
        private final CellStyle decimal;
        private final CellStyle money;
        private final CellStyle date;
        private final CellStyle dateTime;

        private Styles(Workbook workbook) {
            DataFormat format = workbook.createDataFormat();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header = bordered(workbook);
            header.setFont(headerFont);
            header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            text = bordered(workbook);
            integer = bordered(workbook);
            integer.setDataFormat(format.getFormat("0"));
            decimal = bordered(workbook);
            decimal.setDataFormat(format.getFormat("0.0"));
            money = bordered(workbook);
            money.setDataFormat(format.getFormat("#,##0"));
            date = bordered(workbook);
            date.setDataFormat(format.getFormat("dd.mm.yyyy"));
            dateTime = bordered(workbook);
            dateTime.setDataFormat(format.getFormat("dd.mm.yyyy hh:mm"));
        }

        private static CellStyle bordered(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setVerticalAlignment(VerticalAlignment.TOP);
            return style;
        }
    }
}
