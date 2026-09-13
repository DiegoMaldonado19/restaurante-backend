package com.cunoc.restaurant.dining.dto;

import com.cunoc.restaurant.dining.model.CancellationReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelReservationDTO(
    @NotNull CancellationReason reason,
    @Size(max = 255) String note
)
{}
