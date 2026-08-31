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

/**
 * currentStock es un saldo materializado: el unico que lo escribe es
 * InventoryService.applyMovement(), y la consulta de reconciliacion comprueba
 * que siga cuadrando con la suma del libro mayor.
 */
@Entity
@Table(name = "supply")
@Getter
@Setter
@NoArgsConstructor
public class Supply
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_category_id", nullable = false)
    private SupplyCategory category;

    @Column(length = 80, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private MeasureUnit measureUnit;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal currentStock;

    @Column(precision = 12, scale = 3, nullable = false)
    private BigDecimal minStock;

    @Column(precision = 12, scale = 3)
    private BigDecimal maxStock;

    @Column(nullable = false)
    private boolean active;

    /** La alerta de reabastecimiento del enunciado. */
    public boolean isLowStock()
    {
        return currentStock.compareTo(minStock) <= 0;
    }

    public BigDecimal stockValue()
    {
        return currentStock.multiply(unitCost);
    }
}
