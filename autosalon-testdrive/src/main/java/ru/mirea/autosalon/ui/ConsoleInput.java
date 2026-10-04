package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.exception.InputClosedException;
import ru.mirea.autosalon.util.Formats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Безопасное чтение данных из консоли. При некорректном вводе (текст вместо числа, неверный формат даты)
 * выводится сообщение об ошибке и значение запрашивается повторно — программа не завершается аварийно.
 */
public class ConsoleInput {

    private final BufferedReader reader;
    private final PrintStream out;

    public ConsoleInput() {
        this(System.in, System.out);
    }

    public ConsoleInput(InputStream in, PrintStream out) {
        this.reader = new BufferedReader(new InputStreamReader(in, detectConsoleCharset()));
        this.out = out;
    }

    /**
     * Кодировка, в которой консоль передаёт введённый текст. В окне Windows это обычно cp866,
     * в IntelliJ IDEA — UTF-8. Java сообщает её через свойство stdout.encoding (sun.stdout.encoding в Java 17).
     */
    private static Charset detectConsoleCharset() {
        for (String property : new String[]{"stdout.encoding", "sun.stdout.encoding"}) {
            String name = System.getProperty(property);
            if (name != null && Charset.isSupported(name)) {
                return Charset.forName(name);
            }
        }
        return Charset.defaultCharset();
    }

