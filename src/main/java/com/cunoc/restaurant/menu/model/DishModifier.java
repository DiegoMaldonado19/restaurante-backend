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
 * Modificador de un platillo ("sin cebolla", "extra queso"): puede cobrar extra al cliente
 * (extraPrice) y tiene su propia receta, porque al aplicarse descuenta inventario igual que
 * el platillo. La unicidad del nombre es por platillo (uq_dish_modifier_name), la garantiza
 * la base y por eso no se marca unique a nivel de columna.
 */
@Entity
@Table(name = "dish_modifier")
@Getter
@Setter
@NoArgsConstructor
public class DishModifier
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long dishModifierId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(length = 60, nullable = false)
    private String name;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal extraPrice;

    @Column(nullable = false)
    private boolean active;
}
