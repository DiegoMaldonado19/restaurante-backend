package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.dto.RecipeDTO;
import com.cunoc.restaurant.menu.dto.RecipeVersionView;
import com.cunoc.restaurant.menu.dto.RecipeView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Recetas de platillo y de modificador. Un solo controller para las dos, porque comparten
 * la misma logica de versionado (RecipeService); las rutas cuelgan de /dishes y /modifiers.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Recetas", description = "Insumos por platillo y por modificador, con versionado e historial")
public class RecipeController
{
    private final RecipeService recipeService;

    @GetMapping("/dishes/{dishId}/recipe")
    @Operation(summary = "Receta vigente del platillo con sus insumos")
    @ApiResponse(responseCode = "200", description = "Receta vigente")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND | RECIPE_NOT_FOUND")
    public RecipeView getDishRecipe(@PathVariable Long dishId)
    {
        return recipeService.getDishRecipe(dishId);
    }

    @PutMapping("/dishes/{dishId}/recipe")
    @Operation(summary = "Reemplaza la receta del platillo",
               description = "Cierra la version vigente y abre una nueva, para no alterar el costo de "
                           + "ventas ya realizadas.")
    @ApiResponse(responseCode = "200", description = "Nueva version de la receta")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND | SUPPLY_NOT_FOUND")
    public RecipeView replaceDishRecipe(@PathVariable Long dishId, @Valid @RequestBody RecipeDTO request)
    {
        return recipeService.replaceDishRecipe(dishId, request);
    }

    @GetMapping("/dishes/{dishId}/recipe-versions")
    @Operation(summary = "Historial de versiones de la receta del platillo",
               description = "Todas las versiones con su vigencia; la actual queda marcada.")
    @ApiResponse(responseCode = "200", description = "Historial de versiones")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    public List<RecipeVersionView> dishRecipeVersions(@PathVariable Long dishId)
    {
        return recipeService.dishRecipeVersions(dishId);
    }

    @GetMapping("/modifiers/{modifierId}/recipe")
    @Operation(summary = "Insumos que consume el modificador")
    @ApiResponse(responseCode = "200", description = "Receta vigente del modificador")
    @ApiResponse(responseCode = "404", description = "MODIFIER_NOT_FOUND | RECIPE_NOT_FOUND")
    public RecipeView getModifierRecipe(@PathVariable Long modifierId)
    {
        return recipeService.getModifierRecipe(modifierId);
    }

    @PutMapping("/modifiers/{modifierId}/recipe")
    @Operation(summary = "Reemplaza la receta del modificador",
               description = "Lo que el modificador (p. ej. extra queso) descuenta del inventario al aplicarse.")
    @ApiResponse(responseCode = "200", description = "Nueva version de la receta")
    @ApiResponse(responseCode = "404", description = "MODIFIER_NOT_FOUND | SUPPLY_NOT_FOUND")
    public RecipeView replaceModifierRecipe(@PathVariable Long modifierId, @Valid @RequestBody RecipeDTO request)
    {
        return recipeService.replaceModifierRecipe(modifierId, request);
    }
}
