package com.cunoc.restaurant.menu.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Categoria del platillo (entrada, plato fuerte, bebida, postre). displayOrder ordena el menu. */
@Entity
@Table(name = "dish_category")
@Getter
@Setter
@NoArgsConstructor
public class DishCategory
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dishCategoryId;

    @Column(length = 60, nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;
}
