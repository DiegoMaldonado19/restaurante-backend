package com.cunoc.restaurant.common.dto;

/** Una linea de la lista fields[] que acompana a un 400 VALIDATION_ERROR. */
public record FieldErrorDTO(
        String field,
        String rejectedValue,
        String message)
{ }
