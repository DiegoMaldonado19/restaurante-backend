package com.cunoc.restaurant.restaurant.dto;

import com.cunoc.restaurant.common.enums.TableZone;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** No lleva status: el estado solo se mueve por transitionTo(), nunca en una edicion. */
public record UpdateTableDTO(
        @Positive(message = "El numero de mesa debe ser mayor que cero")
        int tableNumber,

        @Positive(message = "La capacidad debe ser mayor que cero")
        @Max(value = 50, message = "La capacidad maxima es de 50 comensales")
        int capacity,

        @NotNull(message = "La zona es obligatoria")
        TableZone zone)
{ }