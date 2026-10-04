package ru.mirea.autosalon.ui;

import ru.mirea.autosalon.service.ExportService;
import ru.mirea.autosalon.util.DataExporter;

import java.nio.file.Path;
import java.util.List;

/**
 * Меню «Экспорт данных». Для каждого доступного формата (DataExporter) создаётся свой пункт меню.
 */
public class ExportMenu extends AbstractMenu {

    /** Файлы сохраняются в папку export рядом с местом запуска программы. */
    private static final Path EXPORT_DIRECTORY = Path.of("export");

    private final ExportService exportService;

    public ExportMenu(ConsoleInput input, ExportService exportService, List<DataExporter> exporters) {
        super("ЭКСПОРТ ДАННЫХ", input);
        this.exportService = exportService;
        for (DataExporter exporter : exporters) {
            addItem("Экспорт в " + exporter.getFormatName(), () -> export(exporter));
        }
    }

    private void export(DataExporter exporter) {
        List<Path> files = exportService.export(exporter, EXPORT_DIRECTORY);
        System.out.println("Экспорт в " + exporter.getFormatName() + " выполнен. Созданные файлы:");
        files.forEach(file -> System.out.println("  " + file));
    }
}
