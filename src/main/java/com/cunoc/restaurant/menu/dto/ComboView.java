package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.Combo;

import java.math.BigDecimal;

public record ComboView(
        Long       comboId,
        String     name,
        String     description,
        BigDecimal comboPrice,
        boolean    active)
{
    public static ComboView from(Combo combo)
    {
        return new ComboView(
                combo.getComboId(),
                combo.getName(),
                combo.getDescription(),
                combo.getComboPrice(),
                combo.isActive());
    }
}
