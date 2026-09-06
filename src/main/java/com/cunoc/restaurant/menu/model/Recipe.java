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

import java.time.LocalDateTime;

/**
 * Cabecera de receta, versionada e inmutable. Pertenece a un platillo O a un modificador,
 * exactamente a uno (ck_recipe_owner: dish_id XOR dish_modifier_id).
 *
 * Versionado sin borrar filas: editar una receta cierra la vigente (effectiveTo = ahora,
 * currentFlag = null) y abre una nueva. La vigente es la de effectiveTo == null.
 *
 * currentFlag es Boolean, no boolean: solo vale TRUE en la vigente y null en las historicas
 * (ck_recipe_flag). MariaDB ignora los null en el indice unico (uq_recipe_dish /
 * uq_recipe_modifier), asi que la base garantiza que no haya dos vigentes del mismo dueno.
 * Nunca se pone en FALSE.
 *
 * createdBy es un Long suelto y no una relacion: app_user pertenece a iam y no cruza la frontera.
 */
@Entity
@Table(name = "recipe")
@Getter
@Setter
@NoArgsConstructor
public class Recipe
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id")
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_modifier_id")
    private DishModifier modifier;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private Boolean currentFlag;

    @Column(nullable = false)
    private Long createdBy;
}
