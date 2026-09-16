package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.OrderItemStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderTicketView(
        Long orderTicketId,
        Long accountId,
        Long waiterId,
        LocalDateTime submittedAt,
        List<OrderItemView> items,
        OrderItemStatus derivedStatus)
{ }
