package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.DishCategory;

public record DishCategoryView(
        Long    dishCategoryId,
        String  name,
        int     displayOrder,
        boolean active)
{
    public static DishCategoryView from(DishCategory category)
    {
        return new DishCategoryView(
                category.getDishCategoryId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isActive());
    }
}
