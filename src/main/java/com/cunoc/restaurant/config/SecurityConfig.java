package com.cunoc.restaurant.config;

import com.cunoc.restaurant.common.dto.ErrorResponse;
import com.cunoc.restaurant.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Cadena sin estado: el token viaja en la cabecera y no hay sesion de servidor, que es
 * lo que permite que las dos SPA hablen con el mismo backend desde origenes distintos.
 * Los permisos finos ("un mesero solo edita comandas de sus propias mesas") dependen de
 * datos, asi que se comprueban en el Service y responden 403 FORBIDDEN_RESOURCE.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig
{
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        return http
                .csrf(AbstractHttpConfigurer::disable)          // API sin estado, token en cabecera
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        // Inventario: la aplicacion administrativa y nadie mas.
                        .requestMatchers("/api/v1/supplies/**",
                                         "/api/v1/supply-categories/**",
                                         "/api/v1/stock-movements",
                                         "/api/v1/stock-entries",
                                         "/api/v1/stock-wastes",
                                         "/api/v1/stock-adjustments").hasRole("ADMIN")
                        // Clientes: el mostrador da de alta, el administrador y la caja corrigen.
                        .requestMatchers(HttpMethod.POST, "/api/v1/customers")
                            .hasAnyRole("WAITER", "CASHIER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customers/*")
                            .hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/*/loyalty-transactions")
                            .hasAnyRole("ADMIN", "CASHIER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers", "/api/v1/customers/*")
                            .hasAnyRole("ADMIN", "WAITER", "CASHIER")
                        // Menu: la escritura y las recetas son del administrador; el salon y la
                        // cocina solo leen el catalogo. El orden importa: las reglas concretas van
                        // antes que los comodines /** de cierre, porque gana la primera que casa.
                        // El menu operativo es la primera llamada de la app de operacion.
                        .requestMatchers(HttpMethod.GET, "/api/v1/menu")
                            .hasAnyRole("WAITER", "KITCHEN")
                        // Disponibilidad manual: tambien cocina ("cocina debe poder marcarlo no disponible").
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/dishes/*/availability")
                            .hasAnyRole("ADMIN", "KITCHEN")
                        // El catalogo se lista tambien desde cocina; la ficha y los modificadores, salon.
                        .requestMatchers(HttpMethod.GET, "/api/v1/dishes")
                            .hasAnyRole("ADMIN", "WAITER", "KITCHEN")
                        // Recetas y su historial: solo el administrador (van antes del GET de ficha).
                        .requestMatchers(HttpMethod.GET, "/api/v1/dishes/*/recipe",
                                                         "/api/v1/dishes/*/recipe-versions",
                                                         "/api/v1/modifiers/*/recipe")
                            .hasRole("ADMIN")
                        // Lecturas de catalogo abiertas al salon: ficha, modificadores y combos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/dishes/*",
                                                         "/api/v1/dishes/*/modifiers",
                                                         "/api/v1/combos", "/api/v1/combos/*",
                                                         "/api/v1/dish-categories", "/api/v1/dish-categories/*")
                            .hasAnyRole("ADMIN", "WAITER")
                        // Todo lo demas de menu (altas, ediciones, bajas, recetas): solo administrador.
                        .requestMatchers("/api/v1/dishes/**",
                                         "/api/v1/modifiers/**",
                                         "/api/v1/combos/**",
                                         "/api/v1/dish-categories/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
                // Un 401 o un 403 los produce la cadena de filtros y no pasan por el
                // @RestControllerAdvice: sin esto saldrian con el cuerpo vacio.
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, request, ErrorCode.UNAUTHENTICATED,
                                           "Inicie sesion para continuar."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, request, ErrorCode.FORBIDDEN_RESOURCE,
                                           "Su rol no tiene acceso a este recurso.")))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request,
                            ErrorCode code, String message) throws IOException
    {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(),
                                ErrorResponse.business(code, message, request.getRequestURI()));
    }
}
