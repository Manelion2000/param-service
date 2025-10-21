package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entité représentant le profil de l'utilisateur.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "ba_profil")
public class BaProfil extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Column(name = "libelle", length = 60)
    @NotNull
    private String libelle;

    @Column(name = "description")
    private String description;

    @ManyToMany
    @JoinTable(name = "ba_profils_roles",
            joinColumns = {@JoinColumn(name = "profil_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id")})
    private Set<BaRole> roles = new HashSet<>();

    /**
     * @param uuid
     */
    public BaProfil(final String uuid) {
        this.id = uuid;
    }


    /**
     * Comparaison d'égalité entre deux objets.
     *
     * @param o obj
     * @return un boolean
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaProfil that = (BaProfil) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Hash code.
     *
     * @return un entier
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
