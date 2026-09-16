package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TableAccountView(
        Long tableAccountId,
        Long restaurantTableId,
        Integer guestCount,
        AccountStatus status,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        SplitsInfo splits,
        List<OrderTicketView> tickets,
        BigDecimal runningTotal,
        Long waiterId,
        String waiterName)
{
    public record SplitsInfo(int count, BigDecimal totalAmount) {}
}
