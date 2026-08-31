package com.cunoc.restaurant.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Las dos aplicaciones renderizan en servidor, pero todas las llamadas a la API salen
 * del navegador: CORS aplica de verdad. Dos origenes explicitos, nunca comodin.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig
{
    private final RestaurantProperties properties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.cors().allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
