package com.cunoc.restaurant.report.dto;

import com.cunoc.restaurant.report.validation.ValidDateRange;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rango de fechas comun a ocho de los nueve reportes. Va como objeto y no como dos
 * parametros sueltos porque @ValidDateRange necesita ver los dos campos a la vez.
 */
@ValidDateRange
public record ReportRange(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
{
    public LocalDateTime fromAt()
    {
        return from.atStartOfDay();
    }

    /**
     * Medianoche del dia siguiente. El rango se consulta como [fromAt, toAt), de modo
     * que el dia "to" entra completo: con <= to a las 00:00 se perderia entero.
     */
    public LocalDateTime toAt()
    {
        return to.plusDays(1).atStartOfDay();
    }
}
