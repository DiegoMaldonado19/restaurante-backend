package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long>
{
    // La vigente es la que lleva current_flag = TRUE; las historicas lo llevan en NULL.
    Optional<Recipe> findByDishDishIdAndCurrentFlagTrue(Long dishId);

    Optional<Recipe> findByModifierDishModifierIdAndCurrentFlagTrue(Long dishModifierId);

    List<Recipe> findByDishDishIdOrderByVersionAsc(Long dishId);

    List<Recipe> findByModifierDishModifierIdOrderByVersionAsc(Long dishModifierId);
}
