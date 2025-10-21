package com.bakouan.app.model;

import com.bakouan.app.enums.EAction;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "ba_log")
public class BaLog extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Enumerated
    @Column(name = "action")
    private EAction action;

    @Column(name = "ip_adresse")
    private String ipAdresse;

    @Column(name = "sujet")
    private String sujet;

    @Column(name = "details")
    private String details;
}
