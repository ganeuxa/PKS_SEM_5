package ru.mirea.autosalon.util;

import java.math.BigDecimal;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.Locale;

/**
 * Единые форматы дат, денег и чисел для консоли, сообщений об ошибках и экспорта.
 */
public final class Formats {

    public static final Locale RUSSIAN = Locale.forLanguageTag("ru-RU");

    /** Формат даты для ввода и вывода: 15.10.2026. STRICT не пропускает несуществующие даты вроде 31.02. */
    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Формат даты и времени для ввода и вывода: 15.10.2026 14:30. */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Formats() {
    }

    public static String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String time(LocalDateTime value) {
        return value == null ? "" : TIME.format(value);
    }

    /** Сумма с разделителями разрядов: 3 850 000. */
    public static String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(RUSSIAN);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(value);
    }

    /** Дробное число с одним знаком после запятой: 42,9. */
    public static String decimal(double value) {
        return String.format(RUSSIAN, "%.1f", value);
    }

    /** Пустые значения выводятся прочерком. */
    public static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** Склонение существительного после числа: 1 год, 2 года, 5 лет. */
    public static String plural(long number, String one, String few, String many) {
        long n = Math.abs(number) % 100;
        if (n >= 11 && n <= 14) {
            return many;
        }
        return switch ((int) (n % 10)) {
            case 1 -> one;
            case 2, 3, 4 -> few;
            default -> many;
        };
    }

    /** Сравнение строк по правилам русского алфавита (например, «ё» стоит рядом с «е»). */
    public static Comparator<String> russianOrder() {
        Collator collator = Collator.getInstance(RUSSIAN);
        return collator::compare;
    }
}
