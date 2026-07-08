package com.bakouan.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BaRegisterDto {
    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 50, message = "Le nom ne doit pas exceder 50 caracteres")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    @Size(max = 50, message = "Le prenom ne doit pas exceder 50 caracteres")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email n'est pas valide")
    @Size(max = 50, message = "L'email ne doit pas exceder 50 caracteres")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 4, max = 50, message = "Le mot de passe doit contenir entre 4 et 50 caracteres")
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    @Size(min = 4, max = 50, message = "La confirmation doit contenir entre 4 et 50 caracteres")
    private String confirmation;
}
