package com.cunoc.restaurant.common.security;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * El id del usuario autenticado, leido del claim "sub" del token. Lo necesitan todos
 * los modulos que registran un usuario responsable: inventory, ordering, billing y cashbox.
 */
public final class CurrentUser
{
    private CurrentUser() { }

    public static Long id()
    {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt))
        {
            throw new BusinessException(ErrorCode.UNAUTHENTICATED, "No hay una sesion activa.");
        }

        return Long.valueOf(jwt.getSubject());
    }
}
