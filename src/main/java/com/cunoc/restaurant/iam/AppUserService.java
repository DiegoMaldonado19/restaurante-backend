package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.iam.dto.ChangePasswordDTO;
import com.cunoc.restaurant.iam.dto.CreateUserDTO;
import com.cunoc.restaurant.iam.dto.ResetPasswordDTO;
import com.cunoc.restaurant.iam.dto.UpdateUserDTO;
import com.cunoc.restaurant.iam.dto.UpdateUserStatusDTO;
import com.cunoc.restaurant.iam.dto.UserView;
import com.cunoc.restaurant.iam.model.AppUser;
import com.cunoc.restaurant.iam.model.UserRole;
import com.cunoc.restaurant.iam.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Personal del restaurante. Los otros modulos solo llaman findById() y requireRole(). */
@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService
{
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder   passwordEncoder;

    @Transactional(readOnly = true)
    public Page<UserView> search(UserRole role, UserStatus status, String search, Pageable pageable)
    {
        return appUserRepository.search(role, status, search, pageable).map(UserView::from);
    }

    @Transactional(readOnly = true)
    public UserView findById(Long userId)
    {
        return UserView.from(findOrFail(userId));
    }

    /** Permiso grueso comprobado desde otro modulo. Lanza FORBIDDEN_RESOURCE. */
    @Transactional(readOnly = true)
    public void requireRole(Long userId, UserRole role)
    {
        if (findOrFail(userId).getRole() != role)
        {
            throw new BusinessException(ErrorCode.FORBIDDEN_RESOURCE,
                                        "La operacion requiere el rol " + role + ".");
        }
    }

    public UserView create(CreateUserDTO request)
    {
        if (appUserRepository.existsByUsername(request.username()))
        {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN,
                                        "El usuario '" + request.username() + "' ya existe.");
        }

        var user = new AppUser();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        return UserView.from(appUserRepository.save(user));
    }

    public UserView update(Long userId, UpdateUserDTO request)
    {
        var user = findOrFail(userId);
        user.setFullName(request.fullName());
        user.setRole(request.role());

        return UserView.from(user);
    }

    /** Nunca se borra un empleado: hay cuentas, comandas y facturas que lo referencian. */
    public UserView changeStatus(Long userId, UpdateUserStatusDTO request)
    {
        var user = findOrFail(userId);
        user.setStatus(request.status());

        return UserView.from(user);
    }

    /** Reposicion por el administrador. Sin esto, quien pierde la contrasena queda fuera para siempre. */
    public void resetPassword(Long userId, ResetPasswordDTO request)
    {
        findOrFail(userId).setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public void changeOwnPassword(Long userId, ChangePasswordDTO request)
    {
        var user = findOrFail(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))
        {
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH,
                                        "La contrasena actual no coincide.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private AppUser findOrFail(Long userId)
    {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND,
                                                         "No existe el usuario " + userId + "."));
    }
}