    /** Читает строку (без пробелов по краям). Подсказка выводится в формате «prompt: ». */
    public String readLine(String prompt) {
        out.print(prompt + ": ");
        out.flush();
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.trim();
        } catch (IOException e) {
            throw new InputClosedException(e);
        }
    }

    /** Необязательный текст: пустая строка превращается в null. */
    public String readOptionalLine(String prompt) {
        String value = readLine(prompt);
        return value.isEmpty() ? null : value;
    }

    /** Текст с возможностью оставить текущее значение (Enter). */
    public String readLineOrKeep(String prompt, String current) {
        String value = readLine(prompt + " [" + Formats.orDash(current) + "]");
        return value.isEmpty() ? current : value;
    }

    /** Необязательный текст при редактировании: Enter — оставить, «-» — очистить. */
    public String readOptionalLineOrKeep(String prompt, String current) {
        String value = readLine(prompt + " [" + Formats.orDash(current) + "] (\"-\" - очистить)");
        if (value.isEmpty()) {
            return current;
        }
        return value.equals("-") ? null : value;
    }

    public int readInt(String prompt) {
        return readParsed(prompt, Integer::parseInt, "значение должно быть целым числом.");
    }

    public int readIntOrKeep(String prompt, int current) {
        return readParsedOrKeep(prompt, Integer::parseInt, "значение должно быть целым числом.",
                current, String::valueOf);
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // сообщение об ошибке выводится ниже
            }
            out.println("Ошибка: введите целое число от " + min + " до " + max + ".");
        }
    }

    /** Номер пункта меню от 0 до maxItem. */
    public int readMenuChoice(int maxItem) {
        while (true) {
            String value = readLine("Выберите действие");
            try {
                int choice = Integer.parseInt(value);
                if (choice >= 0 && choice <= maxItem) {
                    return choice;
                }
            } catch (NumberFormatException ignored) {
                // сообщение об ошибке выводится ниже
            }
            out.println("Ошибка: введите номер пункта меню от 0 до " + maxItem + ".");
        }
    }

    /** Идентификатор записи: целое положительное число. */
    public long readId(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                long id = Long.parseLong(value);
                if (id > 0) {
                    return id;
                }
                out.println("Ошибка: ID должен быть положительным числом.");
            } catch (NumberFormatException e) {
                out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    public long readIdOrKeep(String prompt, long current) {
        while (true) {
            String value = readLine(prompt + " [" + current + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                long id = Long.parseLong(value);
                if (id > 0) {
                    return id;
                }
                out.println("Ошибка: ID должен быть положительным числом.");
            } catch (NumberFormatException e) {
                out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        return readParsed(prompt + " (ДД.ММ.ГГГГ)", text -> LocalDate.parse(text, Formats.DATE),
                "введите существующую дату в формате ДД.ММ.ГГГГ, например 15.10.2026.");
    }

    public LocalDate readDateOrKeep(String prompt, LocalDate current) {
        return readParsedOrKeep(prompt + " (ДД.ММ.ГГГГ)", text -> LocalDate.parse(text, Formats.DATE),
                "введите существующую дату в формате ДД.ММ.ГГГГ, например 15.10.2026.",
                current, Formats::date);
    }

    public LocalDateTime readDateTime(String prompt) {
        return readParsed(prompt + " (ДД.ММ.ГГГГ ЧЧ:ММ)", text -> LocalDateTime.parse(text, Formats.DATE_TIME),
                "введите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ, например 15.10.2026 14:30.");
    }

    public LocalDateTime readDateTimeOrKeep(String prompt, LocalDateTime current) {
        return readParsedOrKeep(prompt + " (ДД.ММ.ГГГГ ЧЧ:ММ)",
                text -> LocalDateTime.parse(text, Formats.DATE_TIME),
                "введите дату и время в формате ДД.ММ.ГГГГ ЧЧ:ММ, например 15.10.2026 14:30.",
                current, Formats::dateTime);
    }

    /** Денежная сумма: допускаются пробелы между разрядами и запятая, например «3 850 000» или «999,99». */
    public BigDecimal readMoney(String prompt) {
        return readParsed(prompt, ConsoleInput::parseMoney, "введите сумму числом, например 3850000.");
    }

    public BigDecimal readMoneyOrKeep(String prompt, BigDecimal current) {
        return readParsedOrKeep(prompt, ConsoleInput::parseMoney, "введите сумму числом, например 3850000.",
                current, Formats::money);
    }

    /** Вопрос «да/нет». */
    public boolean confirm(String question) {
        while (true) {
            String answer = readLine(question + " (д/н)").toLowerCase();
            switch (answer) {
                case "д", "да", "y", "yes" -> {
                    return true;
                }
                case "н", "нет", "n", "no" -> {
                    return false;
                }
                default -> out.println("Ошибка: введите \"д\" (да) или \"н\" (нет).");
            }
        }
    }

    /** Выбор одного варианта из списка по номеру. */
    public <T> T choose(String title, List<T> options, Function<T, String> label) {
        printOptions(title, options, label);
        int index = readIntInRange("Ваш выбор", 1, options.size());
        return options.get(index - 1);
    }

    /** Выбор варианта из списка с возможностью оставить текущий (Enter). */
    public <T> T chooseOrKeep(String title, List<T> options, Function<T, String> label, T current) {
        printOptions(title, options, label);
        while (true) {
            String value = readLine("Ваш выбор [" + label.apply(current) + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                int index = Integer.parseInt(value);
                if (index >= 1 && index <= options.size()) {
                    return options.get(index - 1);
                }
            } catch (NumberFormatException ignored) {
                // сообщение об ошибке выводится ниже
            }
            out.println("Ошибка: введите целое число от 1 до " + options.size() + " или нажмите Enter.");
        }
    }

    private <T> void printOptions(String title, List<T> options, Function<T, String> label) {
        out.println(title + ":");
        for (int i = 0; i < options.size(); i++) {
            out.println("  " + (i + 1) + ". " + label.apply(options.get(i)));
        }
    }

    private <T> T readParsed(String prompt, Function<String, T> parser, String errorMessage) {
        while (true) {
            String value = readLine(prompt);
            try {
                return parser.apply(value);
            } catch (RuntimeException e) {
                out.println("Ошибка: " + errorMessage);
            }
        }
    }

    private <T> T readParsedOrKeep(String prompt, Function<String, T> parser, String errorMessage,
                                   T current, Function<T, String> formatter) {
        while (true) {
            String value = readLine(prompt + " [" + formatter.apply(current) + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                return parser.apply(value);
            } catch (RuntimeException e) {
                out.println("Ошибка: " + errorMessage);
            }
        }
    }

    private static BigDecimal parseMoney(String text) {
        return new BigDecimal(text.replaceAll("[\\s ]", "").replace(',', '.'));
    }
}
