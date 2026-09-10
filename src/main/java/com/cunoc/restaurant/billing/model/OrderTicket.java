package com.cunoc.restaurant.billing.model;

// Lectura directa de una tabla de 'ordering', documentada como excepcion temporal
// mientras ese modulo no existe (07-Orden-de-Construccion.md). Cuando ordering
// tenga su propio Service, esto se reemplaza por una llamada a el.

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_ticket")
@Getter
@NoArgsConstructor
public class OrderTicket
{
    @Id
    private Long orderTicketId;

    private Long tableAccountId;
}
