package com.cunoc.restaurant.customer.model;

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

import java.time.LocalDateTime;

/**
 * El libro mayor de puntos. points va con signo, de modo que el saldo es SUM(points).
 * Aqui si se suma y no se materializa: son decenas de filas por cliente y el saldo se
 * consulta una vez por cobro, asi que es correcto por construccion.
 * invoiceId es columna suelta: la factura pertenece a billing y no cruza la frontera.
 */
@Entity
@Table(name = "loyalty_transaction")
@Getter
@Setter
@NoArgsConstructor
public class LoyaltyTransaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loyaltyTransactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private LoyaltyTransactionType transactionType;

    @Column(nullable = false)
    private int points;

    private Long invoiceId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
