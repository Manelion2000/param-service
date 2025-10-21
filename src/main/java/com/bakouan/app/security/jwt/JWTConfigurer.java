package com.bakouan.app.security.jwt;

import com.bakouan.app.security.BaUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Classe de configuration de JWT.
 */
@Profile(value = "prod")
@Configuration()
@RequiredArgsConstructor
public class JWTConfigurer extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final TokenProvider tokenProvider;

    private final BaUserDetailsService userDetailsService;

    /**
     * Fonction de configuration.
     *
     * @param http http
     */
    @Override
    public void configure(final HttpSecurity http) {
        http.addFilterBefore(new JWTFilter(tokenProvider, userDetailsService),
                UsernamePasswordAuthenticationFilter.class);
    }
}
