package com.cunoc.restaurant.inventory.model;

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
 * El libro mayor del stock: inmutable, nunca se borra una fila. quantity va con signo,
 * de modo que SUM(quantity) por insumo tiene que dar exactamente supply.current_stock.
 * userId y orderItemId son columnas sueltas y no relaciones: las entidades de iam y de
 * ordering pertenecen a otros modulos y no cruzan la frontera.
 */
@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stockMovementId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MovementType movementType;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private WasteReason wasteReason;

    @Column(length = 255)
    private String reason;

    private Long orderItemId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
