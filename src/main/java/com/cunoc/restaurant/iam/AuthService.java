package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.iam.dto.LoginDTO;
import com.cunoc.restaurant.iam.dto.LoginView;
import com.cunoc.restaurant.iam.dto.UserView;
import com.cunoc.restaurant.iam.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticacion. No hay POST /auth/logout: con un JWT sin estado el servidor no puede
 * invalidar nada, asi que el cierre de sesion es del cliente.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService
{
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder   passwordEncoder;
    private final TokenService      tokenService;

    public LoginView login(LoginDTO request)
    {
        var user = appUserRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                                                         "Usuario o contrasena incorrectos."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
        {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                                        "Usuario o contrasena incorrectos.");
        }

        // Se comprueba despues de la contrasena para no revelar que la cuenta existe.
        if (!user.isActive())
        {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE,
                                        "Su cuenta esta desactivada. Contacte al administrador.");
        }

        return LoginView.from(user, tokenService.issue(user));
    }

    public UserView profileOf(Long userId)
    {
        return UserView.from(findOrFail(userId));
    }

    private AppUser findOrFail(Long userId)
    {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND,
                                                         "No existe el usuario " + userId + "."));
    }
}
