package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.menu.dto.CreateDishDTO;
import com.cunoc.restaurant.menu.dto.DishView;
import com.cunoc.restaurant.menu.dto.UpdateDishAvailabilityDTO;
import com.cunoc.restaurant.menu.dto.UpdateDishDTO;
import com.cunoc.restaurant.menu.model.Dish;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platillos: el catalogo y la bandera manual de disponibilidad. La disponibilidad efectiva
 * (manual Y stock) y el costo se derivan en MenuService (menu -> inventory).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DishService
{
    private final DishRepository      dishRepository;
    private final DishCategoryService dishCategoryService;
    private final ComboItemRepository comboItemRepository;
    private final MenuService         menuService;

    /**
     * El filtro available combina la bandera manual con el stock real, asi que se resuelve en
     * memoria sobre la pagina (no cabe en la consulta: el stock vive en inventory). El total
     * de la pagina es el previo al filtro.
     */
    @Transactional(readOnly = true)
    public Page<DishView> search(Long categoryId, String search, Boolean active, Boolean available,
                                 Pageable pageable)
    {
        var page  = dishRepository.search(categoryId, search, active, pageable);
        var views = page.getContent().stream()
                .map(dish -> DishView.from(dish, menuService.isAvailable(dish)))
                .filter(view -> available == null || view.available() == available)
                .toList();

        return new PageImpl<>(views, pageable, page.getTotalElements());
    }

    public DishView create(CreateDishDTO request)
    {
        if (dishRepository.existsByNameIgnoreCase(request.name()))
        {
            throw nameTaken(request.name());
        }

        var dish = new Dish();
        dish.setCategory(dishCategoryService.findOrFail(request.dishCategoryId()));
        dish.setName(request.name());
        dish.setDescription(request.description());
        dish.setSalePrice(request.salePrice());
        dish.setImageUrl(request.imageUrl());
        dish.setPrepMinutes(request.prepMinutes());
        dish.setManualAvailable(true);
        dish.setActive(true);

        return view(dishRepository.save(dish));
    }

    public DishView update(Long dishId, UpdateDishDTO request)
    {
        var dish = findOrFail(dishId);

        if (!dish.getName().equalsIgnoreCase(request.name())
                && dishRepository.existsByNameIgnoreCaseAndDishIdNot(request.name(), dishId))
        {
            throw nameTaken(request.name());
        }

        dish.setCategory(dishCategoryService.findOrFail(request.dishCategoryId()));
        dish.setName(request.name());
        dish.setDescription(request.description());
        dish.setSalePrice(request.salePrice());
        dish.setImageUrl(request.imageUrl());
        dish.setPrepMinutes(request.prepMinutes());

        return view(dish);
    }

    public DishView changeAvailability(Long dishId, UpdateDishAvailabilityDTO request)
    {
        var dish = findOrFail(dishId);
        dish.setManualAvailable(request.manualAvailable());

        return view(dish);
    }

    /** Baja logica. No se borra: hay comandas y facturas historicas que lo referencian. */
    public void delete(Long dishId)
    {
        var dish = findOrFail(dishId);

        if (comboItemRepository.existsByDishDishIdAndComboActiveTrue(dishId))
        {
            throw new BusinessException(ErrorCode.DISH_IN_USE,
                                        "El platillo forma parte de un combo activo.");
        }

        dish.setActive(false);
    }

    Dish findOrFail(Long dishId)
    {
        return dishRepository.findById(dishId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DISH_NOT_FOUND,
                                                         "No existe el platillo " + dishId + "."));
    }

    private DishView view(Dish dish)
    {
        return DishView.from(dish, menuService.isAvailable(dish));
    }

    private BusinessException nameTaken(String name)
    {
        return new BusinessException(ErrorCode.DISH_NAME_TAKEN, "Ya existe el platillo '" + name + "'.");
    }
}
