package com.cunoc.restaurant.report.dto;

import java.math.BigDecimal;

/** Desempeno por mesero, con la calificacion promedio que pide el enunciado. */
public record WaiterPerformanceRowView(
        Long       waiterId,
        String     waiterName,
        Long       accounts,
        BigDecimal sales,
        BigDecimal tips,
        Double     avgServiceMinutes,
        Double     avgRating,
        Long       ratings)
{
}
