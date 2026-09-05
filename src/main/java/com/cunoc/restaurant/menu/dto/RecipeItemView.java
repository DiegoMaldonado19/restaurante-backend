package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.RecipeItem;

import java.math.BigDecimal;

/** Linea de receta. El costo por insumo se agrega en la Fase 4 (necesita unit_cost de inventory). */
public record RecipeItemView(
        Long       supplyId,
        BigDecimal quantity)
{
    public static RecipeItemView from(RecipeItem item)
    {
        return new RecipeItemView(item.getSupplyId(), item.getQuantity());
    }
}
