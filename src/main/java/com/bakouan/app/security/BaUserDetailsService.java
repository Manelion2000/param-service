package com.bakouan.app.security;


import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pour intercepter l'authenticvation des utilisateurs afin d'authentifier.
 *
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Component()
@Slf4j
@RequiredArgsConstructor
public class BaUserDetailsService implements UserDetailsService {

    private final BaUserRepository userRepository;

    private final IUserDetailsFacade udf;

    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);
    private final LoginAttemptService loginAttemptService;
    private final HttpServletRequest request;

    /**
     * Timeout ou delai d'attente avant de débloquer un compte qui avait
     * été bloqué pour cause de multiples tentatives.
     * En nombre de minutes.
     */
    @Value("${app.security.attempts.timeout:5}")
    private Integer attemptTimeout;

    /**
     * Nombre de tentatives autorisées.
     */
    @Value("${app.security.attempts.max:5}")
    private Integer maxAttempt;

    /**
     * Authentication en surchargeant la methode loadUserByUsername.
     *
     * @return les détails de l'utilisateur
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(final String username) {

        final String ip = getClientIP();

        if (loginAttemptService.isBlocked(ip)) {
            log.debug("Utilisateur blocké : {}", ip);
            final int maxTentatives = maxAttempt;
            log.warn("Utilisateur blocké suite à {} tentatives de connexion. Clé : {}", maxAttempt, ip);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Votre poste a été blocké suite à "
                            + maxTentatives
                            + " tentatives de connexion. "
                            + "Reessayez plus tard s'il vous plaît.");
        } else {
            log.debug("Utilisateur non blocké : {}", ip);
        }

        log.debug("Loading user details from username : {}", username);

        try {
            Optional<BaUser> userFromDatabase = this.userRepository
                    .findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
            return userFromDatabase.map(user -> {
                List<GrantedAuthority> ga = user.getProfil().getRoles().stream()
                        .map(authority -> new SimpleGrantedAuthority(authority.getCode()))
                        .collect(Collectors.toList());

                if (!Boolean.TRUE.equals(user.getActivated())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre compte n'a pas été activé.");
                }

                if (Boolean.TRUE.equals(user.getLocked())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre compte a été bloqué. Merci "
                            + "de contacter l'administrateur.");
                }

                loginAttemptService.loginSucceeded(ip);

                user.setLastConnexionDate(ZonedDateTime.now());

                return new BaUserDetailsProd(user.getUsername().toLowerCase(), user.getPassword(), ga)
                        .getUserDetails();


            }).orElseThrow(() -> new UsernameNotFoundException("L'utilisateur " + username + " n'existe pas."));
        } catch (Exception e) {
            log.error("Erreur d'authentification", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations de connexion sont invalides");
        }
    }

    /**
     * Vérifier la validité d'un compte.
     * Si le compte n'est pas bloqué, actif, etc.
     *
     * @param username
     */
    public void checkValiditeCompte(final String username) {
        Optional<BaUser> userFromDatabase = this.userRepository
                .findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
        userFromDatabase.map(user -> {
            if (!Boolean.TRUE.equals(user.getActivated())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre compte n'a pas été activé.");
            }

            if (Boolean.TRUE.equals(user.getLocked())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre compte a été bloqué. Merci "
                        + "de contacter l'administrateur.");
            }
            return user;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le compte " + username + " est introuvable."));
    }


    /**
     * Récupérer l'adresse IP de l'utilisateur connecté.
     *
     * @return L'adresse IP
     */
    private String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr() + request.getRemotePort();
        }
        return xfHeader.split(",")[0];
    }


    /**
     * Recupere un utilisateur par son nom d'utilisateur.
     *
     * @param username
     * @return un optional de l'utilisateur
     */
    public Optional<BaUser> getOneByUsername(final String username) {
        return this.userRepository
                .findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
    }


}
