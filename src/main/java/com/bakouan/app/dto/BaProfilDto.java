package com.bakouan.app.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Dto représentant le profil de l'utilisateur.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class BaProfilDto {
    @EqualsAndHashCode.Include
    private String id;
    private String libelle;
    private String description;
    private Set<BaRoleDto> roles = new HashSet<>();

    /**
     * Constructeur avec l'identifiant.
     *
     * @param id
     */
    public BaProfilDto(final String id) {
        this.id = id;
    }


    /**
     * Modifier le libellé d'un profil.
     * Le libellé sera toujours converti en majuscule
     *
     * @param lbl : Le nouveau libellé
     */
    public void setLibelle(final String lbl) {
        if (lbl != null) {
            this.libelle = lbl.toUpperCase(Locale.ROOT);
        } else {
            this.libelle = null;
        }
    }
}
