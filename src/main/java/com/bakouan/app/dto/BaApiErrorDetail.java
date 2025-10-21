package com.bakouan.app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Cette classe fournit les détails du message de retour de manière élégante.
 *
 * @author : <A HREF="mailto:abdramanbakouan@gmail.com">Abdramane BAKOUAN (Manelion2000)</A>
 * @version : 1.0
 * Copyright (c) 2022 Cromadev, All rights reserved.
 * @since : 29/04/2020 à 15:14
 */
@Data
public class BaApiErrorDetail {

    private HttpStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime errorDate;

    private String message;

    @JsonIgnore()
    private String debugMessage;

    private BaApiErrorDetail() {
        errorDate = LocalDateTime.now();
    }

    /**
     * Constructeur.
     *
     * @param httpStatus Objet de type HttpStatus
     */
    public BaApiErrorDetail(final HttpStatus httpStatus) {
        this();
        this.status = httpStatus;
    }

    /**
     * Constructeur.
     *
     * @param pStatus Objet de type HttpStatus
     * @param ex      Objet de type Throwable
     */
    public BaApiErrorDetail(final HttpStatus pStatus, final Throwable ex) {
        this();
        this.status = pStatus;
        this.message = "Unexpected  error";
        this.debugMessage = ex.getLocalizedMessage();
    }

    /**
     * Constructeur.
     *
     * @param httpStatus Objet de type HttpStatus
     * @param pMessage   Message d'erreur ou d'information
     * @param ex         Objet de type Throwable
     */
    public BaApiErrorDetail(final HttpStatus httpStatus,
                            final String pMessage,
                            final Throwable ex) {
        this();
        this.status = httpStatus;
        this.message = pMessage;
        if (ex != null) {
            this.debugMessage = ex.getLocalizedMessage();
        }
    }

}
