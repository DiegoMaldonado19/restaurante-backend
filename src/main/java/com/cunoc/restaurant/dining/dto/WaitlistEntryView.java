package com.cunoc.restaurant.dining.dto;

import com.cunoc.restaurant.dining.model.WaitlistEntry;
import com.cunoc.restaurant.dining.model.WaitlistStatus;

import java.time.LocalDateTime;

public record WaitlistEntryView(
    Long waitlistEntryId,
    Long customerId,
    String customerName,
    String customerPhone,
    int guestCount,
    WaitlistStatus status,
    Long restaurantTableId,
    Long tableAccountId,
    LocalDateTime arrivedAt,
    LocalDateTime seatedAt
)
{
    public static WaitlistEntryView from(WaitlistEntry entity, String customerName, String customerPhone)
    {
        return new WaitlistEntryView(
            entity.getWaitlistEntryId(),
            entity.getCustomerId(),
            customerName,
            customerPhone,
            entity.getGuestCount(),
            entity.getStatus(),
            entity.getRestaurantTableId(),
            entity.getTableAccountId(),
            entity.getArrivedAt(),
            entity.getSeatedAt()
        );
    }
}
