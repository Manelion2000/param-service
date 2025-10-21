package com.bakouan.app.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Classe d'intercepteur.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class IntercepteurDeconnexion extends AbstractAuthenticationTargetUrlRequestHandler
        implements LogoutSuccessHandler {

    private final LoginAttemptService loginAttemptService;

    /**
     * Fonction appelé à la déconnexion de l'utilisateur.
     *
     * @param request
     * @param response
     * @param authentication
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void onLogoutSuccess(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Authentication authentication) throws IOException, ServletException {

        log.info("Deconnexion");

        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.trim().isEmpty()) {
            loginAttemptService.loginFailed(request.getRemoteAddr() + request.getRemotePort());
        } else {
            loginAttemptService.loginFailed(xfHeader.split(",")[0]);
        }
        response.setStatus(HttpServletResponse.SC_OK);
    }
}
