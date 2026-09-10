package com.cunoc.restaurant.ordering.dto;

import jakarta.validation.constraints.NotNull;

public record SplitLineDTO(
        @NotNull(message = "El identificador del ítem es obligatorio")
        Long orderItemId,

        @NotNull(message = "El identificador de la sub-cuenta es obligatorio")
        Long accountSplitId)
{ }
