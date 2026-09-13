package com.cunoc.restaurant.report;

import com.cunoc.restaurant.report.dto.SalesRowView;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La unica logica del modulo que no es SQL. Se prueba aparte porque es la que convierte
 * cualquiera de los nueve reportes en el archivo que el administrador abre en Excel.
 */
class ReportCsvTest
{
    /** Fila de prueba con un texto libre, que es donde aparecen las comas y las comillas. */
    record SampleRow(Long dishId, String dishName, BigDecimal totalMargin)
    {
    }

    @Test
    void escribeLaCabeceraEnSnakeCaseYEnElOrdenDelRecord()
    {
        var csv = ReportCsv.of(SampleRow.class, List.of());

        assertThat(csv).startsWith("﻿dish_id,dish_name,total_margin\r\n");
    }

    /** Sin filas sigue habiendo cabecera: un reporte vacio tiene que abrirse igual. */
    @Test
    void emiteLaCabeceraAunqueNoHayaFilas()
    {
        var csv = ReportCsv.of(SampleRow.class, List.of());

        assertThat(csv.lines()).hasSize(1);
    }

    @Test
    void entrecomillaSoloLosCamposConComaComillaOSaltoDeLinea()
    {
        var csv = ReportCsv.of(SampleRow.class, List.of(
                new SampleRow(1L, "Arroz, pollo y papas", new BigDecimal("12.50")),
                new SampleRow(2L, "Sin comas", new BigDecimal("3.00"))));

        assertThat(csv).contains("1,\"Arroz, pollo y papas\",12.50");
        assertThat(csv).contains("2,Sin comas,3.00");
    }

    @Test
    void duplicaLasComillasInternasComoPideElFormato()
    {
        var csv = ReportCsv.of(SampleRow.class,
                List.of(new SampleRow(1L, "Pollo \"especial\"", BigDecimal.ONE)));

        assertThat(csv).contains("\"Pollo \"\"especial\"\"\"");
    }

    /** Una columna nula sale como celda vacia, no como el literal "null". */
    @Test
    void escribeLosNulosComoCeldaVacia()
    {
        var csv = ReportCsv.of(SalesRowView.class,
                List.of(new SalesRowView("2026-09-01", 1L, 2L, null, null, null)));

        assertThat(csv).contains("2026-09-01,1,2,,,");
        assertThat(csv).doesNotContain("null");
    }
}
