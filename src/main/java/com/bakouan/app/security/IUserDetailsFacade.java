package com.bakouan.app.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Façade des détails de l'utilisateur.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
public interface IUserDetailsFacade {
    /**
     * Récupérer les informations de l'utilisateur.
     *
     * @return Objet avec les détails
     */
    UserDetails getUserDetails();
}
