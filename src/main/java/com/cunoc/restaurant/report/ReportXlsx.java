package com.cunoc.restaurant.report;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.time.temporal.Temporal;
import java.util.List;

/**
 * Serializa cualquier fila de reporte a un .xlsx real, leyendo los componentes del record
 * igual que {@link ReportCsv}: el record ya declara sus columnas y su orden, asi que los
 * nueve endpoints comparten una sola linea de exportacion.
 *
 * A diferencia del CSV, los numeros viajan como numeros, no como texto: quien reciba el
 * archivo puede sumar la columna sin convertirla antes.
 */
public final class ReportXlsx
{
    private ReportXlsx()
    {
    }

    public static byte[] of(String sheetName, Class<? extends Record> rowType, List<? extends Record> rows)
    {
        var components = rowType.getRecordComponents();

        try (var workbook = new XSSFWorkbook();
             var out      = new ByteArrayOutputStream())
        {
            var sheet  = workbook.createSheet(sheetName);
            var header = sheet.createRow(0);
            var bold   = headerStyle(workbook);

            for (int column = 0; column < components.length; column++)
            {
                var cell = header.createCell(column);
                cell.setCellValue(toSnakeCase(components[column].getName()));
                cell.setCellStyle(bold);
            }

            int rowIndex = 1;
            for (Record row : rows)
            {
                var sheetRow = sheet.createRow(rowIndex++);
                for (int column = 0; column < components.length; column++)
                {
                    write(sheetRow, column, ReportCsv.read(components[column], row));
                }
            }

            for (int column = 0; column < components.length; column++)
            {
                sheet.autoSizeColumn(column);
            }

            workbook.write(out);

            return out.toByteArray();
        }
        catch (IOException exception)
        {
            // El libro se escribe en memoria: un IOException aqui no es una condicion de
            // negocio, es que no hay memoria para el arreglo de bytes.
            throw new UncheckedIOException(
                    "No se pudo generar el Excel del reporte " + sheetName, exception);
        }
    }

    private static void write(Row row, int column, Object value)
    {
        var cell = row.createCell(column);

        if (value == null)
        {
            return;
        }

        switch (value)
        {
            case Number number     -> cell.setCellValue(number.doubleValue());
            case Boolean flag      -> cell.setCellValue(flag);
            case Temporal instante -> cell.setCellValue(instante.toString());
            default                -> cell.setCellValue(String.valueOf(value));
        }
    }

    private static CellStyle headerStyle(Workbook workbook)
    {
        var font = workbook.createFont();
        font.setBold(true);

        var style = workbook.createCellStyle();
        style.setFont(font);

        return style;
    }

    /** Misma forma que el JSON y que el CSV, que salen en snake_case. */
    private static String toSnakeCase(String name)
    {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
