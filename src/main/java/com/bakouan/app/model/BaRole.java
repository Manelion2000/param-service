package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Entité représentant les roles des utilisateurs.
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
@Table(name = "ba_role")
public class BaRole extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Column(name = "libelle", unique = true)
    @NotNull
    private String libelle;

    @Column(name = "code", unique = true)
    @NotNull
    private String code;

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
        BaRole that = (BaRole) o;
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
