package com.bakouan.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Object to return as body in JWT Authentication.
 */
@AllArgsConstructor
@Data
public class BaJWTTokenDto {

    private Instant date;

    private String token;

    private long expiresIn;

    /**
     * Fonction de récupération du token.
     *
     * @return le token
     */
    @JsonProperty("access_token")
    public String getToken() {
        return token;
    }

    /**
     * Fonction de récupération du délai d'expiration du token.
     *
     * @return le token
     */
    @JsonProperty("expires_in")
    public long getExpiresIn() {
        return expiresIn;
    }
}
