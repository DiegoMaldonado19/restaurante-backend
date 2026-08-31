package com.cunoc.restaurant.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger / OpenAPI: requisito literal del enunciado e insumo del manual tecnico. */
@Configuration
public class OpenApiConfig
{
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI restaurantOpenApi()
    {
        var bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("API del Sistema de Gestion de Restaurante")
                        .description("Backend unico que consumen la aplicacion administrativa y la de operacion.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    /**
     * El ModelResolver de swagger-core no lee spring.jackson.property-naming-strategy:
     * construye su propio ObjectMapper y publicaria el esquema en camelCase, rompiendo
     * el contrato con los dos frontends, que copian sus tipos de /v3/api-docs.
     * El ObjectMapper es de Jackson 2 a proposito: swagger-core no conoce Jackson 3,
     * y Boot 4 deja las dos versiones en el classpath.
     */
    @Bean
    public ModelResolver modelResolver()
    {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        return new ModelResolver(mapper);
    }
}
