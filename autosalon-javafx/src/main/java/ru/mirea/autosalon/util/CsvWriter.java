package ru.mirea.autosalon.util;

import ru.mirea.autosalon.exception.ExportException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Запись таблицы в CSV-файл.
 * Разделитель «;» и метка BOM в начале файла нужны, чтобы русский Excel сразу открыл файл правильно.
 */
public final class CsvWriter {

    private static final String SEPARATOR = ";";
    private static final char UTF8_BOM = '﻿';

    private CsvWriter() {
    }

    /**
     * @param file    путь, который пользователь выбрал в FileChooser
     * @param headers заголовки столбцов
     * @param rows    строки таблицы
     * @throws ExportException если файл не удалось записать (открыт в Excel, нет прав на запись и т.п.)
     */
    public static void write(Path file, List<String> headers, List<List<String>> rows) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(UTF8_BOM);
            writer.write(toCsvLine(headers));
            writer.newLine();
            for (List<String> row : rows) {
                writer.write(toCsvLine(row));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new ExportException("не удалось записать файл " + file.toAbsolutePath()
                    + ". Возможно, файл открыт в другой программе или нет прав на запись в папку.", e);
        }
    }

    private static String toCsvLine(List<String> values) {
        return values.stream().map(CsvWriter::escape).collect(Collectors.joining(SEPARATOR));
    }

    /** Значение с разделителем, кавычками или переносом строки заключается в кавычки по правилам CSV. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
