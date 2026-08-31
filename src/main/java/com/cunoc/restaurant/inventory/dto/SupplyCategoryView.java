package com.cunoc.restaurant.inventory.dto;

import com.cunoc.restaurant.inventory.model.SupplyCategory;

public record SupplyCategoryView(
        Long    supplyCategoryId,
        String  name,
        boolean active)
{
    public static SupplyCategoryView from(SupplyCategory category)
    {
        return new SupplyCategoryView(
                category.getSupplyCategoryId(),
                category.getName(),
                category.isActive());
    }
}
