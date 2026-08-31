package com.cunoc.restaurant.iam.dto;

import com.cunoc.restaurant.iam.model.AppUser;
import com.cunoc.restaurant.iam.model.UserRole;
import com.cunoc.restaurant.iam.model.UserStatus;

import java.time.LocalDateTime;

public record UserView(
        Long          userId,
        String        fullName,
        String        username,
        UserRole      role,
        UserStatus    status,
        LocalDateTime createdAt)
{
    public static UserView from(AppUser user)
    {
        return new UserView(
                user.getUserId(),
                user.getFullName(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
