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
 * Sub-cuenta de una division. BY_PERSON crea varias con share_amount igual y los items sin
 * asignar; BY_ITEM asigna cada order_item a su split. Cada sub-cuenta se cobra por separado
 * y produce su propia factura (uq_invoice_split lo garantiza en la base, no aqui).
 */
@Entity
@Table(name = "account_split")
@Getter
@Setter
@NoArgsConstructor
public class AccountSplit
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountSplitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_account_id", nullable = false)
    private TableAccount account;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private SplitMode mode;

    @Column(length = 40)
    private String label;

    @Column(precision = 12, scale = 2)
    private BigDecimal shareAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}