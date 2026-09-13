package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderItemStatusDTO(
        @NotNull(message = "El nuevo estado es obligatorio")
        @io.swagger.v3.oas.annotations.media.Schema(enumAsRef = true, implementation = OrderItemStatus.class)
        OrderItemStatus status)
{ }
