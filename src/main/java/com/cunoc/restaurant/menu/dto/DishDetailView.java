package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.Dish;

import java.math.BigDecimal;

/**
 * Ficha del platillo (GET /dishes/{id}): sus datos, su receta vigente, el costo de produccion
 * y el margen sobre el precio. productionCost / marginPercent / recipe son null si aun no tiene
 * receta vigente (costo indefinido).
 */
public record DishDetailView(
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
        BigDecimal productionCost,
        BigDecimal marginPercent,
        boolean    active,
        RecipeView recipe)
{
    public static DishDetailView from(Dish dish, boolean available, BigDecimal productionCost,
                                      BigDecimal marginPercent, RecipeView recipe)
    {
        return new DishDetailView(
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
                productionCost,
                marginPercent,
                dish.isActive(),
                recipe);
    }
}
