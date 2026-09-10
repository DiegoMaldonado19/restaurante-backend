package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelOrderItemDTO(
        @NotBlank(message = "El motivo de la cancelación es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
        String reason)
{ }
