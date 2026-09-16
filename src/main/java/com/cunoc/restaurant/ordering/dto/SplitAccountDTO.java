package com.cunoc.restaurant.ordering.dto;

import com.cunoc.restaurant.ordering.model.SplitMode;
import com.cunoc.restaurant.ordering.validation.ConsistentSplit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@ConsistentSplit
public record SplitAccountDTO(
        @NotNull(message = "El modo de división es obligatorio")
        SplitMode mode,

        @Min(value = 2, message = "El número mínimo de personas debe ser 2")
        @Max(value = 10, message = "El número máximo de personas permitido es 10")
        Integer personCount,

        java.util.List<SplitLineDTO> items)
{ }
