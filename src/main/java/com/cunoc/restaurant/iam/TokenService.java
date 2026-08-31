package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.config.RestaurantProperties;
import com.cunoc.restaurant.iam.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Emite el token de sesion. Vigencia de 12 horas: un turno completo de restaurante,
 * para que a nadie se le caiga la sesion a media cena. No hay refresh token.
 * iat y exp son Instant porque la especificacion del JWT es UTC por definicion;
 * es la unica marca de tiempo del sistema que no es LocalDateTime.
 */
@Service
@RequiredArgsConstructor
public class TokenService
{
    private final JwtEncoder           jwtEncoder;
    private final RestaurantProperties properties;

    public String issue(AppUser user)
    {
        var now    = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(user.getUserId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(properties.security().jwt().accessMinutes(), ChronoUnit.MINUTES))
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
