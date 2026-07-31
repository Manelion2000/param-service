package com.bakouan.app.dto;

import java.time.ZonedDateTime;

public record AdminUserResponse(
        String id,
        String username,
        String nom,
        String prenom,
        String email,
        String telephone,
        String profile,
        Boolean activated,
        Boolean locked,
        Boolean passwordResetRequired,
        ZonedDateTime lastConnexionDate,
        String temporaryPassword
) {
}
