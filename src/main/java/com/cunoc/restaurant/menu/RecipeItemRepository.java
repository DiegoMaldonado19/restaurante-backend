package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long>
{
    List<RecipeItem> findByRecipeRecipeId(Long recipeId);
}
