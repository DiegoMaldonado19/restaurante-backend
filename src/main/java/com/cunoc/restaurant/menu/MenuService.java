package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.inventory.InventoryService;
import com.cunoc.restaurant.inventory.dto.SupplyConsumption;
import com.cunoc.restaurant.menu.dto.ComboItemView;
import com.cunoc.restaurant.menu.dto.DishDetailView;
import com.cunoc.restaurant.menu.dto.MenuComboView;
import com.cunoc.restaurant.menu.dto.MenuDishView;
import com.cunoc.restaurant.menu.dto.MenuView;
import com.cunoc.restaurant.menu.dto.ModifierView;
import com.cunoc.restaurant.menu.dto.OrderLineDTO;
import com.cunoc.restaurant.menu.dto.RecipeItemView;
import com.cunoc.restaurant.menu.dto.RecipeVersionView;
import com.cunoc.restaurant.menu.dto.RecipeView;
import com.cunoc.restaurant.menu.model.Combo;
import com.cunoc.restaurant.menu.model.Dish;
import com.cunoc.restaurant.menu.model.Recipe;
import com.cunoc.restaurant.menu.model.RecipeItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * El costo, la disponibilidad y el menu operativo, derivados de la receta vigente y de inventory.
 * Es el unico punto donde menu consume inventory; no escribe stock. No depende de DishService ni
 * de RecipeService (ellos dependen de este), para que la direccion sea unica y no haya ciclos.
 *
 * Aqui viven los cinco metodos publicos que ordering consume: explodeRecipe, productionCost,
 * isAvailable, maxPreparableUnits y operationalMenu.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService
{
    private final DishRepository         dishRepository;
    private final DishModifierRepository dishModifierRepository;
    private final ComboRepository        comboRepository;
    private final ComboItemRepository    comboItemRepository;
    private final RecipeRepository       recipeRepository;
    private final RecipeItemRepository   recipeItemRepository;
    private final InventoryService       inventoryService;

    // --- Contrato de disponibilidad y costo ---------------------------------

    public int maxPreparableUnits(Long dishId)
    {
        return maxPreparableUnits(currentDishRecipe(dishId).orElse(null));
    }

    public boolean isAvailable(Long dishId)
    {
        return isAvailable(findDishOrFail(dishId));
    }

    /** Sobrecarga para no recargar el platillo cuando el llamador ya lo tiene (listado, menu). */
    boolean isAvailable(Dish dish)
    {
        return dish.isManualAvailable()
                && maxPreparableUnits(currentDishRecipe(dish.getDishId()).orElse(null)) >= 1;
    }

    /** Costo de la receta vigente del platillo, o null si aun no tiene receta (costo indefinido). */
    public BigDecimal productionCost(Long dishId)
    {
        return currentDishRecipe(dishId).map(this::totalCost).orElse(null);
    }

    /**
     * Insumos que consume una comanda: la receta vigente del platillo mas la de cada modificador
     * aplicado, todo por la cantidad de la linea, agrupado por insumo. Lo que ordering pasa a
     * inventory.registerSaleConsumption() para descontar stock. Sin receta vigente -> RECIPE_NOT_DEFINED.
     */
    public List<SupplyConsumption> explodeRecipe(List<OrderLineDTO> lines)
    {
        Map<Long, BigDecimal> totals = new LinkedHashMap<>();

        for (var line : lines)
        {
            var lineQty = BigDecimal.valueOf(line.quantity());

            accumulate(totals, dishRecipeOrFail(line.dishId()), lineQty);

            if (line.modifierIds() != null)
            {
                for (var modifierId : line.modifierIds())
                {
                    accumulate(totals, modifierRecipeOrFail(modifierId), lineQty);
                }
            }
        }

        return totals.entrySet().stream()
                .map(entry -> new SupplyConsumption(entry.getKey(), entry.getValue()))
                .toList();
    }

    // --- Menu operativo (GET /menu) -----------------------------------------

    public MenuView operationalMenu()
    {
        var dishes = dishRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(this::isAvailable)
                .map(this::menuDishView)
                .toList();

        var combos = comboRepository.search(true).stream()
                .map(this::menuComboView)
                .toList();

        return new MenuView(dishes, combos);
    }

    // --- Vistas compuestas del catalogo -------------------------------------

    public DishDetailView dishDetail(Long dishId)
    {
        var dish       = findDishOrFail(dishId);
        var recipe     = currentDishRecipe(dishId);
        var recipeView = recipe.map(this::recipeViewFor).orElse(null);
        var cost       = recipeView == null ? null : recipeView.productionCost();
        var available  = dish.isManualAvailable() && maxPreparableUnits(recipe.orElse(null)) >= 1;

        return DishDetailView.from(dish, available, cost, marginPercent(dish.getSalePrice(), cost), recipeView);
    }

    /** Receta vigente con costo. La usa RecipeService para servir GET /recipe. */
    public RecipeView recipeViewFor(Recipe recipe)
    {
        var items = itemViews(recipe);

        return new RecipeView(recipe.getRecipeId(), recipe.getVersion(), recipe.getEffectiveFrom(),
                              sumLines(items), items);
    }

    public RecipeVersionView recipeVersionViewFor(Recipe recipe)
    {
        var items = itemViews(recipe);

        return new RecipeVersionView(recipe.getRecipeId(), recipe.getVersion(), recipe.getEffectiveFrom(),
                                     recipe.getEffectiveTo(), Boolean.TRUE.equals(recipe.getCurrentFlag()),
                                     sumLines(items), items);
    }

    // --- Auxiliares del menu operativo --------------------------------------

    private MenuDishView menuDishView(Dish dish)
    {
        var modifiers = dishModifierRepository.findByDishDishIdAndActiveTrueOrderByNameAsc(dish.getDishId())
                .stream()
                .map(ModifierView::from)
                .toList();

        return new MenuDishView(dish.getDishId(), dish.getName(), dish.getCategory().getName(),
                                dish.getSalePrice(), dish.getPrepMinutes(), dish.getImageUrl(), modifiers);
    }

    private MenuComboView menuComboView(Combo combo)
    {
        var items = comboItemRepository.findByComboComboId(combo.getComboId())
                .stream()
                .map(ComboItemView::from)
                .toList();

        return new MenuComboView(combo.getComboId(), combo.getName(), combo.getDescription(),
                                 combo.getComboPrice(), items);
    }

    // --- Auxiliares de explosion de receta ----------------------------------

    private void accumulate(Map<Long, BigDecimal> totals, Recipe recipe, BigDecimal lineQty)
    {
        for (var item : recipeItemRepository.findByRecipeRecipeId(recipe.getRecipeId()))
        {
            totals.merge(item.getSupplyId(), item.getQuantity().multiply(lineQty), BigDecimal::add);
        }
    }

    private Recipe dishRecipeOrFail(Long dishId)
    {
        return recipeRepository.findByDishDishIdAndCurrentFlagTrue(dishId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_DEFINED,
                        "El platillo " + dishId + " no tiene receta vigente."));
    }

    private Recipe modifierRecipeOrFail(Long modifierId)
    {
        return recipeRepository.findByModifierDishModifierIdAndCurrentFlagTrue(modifierId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_DEFINED,
                        "El modificador " + modifierId + " no tiene receta vigente."));
    }

    // --- Auxiliares de costo y disponibilidad -------------------------------

    private Optional<Recipe> currentDishRecipe(Long dishId)
    {
        return recipeRepository.findByDishDishIdAndCurrentFlagTrue(dishId);
    }

    /** floor( MIN sobre la receta de stock_disponible / cantidad ). Sin receta -> 0. */
    private int maxPreparableUnits(Recipe recipe)
    {
        if (recipe == null)
        {
            return 0;
        }

        var items = recipeItemRepository.findByRecipeRecipeId(recipe.getRecipeId());

        return items.stream()
                .mapToInt(item -> inventoryService.availableStock(item.getSupplyId())
                        .divide(item.getQuantity(), 0, RoundingMode.DOWN)
                        .intValue())
                .min()
                .orElse(0);
    }

    private BigDecimal totalCost(Recipe recipe)
    {
        return sumLines(itemViews(recipe));
    }

    private List<RecipeItemView> itemViews(Recipe recipe)
    {
        return recipeItemRepository.findByRecipeRecipeId(recipe.getRecipeId())
                .stream()
                .map(this::itemView)
                .toList();
    }

    private RecipeItemView itemView(RecipeItem item)
    {
        var unitCost = inventoryService.findById(item.getSupplyId()).supply().unitCost();
        var lineCost = item.getQuantity().multiply(unitCost).setScale(2, RoundingMode.HALF_UP);

        return new RecipeItemView(item.getSupplyId(), item.getQuantity(), unitCost, lineCost);
    }

    private BigDecimal sumLines(List<RecipeItemView> items)
    {
        return items.stream().map(RecipeItemView::lineCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal marginPercent(BigDecimal salePrice, BigDecimal cost)
    {
        if (cost == null || salePrice.signum() == 0)
        {
            return null;
        }

        return salePrice.subtract(cost)
                .divide(salePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Dish findDishOrFail(Long dishId)
    {
        return dishRepository.findById(dishId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DISH_NOT_FOUND,
                                                         "No existe el platillo " + dishId + "."));
    }
}
