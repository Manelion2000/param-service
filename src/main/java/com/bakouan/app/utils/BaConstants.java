package com.bakouan.app.utils;

import org.springframework.core.io.ClassPathResource;

public class BaConstants {
    public static final String DEFAULT_USER = "dev@user";
    public static final String ROLE_PREFIX = "BA_";

    /**
     * Constantes des URLs.
     */
    public static class URL {
        public static final  String BASE_URL = "/api";
        public static final String PROFIL = "/profils";
        public static final String ROLE = "/roles";
        public static final String DOCUMENT = "/documents";
        public static final String CSRF_TOKEN = "/csrf";
        public static final String AUTHENTICATE = "/authenticate";
        public static final String USER = "/users";
        public static final String CATEGORIE = "/categories";
        public static final String PRODUCT = "/products";
    }

    /**
     * Classes des constantes liées à l'édition.
     */
    public static class REPORTS {
        public static final String LOGO_URL = new ClassPathResource("/images/logo.png").getPath();
        public static final String PARAM_TITLE = "BA_TITLE";

        /**
         * Racines des reports.
         */
        private static final String REPORT_ROOT = "reports/";
        public static final String REPORT_PRODUIT = REPORT_ROOT + "produit.jasper";
    }

}
