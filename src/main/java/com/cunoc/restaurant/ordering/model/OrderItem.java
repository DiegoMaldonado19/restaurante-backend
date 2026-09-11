package com.cunoc.restaurant.ordering.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

/**
 * Un platillo de una ronda, con su precio y su costo CONGELADOS al enviar la comanda: asi
 * cambiar una receta o el precio del menu no altera el costo de ventas ya realizadas.
 * dishId, comboId y cancelledBy son columnas sueltas (Dish, Combo y AppUser son de otros
 * modulos y no cruzan la frontera); el split si es relacion porque AccountSplit es de este
 * modulo. submittedAt es la base del calculo de tiempo excedido.
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_ticket_id", nullable = false)
    private OrderTicket ticket;

    @Column(nullable = false)
    private Long dishId;

    private Long comboId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_split_id")
    private AccountSplit split;

    @Column(nullable = false)
    private int quantity;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(length = 255)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private OrderItemStatus status;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime readyAt;

    private LocalDateTime deliveredAt;

    private Long cancelledBy;

    @Column(length = 255)
    private String cancellationReason;
}