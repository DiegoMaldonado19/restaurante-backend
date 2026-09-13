package com.cunoc.restaurant.dining.dto;

import jakarta.validation.constraints.NotNull;

public record SeatWaitlistDTO(
    @NotNull Long tableId
)
{}
