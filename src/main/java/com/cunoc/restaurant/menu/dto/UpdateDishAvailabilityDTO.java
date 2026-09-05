package com.cunoc.restaurant.menu.dto;

import jakarta.validation.constraints.NotNull;

/** La bandera manual que apagan el administrador o cocina. No afecta el calculo automatico por stock. */
public record UpdateDishAvailabilityDTO(
        @NotNull(message = "La disponibilidad es obligatoria")
        Boolean manualAvailable)
{ }
