package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import com.cunoc.restaurant.ordering.model.OrderTicket;

import java.time.LocalDateTime;
import java.util.List;

public record OrderTicketView(
        Long orderTicketId,
        Long accountId,
        Long waiterId,
        LocalDateTime submittedAt,
        List<OrderItemView> items,
        OrderItemStatus derivedStatus)
{
    public static OrderTicketView from(OrderTicket ticket)
    {
        var items = ticket.getOrderItems();
        OrderItemStatus derived = items.stream()
                .map(OrderItem::getStatus)
                .min(OrderItemStatus::compareTo)
                .orElse(OrderItemStatus.RECEIVED);

        return new OrderTicketView(
                ticket.getOrderTicketId(),
                ticket.getAccount().getTableAccountId(),
                ticket.getWaiterId(),
                ticket.getSubmittedAt(),
                items.stream().map(OrderItemView::from).toList(),
                derived);
    }
}
