package com.cunoc.restaurant.restaurant.dto;

import com.cunoc.restaurant.common.enums.TableStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTableStatusDTO(
        @NotNull(message = "El estado es obligatorio")
        TableStatus status)
{ }