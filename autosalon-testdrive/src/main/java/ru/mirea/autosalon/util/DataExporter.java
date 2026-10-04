package ru.mirea.autosalon.util;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт данных в файл определённого формата.
 * <p>
 * Реализации: {@link ExcelExporter} (.xlsx) и {@link CsvExporter} (.csv). Меню экспорта работает
 * со списком DataExporter и вызывает {@link #export}, не зная конкретного класса, — это полиморфизм.
 */
public interface DataExporter {

    DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Название формата для меню, например «Excel (.xlsx)». */
    String getFormatName();

    /**
     * Выгружает данные в папку directory (создаёт её при необходимости).
     *
     * @return пути к созданным файлам
     * @throws ru.mirea.autosalon.exception.ExportException если файл не удалось записать
     */
    List<Path> export(ExportData data, Path directory);

    /** Общее начало имени файлов: autosalon_testdrive_2026-10-04_15-30-00. */
    default String buildBaseFileName(ExportData data) {
        return "autosalon_testdrive_" + FILE_TIMESTAMP.format(data.getGeneratedAt());
    }
}
