package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.DishModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishModifierRepository extends JpaRepository<DishModifier, Long>
{
    List<DishModifier> findByDishDishIdOrderByNameAsc(Long dishId);

    // Solo los activos: es lo que ofrece el menu operativo.
    List<DishModifier> findByDishDishIdAndActiveTrueOrderByNameAsc(Long dishId);

    boolean existsByDishDishIdAndNameIgnoreCase(Long dishId, String name);

    boolean existsByDishDishIdAndNameIgnoreCaseAndDishModifierIdNot(Long dishId, String name, Long dishModifierId);
}
