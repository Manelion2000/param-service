package com.bakouan.app.security;

/**
 * Constants for Spring Security authorities.
 */
public final class BaRolesConstants {

    /**
     * L'administrateur de la plateforme.
     */
    public static final String BA_ADMIN = "BA_ADMIN";

    /**
     * Tout utilisateur disposant d'un compte(role commun).
     */
    public static final String BA_CONNECT = "BA_CONNECT";

    private BaRolesConstants() {
    }


}
