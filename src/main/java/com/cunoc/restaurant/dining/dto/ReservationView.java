package com.cunoc.restaurant.dining.dto;

import com.cunoc.restaurant.dining.model.CancellationReason;
import com.cunoc.restaurant.dining.model.Reservation;
import com.cunoc.restaurant.dining.model.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationView(
    Long reservationId,
    Long customerId,
    String customerName,
    String customerPhone,
    Long restaurantTableId,
    Long tableAccountId,
    LocalDateTime reservedAt,
    int guestCount,
    ReservationStatus status,
    CancellationReason cancellationReason,
    String note,
    LocalDateTime createdAt
)
{
    public static ReservationView from(Reservation entity, String customerName, String customerPhone)
    {
        return new ReservationView(
            entity.getReservationId(),
            entity.getCustomerId(),
            customerName,
            customerPhone,
            entity.getRestaurantTableId(),
            entity.getTableAccountId(),
            entity.getReservedAt(),
            entity.getGuestCount(),
            entity.getStatus(),
            entity.getCancellationReason(),
            entity.getNote(),
            entity.getCreatedAt()
        );
    }
}
