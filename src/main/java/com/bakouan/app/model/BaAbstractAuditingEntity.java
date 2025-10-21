package com.bakouan.app.model;


import com.bakouan.app.config.YtEntityVersionListener;
import com.bakouan.app.enums.EStatut;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Base abstract class for entities which will hold definitions for created, last modified by and created,
 * last modified by date.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) Yandoama Consulting 2021 All rights reserved.
 * @since : 14/05/2024 à 12:18
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, YtEntityVersionListener.class})
public abstract class BaAbstractAuditingEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    @Column(name = "statut")
    @Enumerated(EnumType.STRING)
    private EStatut statut = EStatut.A;

    /**
     * Version actuelle de l'entité.
     */
    @Column(name = "version")
    private Long version = 1L;

    /**
     * Version actuelle.
     *
     * @return la version actuelle ou 1
     */
    public Long getVersion() {
        return version == null ? Long.valueOf("1") : version;
    }
}
