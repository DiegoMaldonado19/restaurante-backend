package com.cunoc.restaurant.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Propiedades del prefijo "restaurant". Se validan al arrancar, de modo que un
 * JWT_SECRET corto sea un fallo de arranque y no un fallo al primer login.
 * RestaurantApplication lleva @ConfigurationPropertiesScan; sin el, este record no se registra.
 */
@ConfigurationProperties("restaurant")
@Validated
public record RestaurantProperties(@Valid Security security, @Valid Cors cors)
{
    public record Security(@Valid Jwt jwt) { }

    public record Jwt(
            @NotBlank
            @Size(min = 32, message = "El secreto HS256 requiere al menos 32 bytes")
            String secret,

            @Positive
            int accessMinutes)
    { }

    public record Cors(
            @NotEmpty(message = "Se requiere al menos un origen permitido")
            List<String> allowedOrigins)
    { }
}
