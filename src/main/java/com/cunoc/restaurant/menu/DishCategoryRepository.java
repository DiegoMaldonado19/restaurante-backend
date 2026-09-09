package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishCategoryRepository extends JpaRepository<DishCategory, Long>
{
    List<DishCategory> findAllByOrderByDisplayOrderAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndDishCategoryIdNot(String name, Long dishCategoryId);
}
