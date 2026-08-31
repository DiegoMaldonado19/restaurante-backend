package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.inventory.dto.SupplyCategoryDTO;
import com.cunoc.restaurant.inventory.dto.SupplyCategoryView;
import com.cunoc.restaurant.inventory.model.SupplyCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Categorias de insumo. El enunciado deja la lista abierta, por eso es tabla y no enum. */
@Service
@RequiredArgsConstructor
@Transactional
public class SupplyCategoryService
{
    private final SupplyCategoryRepository supplyCategoryRepository;

    @Transactional(readOnly = true)
    public List<SupplyCategoryView> findAll()
    {
        return supplyCategoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(SupplyCategoryView::from)
                .toList();
    }

    public SupplyCategoryView create(SupplyCategoryDTO request)
    {
        requireNameAvailable(request.name());

        var category = new SupplyCategory();
        category.setName(request.name());
        category.setActive(true);

        return SupplyCategoryView.from(supplyCategoryRepository.save(category));
    }

    public SupplyCategoryView rename(Long categoryId, SupplyCategoryDTO request)
    {
        var category = findOrFail(categoryId);

        if (!category.getName().equalsIgnoreCase(request.name()))
        {
            requireNameAvailable(request.name());
        }

        category.setName(request.name());

        return SupplyCategoryView.from(category);
    }

    /** Baja logica: los insumos que ya la usan la conservan, solo deja de ofrecerse. */
    public void deactivate(Long categoryId)
    {
        findOrFail(categoryId).setActive(false);
    }

    SupplyCategory findOrFail(Long categoryId)
    {
        return supplyCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SUPPLY_CATEGORY_NOT_FOUND,
                                                         "No existe la categoria " + categoryId + "."));
    }

    private void requireNameAvailable(String name)
    {
        if (supplyCategoryRepository.existsByNameIgnoreCase(name))
        {
            throw new BusinessException(ErrorCode.SUPPLY_CATEGORY_NAME_TAKEN,
                                        "Ya existe la categoria '" + name + "'.");
        }
    }
}
