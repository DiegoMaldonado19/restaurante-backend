package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.inventory.InventoryService;
import com.cunoc.restaurant.menu.dto.RecipeDTO;
import com.cunoc.restaurant.menu.dto.RecipeItemDTO;
import com.cunoc.restaurant.menu.dto.RecipeVersionView;
import com.cunoc.restaurant.menu.dto.RecipeView;
import com.cunoc.restaurant.menu.model.Recipe;
import com.cunoc.restaurant.menu.model.RecipeItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Recetas de platillo y de modificador, versionadas e inmutables. Editar una receta no
 * actualiza filas: cierra la vigente (effectiveTo, current_flag = null) y abre una nueva.
 * Asi cambiar una receta hoy no altera el costo de una venta de ayer.
 *
 * El costo de las vistas lo arma MenuService (menu -> inventory); aqui se toca inventory solo
 * para validar que cada insumo exista.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RecipeService
{
    private final RecipeRepository     recipeRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final DishService          dishService;
    private final ModifierService      modifierService;
    private final InventoryService     inventoryService;
    private final MenuService          menuService;

    // --- Lectura ------------------------------------------------------------

    @Transactional(readOnly = true)
    public RecipeView getDishRecipe(Long dishId)
    {
        dishService.findOrFail(dishId);

        return menuService.recipeViewFor(recipeRepository.findByDishDishIdAndCurrentFlagTrue(dishId)
                .orElseThrow(() -> recipeNotFound(dishId)));
    }

    @Transactional(readOnly = true)
    public RecipeView getModifierRecipe(Long modifierId)
    {
        modifierService.findOrFail(modifierId);

        return menuService.recipeViewFor(
                recipeRepository.findByModifierDishModifierIdAndCurrentFlagTrue(modifierId)
                        .orElseThrow(() -> recipeNotFound(modifierId)));
    }

    @Transactional(readOnly = true)
    public List<RecipeVersionView> dishRecipeVersions(Long dishId)
    {
        dishService.findOrFail(dishId);

        return recipeRepository.findByDishDishIdOrderByVersionAsc(dishId)
                .stream()
                .map(menuService::recipeVersionViewFor)
                .toList();
    }

    // --- Reemplazo versionado -----------------------------------------------

    public RecipeView replaceDishRecipe(Long dishId, RecipeDTO request)
    {
        var dish    = dishService.findOrFail(dishId);
        var current = recipeRepository.findByDishDishIdAndCurrentFlagTrue(dishId);

        return replace(request, current, recipe -> recipe.setDish(dish));
    }

    public RecipeView replaceModifierRecipe(Long modifierId, RecipeDTO request)
    {
        var modifier = modifierService.findOrFail(modifierId);
        var current  = recipeRepository.findByModifierDishModifierIdAndCurrentFlagTrue(modifierId);

        return replace(request, current, recipe -> recipe.setModifier(modifier));
    }

    private RecipeView replace(RecipeDTO request, Optional<Recipe> current, Consumer<Recipe> assignOwner)
    {
        requireDistinctSupplies(request.items());
        request.items().forEach(item -> inventoryService.availableStock(item.supplyId())); // valida existencia

        int nextVersion = 1;

        if (current.isPresent())
        {
            var previous = current.get();
            previous.setEffectiveTo(LocalDateTime.now());
            previous.setCurrentFlag(null);
            nextVersion = previous.getVersion() + 1;

            // Cierra la vigente ANTES de insertar la nueva: si no, Hibernate insertaria
            // primero y habria dos con current_flag = TRUE, violando uq_recipe_dish.
            recipeRepository.flush();
        }

        var recipe = new Recipe();
        assignOwner.accept(recipe);
        recipe.setVersion(nextVersion);
        recipe.setEffectiveFrom(LocalDateTime.now());
        recipe.setCurrentFlag(Boolean.TRUE);
        recipe.setCreatedBy(CurrentUser.id());
        recipeRepository.save(recipe);

        recipeItemRepository.saveAll(request.items().stream().map(line -> newItem(recipe, line)).toList());

        return menuService.recipeViewFor(recipe);
    }

    // --- Auxiliares ---------------------------------------------------------

    private RecipeItem newItem(Recipe recipe, RecipeItemDTO line)
    {
        var item = new RecipeItem();
        item.setRecipe(recipe);
        item.setSupplyId(line.supplyId());
        item.setQuantity(line.quantity());

        return item;
    }

    // uq_recipe_item_supply impide el mismo insumo dos veces; se ataja aqui para responder
    // 400 VALIDATION_ERROR en vez del 500 que daria la violacion de la restriccion.
    private void requireDistinctSupplies(List<RecipeItemDTO> items)
    {
        var distinct = items.stream().map(RecipeItemDTO::supplyId).distinct().count();

        if (distinct != items.size())
        {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                        "Un insumo no puede aparecer dos veces en la receta.");
        }
    }

    private NotFoundException recipeNotFound(Long ownerId)
    {
        return new NotFoundException(ErrorCode.RECIPE_NOT_FOUND,
                                     "No hay una receta vigente para " + ownerId + ".");
    }
}
