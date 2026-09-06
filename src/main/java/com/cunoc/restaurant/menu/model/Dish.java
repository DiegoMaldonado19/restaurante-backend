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
 * El platillo. manualAvailable es la bandera que el administrador o cocina apagan a mano; la
 * disponibilidad que ve el frontend es esa bandera Y ademas que haya stock para la receta,
 * pero ese calculo vive en el service (menu -> inventory), no en la entidad.
 */
@Entity
@Table(name = "dish")
@Getter
@Setter
@NoArgsConstructor
public class Dish
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dishId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_category_id", nullable = false)
    private DishCategory category;

    @Column(length = 80, nullable = false, unique = true)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal salePrice;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private int prepMinutes;

    @Column(nullable = false)
    private boolean manualAvailable;

    @Column(nullable = false)
    private boolean active;
}
