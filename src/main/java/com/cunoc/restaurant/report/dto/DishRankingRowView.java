package com.cunoc.restaurant.report.dto;

import java.math.BigDecimal;

/** Platillos mas y menos vendidos. Incluye los de cero ventas, que son los "menos". */
public record DishRankingRowView(
        Long       dishId,
        String     dishName,
        String     categoryName,
        Long       units,
        BigDecimal sales,
        BigDecimal cost,
        BigDecimal margin)
{
}
