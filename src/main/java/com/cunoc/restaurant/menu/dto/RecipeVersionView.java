package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.Recipe;
import com.cunoc.restaurant.menu.model.RecipeItem;

import java.time.LocalDateTime;
import java.util.List;

/** Una version de la receta en el historial: su vigencia y sus insumos. El costo llega en la Fase 4. */
public record RecipeVersionView(
        Long                recipeId,
        int                 version,
        LocalDateTime       effectiveFrom,
        LocalDateTime       effectiveTo,
        boolean             current,
        List<RecipeItemView> items)
{
    public static RecipeVersionView from(Recipe recipe, List<RecipeItem> items)
    {
        return new RecipeVersionView(
                recipe.getRecipeId(),
                recipe.getVersion(),
                recipe.getEffectiveFrom(),
                recipe.getEffectiveTo(),
                Boolean.TRUE.equals(recipe.getCurrentFlag()),
                items.stream().map(RecipeItemView::from).toList());
    }
}
