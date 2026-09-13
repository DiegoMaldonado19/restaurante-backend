package com.cunoc.restaurant.dining.dto;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lectura compuesta por mesa para GET /floor-plan: estado, cuenta abierta si la hay,
 * y la proxima reserva del dia. No es una tabla, se arma en el Service consultando
 * restaurant, ordering y dining en el momento de la peticion.
 */
public record FloorPlanView(
    Long restaurantTableId,
    int tableNumber,
    int capacity,
    TableZone zone,
    TableStatus status,
    OpenAccountSummary openAccount,
    NextReservationSummary nextReservation
)
{
    public record OpenAccountSummary(
        Long tableAccountId,
        String waiterName,
        LocalDateTime openedAt,
        BigDecimal runningTotal
    )
    {}

    public record NextReservationSummary(
        Long reservationId,
        String customerName,
        LocalDateTime reservedAt,
        int guestCount
    )
    {}
}
