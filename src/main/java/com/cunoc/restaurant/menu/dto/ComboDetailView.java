package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.Combo;
import com.cunoc.restaurant.menu.model.ComboItem;

import java.math.BigDecimal;
import java.util.List;

/** Detalle del combo con su composicion y el ahorro frente a comprar los platillos sueltos. */
public record ComboDetailView(
        Long                comboId,
        String              name,
        String              description,
        BigDecimal          comboPrice,
        boolean             active,
        BigDecimal          itemsTotal,
        BigDecimal          savings,
        List<ComboItemView> items)
{
    public static ComboDetailView from(Combo combo, List<ComboItem> items)
    {
        var itemsTotal = items.stream()
                .map(item -> item.getDish().getSalePrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ComboDetailView(
                combo.getComboId(),
                combo.getName(),
                combo.getDescription(),
                combo.getComboPrice(),
                combo.isActive(),
                itemsTotal,
                itemsTotal.subtract(combo.getComboPrice()),
                items.stream().map(ComboItemView::from).toList());
    }
}
