package com.bakouan.app.dto;

import com.bakouan.app.enums.EAction;
import com.bakouan.app.model.BaLog;
import com.bakouan.app.utils.BaUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for {@link BaLog}.
 */
@Data
@NoArgsConstructor
public class BaLogDto implements Serializable {
    private String id = BaUtils.randomUUID();
    private String createdBy;
    private EAction action;
    private String ipAdresse;
    private String sujet;
    private String details;

    /**
     * Constructeur.
     *
     * @param action
     * @param sujet
     */
    public BaLogDto(final EAction action, final String sujet) {
        this.action = action;
        this.sujet = sujet;
    }
}
