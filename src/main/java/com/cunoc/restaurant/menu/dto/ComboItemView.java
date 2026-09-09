package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.ComboItem;

import java.math.BigDecimal;

public record ComboItemView(
        Long       dishId,
        String     dishName,
        BigDecimal salePrice,
        int        quantity)
{
    public static ComboItemView from(ComboItem item)
    {
        return new ComboItemView(
                item.getDish().getDishId(),
                item.getDish().getName(),
                item.getDish().getSalePrice(),
                item.getQuantity());
    }
}
