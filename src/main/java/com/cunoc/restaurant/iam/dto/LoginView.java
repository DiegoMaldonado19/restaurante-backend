package com.cunoc.restaurant.iam.dto;

import com.cunoc.restaurant.iam.model.AppUser;
import com.cunoc.restaurant.iam.model.UserRole;

/** El nombre viaja con el token para que la barra superior no necesite una segunda llamada. */
public record LoginView(
        String   accessToken,
        UserRole role,
        Long     userId,
        String   fullName)
{
    public static LoginView from(AppUser user, String accessToken)
    {
        return new LoginView(accessToken, user.getRole(), user.getUserId(), user.getFullName());
    }
}
