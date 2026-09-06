package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.menu.dto.DishCategoryDTO;
import com.cunoc.restaurant.menu.dto.DishCategoryView;
import com.cunoc.restaurant.menu.model.DishCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Categorias de platillo (entrada, plato fuerte, bebida, postre). displayOrder ordena el menu. */
@Service
@RequiredArgsConstructor
@Transactional
public class DishCategoryService
{
    private final DishCategoryRepository dishCategoryRepository;

    @Transactional(readOnly = true)
    public List<DishCategoryView> findAll()
    {
        return dishCategoryRepository.findAllByOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(DishCategoryView::from)
                .toList();
    }

    public DishCategoryView create(DishCategoryDTO request)
    {
        requireNameAvailable(request.name());

        var category = new DishCategory();
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        category.setActive(true);

        return DishCategoryView.from(dishCategoryRepository.save(category));
    }

    public DishCategoryView update(Long categoryId, DishCategoryDTO request)
    {
        var category = findOrFail(categoryId);

        if (!category.getName().equalsIgnoreCase(request.name())
                && dishCategoryRepository.existsByNameIgnoreCaseAndDishCategoryIdNot(request.name(), categoryId))
        {
            throw nameTaken(request.name());
        }

        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());

        return DishCategoryView.from(category);
    }

    /** Baja logica: los platillos que ya la usan la conservan, solo deja de ofrecerse. */
    public void deactivate(Long categoryId)
    {
        findOrFail(categoryId).setActive(false);
    }

    DishCategory findOrFail(Long categoryId)
    {
        return dishCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DISH_CATEGORY_NOT_FOUND,
                                                         "No existe la categoria " + categoryId + "."));
    }

    private void requireNameAvailable(String name)
    {
        if (dishCategoryRepository.existsByNameIgnoreCase(name))
        {
            throw nameTaken(name);
        }
    }

    private BusinessException nameTaken(String name)
    {
        return new BusinessException(ErrorCode.DISH_CATEGORY_NAME_TAKEN,
                                     "Ya existe la categoria '" + name + "'.");
    }
}
