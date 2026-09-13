package com.cunoc.restaurant.report.dto;

/** Ocupacion por franja horaria: cuentas abiertas, comensales y estancia media. */
public record TableOccupancyRowView(
        Integer hourSlot,
        Long    accounts,
        Long    guests,
        Double  avgMinutes)
{
}
