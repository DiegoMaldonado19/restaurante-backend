package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusDTO(
        @NotNull(message = "El nuevo estado es obligatorio")
        @io.swagger.v3.oas.annotations.media.Schema(enumAsRef = true, implementation = AccountStatus.class)
        AccountStatus status)
{ }
