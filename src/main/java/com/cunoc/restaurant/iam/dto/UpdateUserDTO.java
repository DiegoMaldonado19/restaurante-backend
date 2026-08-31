package com.cunoc.restaurant.iam.dto;

import com.cunoc.restaurant.iam.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserDTO(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        UserRole role)
{ }
