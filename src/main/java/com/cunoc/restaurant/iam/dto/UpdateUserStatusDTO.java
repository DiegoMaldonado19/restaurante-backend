package com.cunoc.restaurant.iam.dto;

import com.cunoc.restaurant.iam.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusDTO(
        @NotNull(message = "El estado es obligatorio")
        UserStatus status)
{ }
