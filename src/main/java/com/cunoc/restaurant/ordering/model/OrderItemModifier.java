package com.cunoc.restaurant.ordering.model;

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
 * Un modificador aplicado a un order_item (\"extra queso\", \"sin cebolla\"), con su costo
 * adicional congelado al enviar la comanda. dishModifierId es columna suelta (DishModifier
 * es de menu y no cruza la frontera); la unicidad la garantiza uq_order_item_modifier.
 */
@Entity
@Table(name = "order_item_modifier")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemModifier
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemModifierId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Long dishModifierId;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal extraPrice;
}