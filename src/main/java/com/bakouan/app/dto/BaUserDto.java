package com.bakouan.app.dto;

import com.bakouan.app.enums.ESexe;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * DTO représentant un utilisateur de l'application.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 29/07/2021 à 12:18
 */
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class BaUserDto {

    @EqualsAndHashCode.Include
    private String id;

    @NotEmpty(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 5, max = 50, message = "Le nom d'utilisateur doit comporter au moins 5 caractères")
    private String username;

    @Size(max = 50, message = "Le nom ne doit pas excéder 50 caractères")
    private String nom;

    @Size(max = 50, message = "Le prénom ne doit pas excéder 50 caractères")
    private String prenom;

    @Email(message = "Le mail n'est pas correct.")
    private String email;

    private Boolean locked = Boolean.FALSE;

    @JsonIgnore
    private String password;

    @JsonIgnore
    private String resetKey;

    @JsonIgnore
    private ZonedDateTime resetDate = null;

    /**
     * Spécifie si l'utilisateur a été activé ou pas.
     * C'est encore utilisé au cas où, l'utilisateur
     * demande une réinitialisation de son mot de passe.
     */
    private Boolean activated = Boolean.FALSE;

    @NotEmpty(message = "Le téléphone est obligatoire")
    private String telephone;

    private String idProfil;

    private String libelleProfil;

    private Set<BaRoleDto> roles;

    /**
     * Date de la dernière connexion.
     */
    private ZonedDateTime lastConnexionDate;

    /**
     * Le genre de l'utilisateur.
     */
    @NotNull(message = "Sexe obligatoire")
    private ESexe sexe;

    private BaCredentialDto credentials;

    private String indicatifPays;

    /**
     * Constructeur initialisation id.
     *
     * @param uuid
     */
    public BaUserDto(final String uuid) {
        this.id = uuid;
    }

    /**
     * Redefinition de la méthode d'affichage.
     *
     * @return String
     */
    @Override
    public String toString() {
        return "User{id='" + id + "', username='"
                + username
                + ", nom=''" + nom + "', prenom='" + prenom
                + "', email='" + email + "', locked=" + locked + ", telephone='"
                + telephone + ", profil=" + idProfil + "}";
    }

    /**
     * Le nom d'utilisateur est enregistré toujours en majuscule.
     *
     * @param login
     */
    public void setUsername(final String login) {
        if (login != null) {
            this.username = login.toLowerCase(Locale.ENGLISH);
        } else {
            this.username = null;
        }
    }

    /**
     * Modification de l'email.
     *
     * @param email
     */
    public void setEmail(final String email) {
        this.email = email;
        setUsername(email);
    }


    /**
     * Modifier le nom d'un utilisateur.
     * Le nom sera toujours converti en majuscule pour faciliter
     * la recherche.
     *
     * @param pNom : Le nouveau nom
     */
    public void setNom(final String pNom) {
        if (pNom != null) {
            this.nom = pNom.toUpperCase(Locale.ROOT);
        } else {
            this.nom = null;
        }
    }

    /**
     * Modifier le prénom d'un utilisateur.
     * Le nom sera toujours converti en majuscule pour faciliter
     * la recherche.
     *
     * @param pPrenom : Le nouveau prénom
     */
    public void setPrenom(final String pPrenom) {
        if (pPrenom != null) {
            this.prenom = pPrenom.toUpperCase(Locale.ROOT);
        } else {
            this.prenom = null;
        }
    }
}
