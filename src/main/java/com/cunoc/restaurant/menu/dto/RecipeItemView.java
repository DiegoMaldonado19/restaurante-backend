package com.cunoc.restaurant.menu.dto;

import java.math.BigDecimal;

/** Linea de receta con su costo: cantidad x costo unitario del insumo (unit_cost de inventory). */
public record RecipeItemView(
        Long       supplyId,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineCost)
{ }
