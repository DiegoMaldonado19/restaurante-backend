package com.cunoc.restaurant.dining.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWaitlistEntryDTO(
    @NotBlank @Size(max = 80) String customerName,
    @NotBlank @Size(max = 20) String customerPhone,
    @NotNull @Min(1) Integer guestCount
)
{}
