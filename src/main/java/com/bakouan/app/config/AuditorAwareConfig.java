package com.bakouan.app.config;

import com.bakouan.app.security.BaAuditorAwareImpl;
import com.bakouan.app.security.IUserDetailsFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration()
@RequiredArgsConstructor
public class AuditorAwareConfig {

    private final IUserDetailsFacade udf;

    /**
     * Implémentation de la récupération des informations
     * de l'utilisateur connecté.
     *
     * @return Un objet encapsulant le nom d'utilisateur
     */
    @Bean
    public org.springframework.data.domain.AuditorAware<String> auditorAware() {
        log.debug("Getting auditor aware");
        return new BaAuditorAwareImpl(udf);
    }
}
