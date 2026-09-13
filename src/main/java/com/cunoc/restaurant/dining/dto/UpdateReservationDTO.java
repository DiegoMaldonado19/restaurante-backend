package com.cunoc.restaurant.dining.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record UpdateReservationDTO(
    @NotNull Long tableId,
    @NotNull @Future LocalDateTime reservedAt,
    @NotNull @Min(1) Integer guestCount
)
{}
