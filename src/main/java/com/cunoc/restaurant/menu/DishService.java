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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Platillos. La disponibilidad automatica por stock y el costo de la receta llegan despues
 * (menu -> inventory); aqui vive el catalogo y la bandera manual de disponibilidad.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DishService
{
    private final DishRepository       dishRepository;
    private final DishCategoryService  dishCategoryService;
    private final ComboItemRepository  comboItemRepository;

    @Transactional(readOnly = true)
    public Page<DishView> search(Long categoryId, String search, Boolean active, Pageable pageable)
    {
        return dishRepository.search(categoryId, search, active, pageable).map(DishView::from);
    }

    @Transactional(readOnly = true)
    public DishView findById(Long dishId)
    {
        return DishView.from(findOrFail(dishId));
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

        return DishView.from(dishRepository.save(dish));
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

        return DishView.from(dish);
    }

    public DishView changeAvailability(Long dishId, UpdateDishAvailabilityDTO request)
    {
        var dish = findOrFail(dishId);
        dish.setManualAvailable(request.manualAvailable());

        return DishView.from(dish);
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

    private BusinessException nameTaken(String name)
    {
        return new BusinessException(ErrorCode.DISH_NAME_TAKEN, "Ya existe el platillo '" + name + "'.");
    }
}
