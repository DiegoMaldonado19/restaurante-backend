package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.common.security.CurrentUser;
import com.cunoc.restaurant.iam.dto.ChangePasswordDTO;
import com.cunoc.restaurant.iam.dto.CreateUserDTO;
import com.cunoc.restaurant.iam.dto.ResetPasswordDTO;
import com.cunoc.restaurant.iam.dto.UpdateUserDTO;
import com.cunoc.restaurant.iam.dto.UpdateUserStatusDTO;
import com.cunoc.restaurant.iam.dto.UserView;
import com.cunoc.restaurant.iam.model.UserRole;
import com.cunoc.restaurant.iam.model.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Personal", description = "Empleados del restaurante y su rol en la aplicacion de operacion")
public class UserController
{
    private final AppUserService appUserService;

    @GetMapping
    @Operation(summary = "Personal del restaurante, filtrable por rol, estado y texto libre")
    @ApiResponse(responseCode = "200", description = "Pagina de empleados")
    public PagedModel<UserView> findAll(@RequestParam(required = false) UserRole   role,
                                        @RequestParam(required = false) UserStatus status,
                                        @RequestParam(required = false) String     search,
                                        @ParameterObject                Pageable   pageable)
    {
        return new PagedModel<>(appUserService.search(role, status, search, pageable));
    }

    @PostMapping
    @Operation(summary = "Da de alta a un empleado con su rol",
               description = "Aqui todo empleado es un usuario: crea la persona y la cuenta a la vez.")
    @ApiResponse(responseCode = "201", description = "Empleado creado")
    @ApiResponse(responseCode = "409", description = "USERNAME_TAKEN")
    public ResponseEntity<UserView> create(@Valid @RequestBody CreateUserDTO request)
    {
        var user = appUserService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/users/" + user.userId()))
                .body(user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha del empleado")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public UserView findById(@PathVariable Long id)
    {
        return appUserService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica el nombre y el rol del empleado")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public UserView update(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO request)
    {
        return appUserService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa o desactiva al empleado",
               description = "Nunca se borra: hay cuentas, comandas y facturas que lo referencian.")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public UserView changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusDTO request)
    {
        return appUserService.changeStatus(id, request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambia la contrasena propia exigiendo la actual")
    @ApiResponse(responseCode = "204", description = "Contrasena actualizada")
    @ApiResponse(responseCode = "409", description = "CURRENT_PASSWORD_MISMATCH")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordDTO request)
    {
        appUserService.changeOwnPassword(CurrentUser.id(), request);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Repone la contrasena de un empleado que la olvido")
    @ApiResponse(responseCode = "204", description = "Contrasena repuesta")
    @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @Valid @RequestBody ResetPasswordDTO request)
    {
        appUserService.resetPassword(id, request);

        return ResponseEntity.noContent().build();
    }
}
