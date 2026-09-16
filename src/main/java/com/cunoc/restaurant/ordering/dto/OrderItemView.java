package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.OrderItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderItemView(
        Long orderItemId,
        Long dishId,
        String dishName,
        List<DishModifierView> modifiers,
        Long comboId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal unitCost,
        String note,
        OrderItemStatus status,
        LocalDateTime submittedAt,
        LocalDateTime readyAt,
        LocalDateTime deliveredAt,
        boolean overdue,
        LocalDateTime accountSplitCreatedAt)
{ }
