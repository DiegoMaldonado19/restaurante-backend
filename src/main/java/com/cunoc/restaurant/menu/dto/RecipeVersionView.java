package com.cunoc.restaurant.menu.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Una version de la receta en el historial: su vigencia, sus insumos y su costo. */
public record RecipeVersionView(
        Long                 recipeId,
        int                  version,
        LocalDateTime        effectiveFrom,
        LocalDateTime        effectiveTo,
        boolean              current,
        BigDecimal           productionCost,
        List<RecipeItemView> items)
{ }
