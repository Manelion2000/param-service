package com.bakouan.app.security;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class LoginAttemptService {


    /**
     * Nombre de tentatives autorisées.
     */
    @Value("${app.security.attempts.max:5}")
    private Integer maxAttempt;

    /**
     * Timeout ou delai d'attente avant de débloquer un compte
     * qui avait été bloqué pour cause de multiples tentatives.
     * En nombre de minutes
     */
    @Value("${app.security.attempts.timeout:5}")
    private Integer attemptTimeout;


    private LoadingCache<String, Integer> attemptsCache;

    /**
     * Constructeur du service.
     */
    public LoginAttemptService() {
        super();
    }

    /**
     * Connexion reussi.
     *
     * @param key
     */
    public void loginSucceeded(final String key) {
        getAttemptsCache().invalidate(key);
    }

    /**
     * Connexion echouée.
     * Donc il faut incrémenter le compteur à ce sujet.
     *
     * @param key
     */
    public void loginFailed(final String key) {
        log.info("Echec de de connexion de {}", key);

        int attempts = 0;
        try {
            attempts = getAttemptsCache().get(key);
        } catch (ExecutionException ignored) {
        }
        attempts++;
        getAttemptsCache().put(key, attempts);
    }


    /**
     * Vérifie si un host est bloqué.
     *
     * @param key
     * @return un boolean
     */
    public boolean isBlocked(final String key) {
        try {
            log.info("Nombre de tentatives de connexion de {} : {}", key, getAttemptsCache().get(key));
            return getAttemptsCache().get(key) >= maxAttempt;
        } catch (ExecutionException e) {
            return false;
        }
    }

    /**
     * Récupérer l'objet de stockage du cache.
     *
     * @return l'objet
     */
    public LoadingCache<String, Integer> getAttemptsCache() {
        if (attemptsCache == null) {
            attemptsCache = CacheBuilder
                    .newBuilder()
                    .expireAfterWrite(attemptTimeout, TimeUnit.MINUTES)
                    .build(new CacheLoader<String, Integer>() {

                        public Integer load(final String key) {
                            return 0;
                        }
                    });
        }
        return attemptsCache;
    }
}
