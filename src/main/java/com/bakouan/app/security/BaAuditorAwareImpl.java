package com.bakouan.app.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import com.bakouan.app.utils.BaConstants;

@Slf4j
@RequiredArgsConstructor
public class BaAuditorAwareImpl implements AuditorAware<String> {

    private final IUserDetailsFacade udf;

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .map(authentication -> {
                    if (authentication.getPrincipal() instanceof UserDetails) {
                        UserDetails springSecurityUser = (UserDetails) authentication.getPrincipal();
                        return springSecurityUser.getUsername();
                    } else if (authentication.getPrincipal() instanceof String) {
                        return (String) authentication.getPrincipal();
                    }
                    return null;
                });
    }

    /**
     * Récupérer le nom de l'utilisateur connecté.
     *
     * @return Optional contenant le nom de l'utilisateur
     */
    @Override
    public Optional<String> getCurrentAuditor() {
        String username = Optional.ofNullable(udf.getUserDetails())
                .map(UserDetails::getUsername)
                .orElseGet(() -> getCurrentUserLogin().orElse(BaConstants.DEFAULT_USER));
        log.debug("--== Recuperation du username = {} --==", username);
        return Optional.of(username);
    }

}
