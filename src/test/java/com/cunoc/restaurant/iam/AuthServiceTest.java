package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.iam.dto.LoginDTO;
import com.cunoc.restaurant.iam.model.AppUser;
import com.cunoc.restaurant.iam.model.UserRole;
import com.cunoc.restaurant.iam.model.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Las dos reglas con filo del login. Los CRUD sin regla no se prueban. */
class AuthServiceTest
{
    private static final String PASSWORD = "Admin123!";

    private final AppUserRepository   appUserRepository = mock(AppUserRepository.class);
    private final TokenService        tokenService      = mock(TokenService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final AuthService authService =
            new AuthService(appUserRepository, passwordEncoder, tokenService);

    @Test
    void rechazaLaContrasenaIncorrecta()
    {
        givenUser(UserStatus.ACTIVE);

        assertThatThrownBy(() -> authService.login(new LoginDTO("admin", "otraClave1")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void rechazaLaCuentaDesactivadaAunConLaContrasenaCorrecta()
    {
        givenUser(UserStatus.INACTIVE);

        assertThatThrownBy(() -> authService.login(new LoginDTO("admin", PASSWORD)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_INACTIVE);
    }

    @Test
    void emiteElTokenConElRolCuandoLasCredencialesSonValidas()
    {
        givenUser(UserStatus.ACTIVE);
        when(tokenService.issue(any())).thenReturn("un-token");

        var session = authService.login(new LoginDTO("admin", PASSWORD));

        assertThat(session.accessToken()).isEqualTo("un-token");
        assertThat(session.role()).isEqualTo(UserRole.ADMIN);
    }

    private void givenUser(UserStatus status)
    {
        var user = new AppUser();
        user.setUserId(1L);
        user.setFullName("Administrador");
        user.setUsername("admin");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.ADMIN);
        user.setStatus(status);

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
    }
}
