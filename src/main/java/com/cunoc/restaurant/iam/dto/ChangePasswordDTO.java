package com.cunoc.restaurant.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio propio: exige la contrasena actual. */
public record ChangePasswordDTO(
        @NotBlank(message = "La contrasena actual es obligatoria")
        String currentPassword,

        @NotBlank(message = "La contrasena nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena tiene entre 8 y 72 caracteres")
        String newPassword)
{ }
