package com.cunoc.restaurant.iam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** El empleado es el usuario: no existe personal sin credenciales. */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(length = 80, nullable = false)
    private String fullName;

    @Column(length = 40, nullable = false, unique = true)
    private String username;

    @Column(length = 72, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public boolean isActive()
    {
        return status == UserStatus.ACTIVE;
    }
}
