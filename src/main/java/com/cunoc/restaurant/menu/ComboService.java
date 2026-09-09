package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.menu.dto.ComboDetailView;
import com.cunoc.restaurant.menu.dto.ComboItemDTO;
import com.cunoc.restaurant.menu.dto.ComboView;
import com.cunoc.restaurant.menu.dto.CreateComboDTO;
import com.cunoc.restaurant.menu.dto.UpdateComboDTO;
import com.cunoc.restaurant.menu.dto.UpdateComboStatusDTO;
import com.cunoc.restaurant.menu.model.Combo;
import com.cunoc.restaurant.menu.model.ComboItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Combos y promociones. Resuelve los platillos con DishService (Service de su propio modulo,
 * nunca el Repository de otro) y no depende de ComboService a la inversa: por eso el guard
 * DISH_IN_USE vive en DishService leyendo ComboItemRepository, evitando un ciclo entre ambos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ComboService
{
    private final ComboRepository     comboRepository;
    private final ComboItemRepository comboItemRepository;
    private final DishService         dishService;

    @Transactional(readOnly = true)
    public List<ComboView> search(Boolean active)
    {
        return comboRepository.search(active).stream().map(ComboView::from).toList();
    }

    @Transactional(readOnly = true)
    public ComboDetailView findById(Long comboId)
    {
        var combo = findOrFail(comboId);

        return ComboDetailView.from(combo, comboItemRepository.findByComboComboId(comboId));
    }

    public ComboDetailView create(CreateComboDTO request)
    {
        if (comboRepository.existsByNameIgnoreCase(request.name()))
        {
            throw nameTaken(request.name());
        }
        requireDistinctDishes(request.items());

        var combo = new Combo();
        combo.setName(request.name());
        combo.setDescription(request.description());
        combo.setComboPrice(request.comboPrice());
        combo.setActive(true);
        comboRepository.save(combo);

        var items = comboItemRepository.saveAll(buildItems(combo, request.items()));

        return ComboDetailView.from(combo, items);
    }

    public ComboDetailView update(Long comboId, UpdateComboDTO request)
    {
        var combo = findOrFail(comboId);

        if (!combo.getName().equalsIgnoreCase(request.name())
                && comboRepository.existsByNameIgnoreCaseAndComboIdNot(request.name(), comboId))
        {
            throw nameTaken(request.name());
        }
        requireDistinctDishes(request.items());

        combo.setName(request.name());
        combo.setDescription(request.description());
        combo.setComboPrice(request.comboPrice());

        comboItemRepository.deleteByComboId(comboId);
        var items = comboItemRepository.saveAll(buildItems(combo, request.items()));

        return ComboDetailView.from(combo, items);
    }

    public ComboView changeStatus(Long comboId, UpdateComboStatusDTO request)
    {
        var combo = findOrFail(comboId);
        combo.setActive(request.active());

        return ComboView.from(combo);
    }

    private List<ComboItem> buildItems(Combo combo, List<ComboItemDTO> lines)
    {
        return lines.stream().map(line ->
        {
            var item = new ComboItem();
            item.setCombo(combo);
            item.setDish(dishService.findOrFail(line.dishId()));
            item.setQuantity(line.quantity());

            return item;
        }).toList();
    }

    // uq_combo_item_dish impide el mismo platillo dos veces; se ataja aqui para responder
    // 400 VALIDATION_ERROR en vez del 500 que daria la violacion de la restriccion.
    private void requireDistinctDishes(List<ComboItemDTO> items)
    {
        var distinct = items.stream().map(ComboItemDTO::dishId).distinct().count();

        if (distinct != items.size())
        {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                                        "Un platillo no puede aparecer dos veces en el mismo combo.");
        }
    }

    private Combo findOrFail(Long comboId)
    {
        return comboRepository.findById(comboId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.COMBO_NOT_FOUND,
                                                         "No existe el combo " + comboId + "."));
    }

    private BusinessException nameTaken(String name)
    {
        return new BusinessException(ErrorCode.COMBO_NAME_TAKEN, "Ya existe el combo '" + name + "'.");
    }
}
