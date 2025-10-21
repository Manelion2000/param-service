package com.bakouan.app.model;

import com.bakouan.app.enums.ESexe;
import com.bakouan.app.utils.BaUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.Locale;

/**
 * Entité représentant un utilisateur de l'application.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ba_utilisateur")
public class BaUser extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "username", length = 50, unique = true, updatable = false, nullable = false)
    private String username;

    @NotNull
    @Column(name = "password_hash", length = 254)
    private String password;

    @Size(max = 50)
    @Column(name = "nom", length = 50)
    private String nom;

    @Size(max = 50)
    @Column(name = "prenom", length = 50)
    private String prenom;

    @Column(length = 100)
    private String email;

    @NotNull
    @Column(name = "account_locked", nullable = false)
    private Boolean locked = false;

    @Size(max = 20)
    @Column(name = "reset_key", length = 20)
    @JsonIgnore
    private String resetKey;

    @Column(name = "reset_date")
    private ZonedDateTime resetDate = null;

    /**
     * Date de la dernière connexion.
     */
    @Column(name = "last_connexion_date")
    private ZonedDateTime lastConnexionDate;

    @NotNull
    @Column(name = "telephone", nullable = false, unique = true)
    private String telephone;

    @ManyToOne
    @JoinColumn(name = "profil_uuid")
    private BaProfil profil;

    /**
     * Spécifie si l'utilisateur a été activé ou pas.
     * C'est encore utilisé au cas où, l'utilisateur
     * demande une réinitialisation de son mot de passe.
     */
    @Column(name = "activated")
    private Boolean activated = Boolean.FALSE;

    /**
     * Le genre de l'utilisateur.
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = "sexe")
    private ESexe sexe;

    @Column(name = "indicatif_pays")
    private String indicatifPays;

    /**
     * Constructeur initialisation id.
     *
     * @param uuid
     */
    public BaUser(final String uuid) {
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
                + username + "', password='" + password
                + ", nom=''" + nom + "', prenom='" + prenom
                + "', email='" + email + "', locked=" + locked + "', resetKey='"
                + resetKey + "', resetDate=" + resetDate + ", telephone='"
                + telephone + ", profil=" + profil + "}";
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
}
