package com.bakouan.app.dto;

public record AdminPasswordResetResponse(
        String userId,
        String username,
        String temporaryPassword
) {
}
