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
        boolean    available,
        boolean    active)
{
    /** available = bandera manual Y stock suficiente; lo calcula MenuService (menu -> inventory). */
    public static DishView from(Dish dish, boolean available)
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
                available,
                dish.isActive());
    }
}
