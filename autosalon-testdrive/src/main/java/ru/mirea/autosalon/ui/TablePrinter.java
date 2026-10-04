package ru.mirea.autosalon.ui;

import java.util.List;

/**
 * Вывод данных в консоль в виде таблицы с рамкой из символов + - |.
 * Ширина столбца подбирается по самому длинному значению, но не больше {@link #MAX_COLUMN_WIDTH}.
 */
public final class TablePrinter {

    private static final int MAX_COLUMN_WIDTH = 40;

    private TablePrinter() {
    }

    public static void print(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], Math.min(MAX_COLUMN_WIDTH, cell(row, i).length()));
            }
        }

        String border = buildBorder(widths);
        System.out.println(border);
        System.out.println(buildRow(headers, widths));
        System.out.println(border);
        for (List<String> row : rows) {
            System.out.println(buildRow(row, widths));
        }
        System.out.println(border);
    }

    /** Обрезает длинный текст и добавляет «...». */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String oneLine = value.replace('\n', ' ').replace('\r', ' ');
        return oneLine.length() <= maxLength ? oneLine : oneLine.substring(0, maxLength - 3) + "...";
    }

    private static String cell(List<String> row, int index) {
        String value = index < row.size() ? row.get(index) : null;
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private static String buildBorder(int[] widths) {
        StringBuilder border = new StringBuilder("+");
        for (int width : widths) {
            border.append("-".repeat(width + 2)).append('+');
        }
        return border.toString();
    }

    private static String buildRow(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String value = truncate(cell(cells, i), widths[i]);
            line.append(' ').append(value).append(" ".repeat(widths[i] - value.length())).append(" |");
        }
        return line.toString();
    }
}
