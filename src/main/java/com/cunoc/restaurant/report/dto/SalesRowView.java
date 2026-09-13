package com.cunoc.restaurant.report.dto;

import java.math.BigDecimal;

/** Una fila por franja de tiempo. `sales` es el subtotal vendido, sin impuesto ni propina. */
public record SalesRowView(
        String     period,
        Long       invoices,
        Long       units,
        BigDecimal sales,
        BigDecimal cost,
        BigDecimal margin)
{
}
