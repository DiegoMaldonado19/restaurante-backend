package com.cunoc.restaurant.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 4, max = 40, message = "El usuario tiene entre 4 y 40 caracteres")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena tiene entre 8 y 72 caracteres")
        String password)
{ }
