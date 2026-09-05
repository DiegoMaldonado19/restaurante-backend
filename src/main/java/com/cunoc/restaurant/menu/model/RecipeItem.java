package com.cunoc.restaurant.menu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Una linea de receta: un insumo con la cantidad exacta que consume. El mismo insumo no se
 * repite en una receta (uq_recipe_item_supply). supplyId es un Long suelto y no una relacion:
 * supply pertenece a inventory y no cruza la frontera; el costo y el stock se piden al service
 * de inventory (menu -> inventory).
 */
@Entity
@Table(name = "recipe_item")
@Getter
@Setter
@NoArgsConstructor
public class RecipeItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recipeItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(nullable = false)
    private Long supplyId;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity;
}
