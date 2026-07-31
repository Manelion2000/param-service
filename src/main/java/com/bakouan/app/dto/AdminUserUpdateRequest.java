package com.bakouan.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @NotBlank @Size(max = 50) String nom,
        @NotBlank @Size(max = 50) String prenom,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 30) String telephone,
        @NotBlank @Pattern(regexp = "ADMIN|AGENT", message = "Le profil doit etre ADMIN ou AGENT") String profile
) {
}
