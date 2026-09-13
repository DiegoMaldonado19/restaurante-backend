package com.cunoc.restaurant.report;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Serializa cualquier fila de reporte a CSV leyendo los componentes del record. Es lo que
 * permite que los nueve endpoints compartan una sola linea de exportacion en vez de
 * nueve serializadores: el record ya declara sus columnas y su orden.
 *
 * Se escribe a mano y sin libreria: son treinta lineas contra una dependencia nueva.
 */
public final class ReportCsv
{
    /** Marca de orden de bytes: sin ella Excel abre los acentos como caracteres sueltos. */
    private static final String BOM = "\uFEFF";

    private ReportCsv()
    {
    }

    public static String of(Class<? extends Record> rowType, List<? extends Record> rows)
    {
        var components = rowType.getRecordComponents();
        var out        = new StringBuilder(BOM);

        out.append(Stream.of(components)
                .map(component -> toSnakeCase(component.getName()))
                .collect(Collectors.joining(",")))
           .append("\r\n");

        for (Record row : rows)
        {
            out.append(Stream.of(components)
                    .map(component -> escape(read(component, row)))
                    .collect(Collectors.joining(",")))
               .append("\r\n");
        }

        return out.toString();
    }

    private static Object read(RecordComponent component, Record row)
    {
        try
        {
            return component.getAccessor().invoke(row);
        }
        catch (ReflectiveOperationException exception)
        {
            // El accesor de un record publico siempre es invocable: si falla, es un error
            // de programacion y no una condicion de negocio que valga la pena propagar.
            throw new IllegalStateException(
                    "No se pudo leer el componente " + component.getName()
                            + " de " + row.getClass().getName(), exception);
        }
    }

    /** RFC 4180: se entrecomilla solo si hace falta y las comillas internas se duplican. */
    private static String escape(Object value)
    {
        if (value == null)
        {
            return "";
        }

        var text = String.valueOf(value);

        if (text.indexOf(',') < 0 && text.indexOf('"') < 0
                && text.indexOf('\n') < 0 && text.indexOf('\r') < 0)
        {
            return text;
        }

        return '"' + text.replace("\"", "\"\"") + '"';
    }

    /** Misma forma que el JSON, que sale en snake_case por configuracion de Jackson. */
    private static String toSnakeCase(String name)
    {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
