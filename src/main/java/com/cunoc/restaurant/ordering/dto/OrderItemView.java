package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.OrderItem;
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
{
    public static OrderItemView from(OrderItem item)
    {
        return new OrderItemView(
                item.getOrderItemId(),
                item.getDishId(),
                "DishNamePlaceholder",
                List.of(),
                item.getComboId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitCost(),
                item.getNote(),
                item.getStatus(),
                item.getSubmittedAt(),
                item.getReadyAt(),
                item.getDeliveredAt(),
                false,
                item.getSplit() != null ? item.getSplit().getCreatedAt() : null);
    }
}
