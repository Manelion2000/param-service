package com.bakouan.app.config;

import com.bakouan.app.model.BaAbstractAuditingEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * Entité listener, pour faire varier la version
 * à chaque modification.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2022 Yandoama, All rights reserved.
 * @since : 29/04/2022 à 05:14
 */
@Slf4j
public class YtEntityVersionListener {

    /**
     * Mise à jour des informations d'une entité.
     *
     * @param entity
     */
    @PrePersist
    @PreUpdate
    private void beforeAnyUpdate(final BaAbstractAuditingEntity entity) {
        log.debug("Mise a jour de la classe : {}", entity.getClass());
        log.debug("Version actuelle : {}, Prochaine version : {}",
                entity.getVersion(), entity.getVersion() + 1);
        entity.setVersion(entity.getVersion() + 1);
    }
}
