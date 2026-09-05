package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.DishModifier;

import java.math.BigDecimal;

public record ModifierView(
        Long       dishModifierId,
        Long       dishId,
        String     name,
        BigDecimal extraPrice,
        boolean    active)
{
    public static ModifierView from(DishModifier modifier)
    {
        return new ModifierView(
                modifier.getDishModifierId(),
                modifier.getDish().getDishId(),
                modifier.getName(),
                modifier.getExtraPrice(),
                modifier.isActive());
    }
}
