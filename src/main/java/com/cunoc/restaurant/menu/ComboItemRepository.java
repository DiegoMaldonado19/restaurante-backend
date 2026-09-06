package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.model.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboItemRepository extends JpaRepository<ComboItem, Long>
{
    List<ComboItem> findByComboComboId(Long comboId);

    // El guard DISH_IN_USE: un platillo no se da de baja si un combo activo lo incluye.
    boolean existsByDishDishIdAndComboActiveTrue(Long dishId);

    // Reemplazo de composicion en PUT /combos/{id}: borrado directo para que las nuevas
    // lineas no choquen con uq_combo_item_dish al reinsertar el mismo platillo.
    @Modifying
    @Query("DELETE FROM ComboItem ci WHERE ci.combo.comboId = :comboId")
    void deleteByComboId(@Param("comboId") Long comboId);
}
