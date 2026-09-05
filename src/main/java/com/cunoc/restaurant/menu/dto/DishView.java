package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.Dish;

import java.math.BigDecimal;

public record DishView(
        Long       dishId,
        Long       dishCategoryId,
        String     categoryName,
        String     name,
        String     description,
        BigDecimal salePrice,
        String     imageUrl,
        int        prepMinutes,
        boolean    manualAvailable,
        boolean    active)
{
    public static DishView from(Dish dish)
    {
        return new DishView(
                dish.getDishId(),
                dish.getCategory().getDishCategoryId(),
                dish.getCategory().getName(),
                dish.getName(),
                dish.getDescription(),
                dish.getSalePrice(),
                dish.getImageUrl(),
                dish.getPrepMinutes(),
                dish.isManualAvailable(),
                dish.isActive());
    }
}
