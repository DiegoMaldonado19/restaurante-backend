package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.iam.dto.LoginDTO;
import com.cunoc.restaurant.iam.dto.LoginView;
import com.cunoc.restaurant.iam.dto.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Inicio de sesion y perfil del usuario autenticado")
public class AuthController
{
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Autentica al usuario y emite el token de sesion",
               description = "El token es un JWT HS256 con vigencia de 12 horas. No existe /auth/logout: "
                           + "con un token sin estado el cierre de sesion es del cliente.")
    @ApiResponse(responseCode = "200", description = "Token emitido")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
    @ApiResponse(responseCode = "403", description = "ACCOUNT_INACTIVE")
    public LoginView login(@Valid @RequestBody LoginDTO request)
    {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil y rol con los que la interfaz decide que mostrar")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario autenticado")
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED")
    public UserView me()
    {
        return authService.profileOf(CurrentUser.id());
    }
}
