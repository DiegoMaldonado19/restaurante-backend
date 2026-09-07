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

import java.time.LocalDateTime;

/**
 * Una cuenta por mesa, abierta al sentar. La mesa y el mesero son columnas sueltas y no
 * relaciones: RestaurantTable y AppUser pertenecen a otros modulos y no cruzan la frontera
 * (mismo patron que StockMovement.userId). El estado de la mesa lo mueve
 * restaurant.transitionTo(), nunca esta entidad. closedAt se llena al cerrar, anular o fusionar.
 */
@Entity
@Table(name = "table_account")
@Getter
@Setter
@NoArgsConstructor
public class TableAccount
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tableAccountId;

    @Column(nullable = false)
    private Long restaurantTableId;

    @Column(nullable = false)
    private Long waiterId;

    @Column(nullable = false)
    private int guestCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_account_id")
    private TableAccount mergedInto;

    @Column(length = 255)
    private String cancellationReason;

    @Column(nullable = false)
    private LocalDateTime openedAt;

    private LocalDateTime closedAt;
}