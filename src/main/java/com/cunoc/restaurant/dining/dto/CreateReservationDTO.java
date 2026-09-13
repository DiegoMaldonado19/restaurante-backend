package com.cunoc.restaurant.dining.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateReservationDTO(
    @NotBlank @Size(max = 80) String customerName,
    @NotBlank @Size(max = 20) String customerPhone,
    @NotNull Long tableId,
    @NotNull @Future LocalDateTime reservedAt,
    @NotNull @Min(1) Integer guestCount,
    @Size(max = 255) String note
)
{}
