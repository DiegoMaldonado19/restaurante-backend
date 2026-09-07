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

import java.time.LocalDateTime;

/**
 * Una ronda de la cuenta (entrada+bebida, luego plato fuerte, luego postre): cada ronda
 * tiene su propio ciclo de estados independiente de las anteriores, por eso es una fila
 * propia y no una etiqueta sobre los items. submittedAt ordena la cola de cocina (la mas
 * antigua primero). El mesero es columna suelta: AppUser es de iam y no cruza la frontera.
 */
@Entity
@Table(name = "order_ticket")
@Getter
@Setter
@NoArgsConstructor
public class OrderTicket
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderTicketId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_account_id", nullable = false)
    private TableAccount account;

    @Column(nullable = false)
    private Long waiterId;

    @Column(nullable = false)
    private LocalDateTime submittedAt;
}