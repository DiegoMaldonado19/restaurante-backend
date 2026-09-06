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

/** Un platillo dentro de un combo, con su cantidad. Un platillo no se repite en el mismo combo (uq_combo_item_dish). */
@Entity
@Table(name = "combo_item")
@Getter
@Setter
@NoArgsConstructor
public class ComboItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long comboItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false)
    private int quantity;
}
