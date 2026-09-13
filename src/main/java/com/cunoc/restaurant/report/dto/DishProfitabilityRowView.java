package com.cunoc.restaurant.report.dto;

import java.math.BigDecimal;

/**
 * Rentabilidad por platillo. El costo sale del unit_cost congelado en la linea, no de
 * la receta de hoy: es la columna que permite no alterar el costo de ventas pasadas.
 * menuPrice es el precio vigente, para ver cuanto se ha desviado del realmente cobrado.
 */
public record DishProfitabilityRowView(
        Long       dishId,
        String     dishName,
        String     categoryName,
        BigDecimal menuPrice,
        BigDecimal avgSalePrice,
        BigDecimal avgUnitCost,
        BigDecimal marginPerUnit,
        BigDecimal marginPercent,
        Long       units,
        BigDecimal totalMargin)
{
}
