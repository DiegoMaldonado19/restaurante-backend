package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.constraints.NotNull;

public record MergeAccountDTO(
        @NotNull(message = "El identificador de la cuenta origen a fusionar es obligatorio")
        Long sourceAccountId)
{ }
