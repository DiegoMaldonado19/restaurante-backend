package com.cunoc.restaurant.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

/**
 * Propiedades del prefijo "restaurant". Se validan al arrancar, de modo que un
 * JWT_SECRET corto sea un fallo de arranque y no un fallo al primer login.
 * RestaurantApplication lleva @ConfigurationPropertiesScan; sin el, este record no se registra.
 */
@ConfigurationProperties("restaurant")
@Validated
public record RestaurantProperties(@Valid Security security, @Valid Cors cors, @Valid Loyalty loyalty)
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

    /**
     * Semilla y respaldo del programa de puntos. La configuracion editable vive en
     * restaurant_setting, que V2__seed_data.sql ya sembro con estos mismos valores.
     * ponytail: respaldo de propiedades. Cuando restaurant exponga getSettings(), la
     * tasa se lee de la tabla y esto se queda solo como valor de arranque.
     */
    public record Loyalty(
            @NotNull
            @PositiveOrZero
            BigDecimal pointsPerCurrencyUnit)
    { }
}
