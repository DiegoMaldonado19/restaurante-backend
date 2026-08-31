package com.cunoc.restaurant.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reposicion por el administrador: no pide la contrasena actual, porque el empleado la olvido. */
public record ResetPasswordDTO(
        @NotBlank(message = "La contrasena nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contrasena tiene entre 8 y 72 caracteres")
        String newPassword)
{ }
