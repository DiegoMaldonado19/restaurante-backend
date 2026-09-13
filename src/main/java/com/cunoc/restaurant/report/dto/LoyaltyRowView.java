package com.cunoc.restaurant.report.dto;

/**
 * Fidelizacion por cliente. Los puntos redimidos se guardan negativos en el libro
 * mayor y aqui se reportan en positivo, que es como se leen en un informe.
 */
public record LoyaltyRowView(
        Long   customerId,
        String fullName,
        String phone,
        Long   visits,
        Long   pointsAccrued,
        Long   pointsRedeemed,
        Long   netPoints)
{
}
