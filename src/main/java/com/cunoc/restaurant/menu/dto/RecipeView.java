package com.cunoc.restaurant.menu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** La receta vigente con sus insumos y el costo de produccion (suma de las lineas). */
public record RecipeView(
        Long                 recipeId,
        int                  version,
        LocalDateTime        effectiveFrom,
        BigDecimal           productionCost,
        List<RecipeItemView> items)
{ }
