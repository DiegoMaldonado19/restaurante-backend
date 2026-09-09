package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.menu.dto.CreateModifierDTO;
import com.cunoc.restaurant.menu.dto.ModifierView;
import com.cunoc.restaurant.menu.dto.UpdateModifierDTO;
import com.cunoc.restaurant.menu.model.DishModifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Modificadores de un platillo. Su receta (lo que descuentan del inventario) llega en la fase de recetas. */
@Service
@RequiredArgsConstructor
@Transactional
public class ModifierService
{
    private final DishModifierRepository dishModifierRepository;
    private final DishService            dishService;

    @Transactional(readOnly = true)
    public List<ModifierView> findByDish(Long dishId)
    {
        dishService.findOrFail(dishId);

        return dishModifierRepository.findByDishDishIdOrderByNameAsc(dishId)
                .stream()
                .map(ModifierView::from)
                .toList();
    }

    public ModifierView create(Long dishId, CreateModifierDTO request)
    {
        var dish = dishService.findOrFail(dishId);

        if (dishModifierRepository.existsByDishDishIdAndNameIgnoreCase(dishId, request.name()))
        {
            throw nameTaken(request.name());
        }

        var modifier = new DishModifier();
        modifier.setDish(dish);
        modifier.setName(request.name());
        modifier.setExtraPrice(request.extraPrice());
        modifier.setActive(true);

        return ModifierView.from(dishModifierRepository.save(modifier));
    }

    public ModifierView update(Long modifierId, UpdateModifierDTO request)
    {
        var modifier = findOrFail(modifierId);
        var dishId   = modifier.getDish().getDishId();

        if (!modifier.getName().equalsIgnoreCase(request.name())
                && dishModifierRepository
                        .existsByDishDishIdAndNameIgnoreCaseAndDishModifierIdNot(dishId, request.name(), modifierId))
        {
            throw nameTaken(request.name());
        }

        modifier.setName(request.name());
        modifier.setExtraPrice(request.extraPrice());

        return ModifierView.from(modifier);
    }

    /** Baja logica: las comandas historicas que lo aplicaron lo conservan. */
    public void delete(Long modifierId)
    {
        findOrFail(modifierId).setActive(false);
    }

    DishModifier findOrFail(Long modifierId)
    {
        return dishModifierRepository.findById(modifierId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MODIFIER_NOT_FOUND,
                                                         "No existe el modificador " + modifierId + "."));
    }

    private BusinessException nameTaken(String name)
    {
        return new BusinessException(ErrorCode.MODIFIER_NAME_TAKEN,
                                     "El platillo ya tiene un modificador '" + name + "'.");
    }
}
