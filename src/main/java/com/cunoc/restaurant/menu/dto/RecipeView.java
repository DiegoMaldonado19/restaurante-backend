package com.cunoc.restaurant.menu.dto;

import com.cunoc.restaurant.menu.model.Recipe;
import com.cunoc.restaurant.menu.model.RecipeItem;

import java.time.LocalDateTime;
import java.util.List;

/** La receta vigente con sus insumos. El costo de produccion se agrega en la Fase 4. */
public record RecipeView(
        Long                recipeId,
        int                 version,
        LocalDateTime       effectiveFrom,
        List<RecipeItemView> items)
{
    public static RecipeView from(Recipe recipe, List<RecipeItem> items)
    {
        return new RecipeView(
                recipe.getRecipeId(),
                recipe.getVersion(),
                recipe.getEffectiveFrom(),
                items.stream().map(RecipeItemView::from).toList());
    }
}
