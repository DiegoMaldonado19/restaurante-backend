package com.cunoc.restaurant.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Emision y validacion del token. Con el starter de OAuth2 no se escribe ningun filtro. */
@Configuration
@RequiredArgsConstructor
public class JwtConfig
{
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RestaurantProperties properties;

    @Bean
    public JwtEncoder jwtEncoder()
    {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder()
    {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // El claim "role" trae un solo valor; JwtGrantedAuthoritiesConverter le antepone ROLE_.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter()
    {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return converter;
    }

    private SecretKey secretKey()
    {
        return new SecretKeySpec(properties.security().jwt().secret().getBytes(UTF_8), HMAC_ALGORITHM);
    }
}
