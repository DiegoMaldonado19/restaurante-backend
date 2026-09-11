package com.cunoc.restaurant.ordering.dto;

import java.math.BigDecimal;

public record DishModifierView(
        Long dishModifierId,
        String name,
        BigDecimal extraPrice)
{ }
