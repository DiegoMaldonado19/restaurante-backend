package com.cunoc.restaurant.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerDTO(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String fullName,

        @NotBlank(message = "El telefono es obligatorio")
        @Size(max = 20, message = "El telefono no puede pasar de 20 caracteres")
        @Pattern(regexp = "[0-9+\\-\\s]+", message = "El telefono solo admite digitos, espacios, + y -")
        String phone)
{ }
