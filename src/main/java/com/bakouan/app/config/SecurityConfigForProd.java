package com.bakouan.app.config;


import com.bakouan.app.security.BaRolesConstants;
import com.bakouan.app.security.BaUserDetailsService;
import com.bakouan.app.security.jwt.JWTConfigurer;
import com.bakouan.app.security.jwt.JWTFilter;
import com.bakouan.app.security.jwt.TokenProvider;
import com.bakouan.app.utils.BaConstants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Profile("prod")
@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfigForProd {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final BaUserDetailsService userDetailsService;
    private final TokenProvider tokenProvider;

    /**
     * Initiateur après l'appel du constructeur.
     */
    @PostConstruct
    public void init() {
        try {
            authenticationManagerBuilder
                    .userDetailsService(userDetailsService)
                    .passwordEncoder(new BCryptPasswordEncoder());
        } catch (Exception e) {
            throw new BeanInitializationException("Security configuration failed", e);
        }
    }

    /**
     * Build AuthenticationProvider.
     *
     * @return AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    /**
     * Build AuthenticationManager.
     *
     * @param config
     * @return AuthenticationManager
     * @throws Exception
     */
    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Build a password encoder.
     *
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure security.
     *
     * @param http
     * @return chain
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain configure(final HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.addFilterBefore(corsFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> {
                });

        http.headers(h -> {
            h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
        });
        http.sessionManagement(session -> {
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });

        http.authorizeHttpRequests(auth -> {
            auth
                    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                    .requestMatchers(BaConstants.URL.BASE_URL + "/activate").permitAll()
                    .requestMatchers(BaConstants.URL.BASE_URL + BaConstants.URL.AUTHENTICATE).permitAll()
                    .requestMatchers(HttpMethod.GET, BaConstants.URL.CSRF_TOKEN).permitAll()
                    .requestMatchers(BaConstants.URL.BASE_URL + "/reset/**").permitAll()
                    .requestMatchers(BaConstants.URL.BASE_URL + "/admin/**").hasAuthority(BaRolesConstants.BA_ADMIN)
                    .requestMatchers(BaConstants.URL.BASE_URL + BaConstants.URL.USER).hasAuthority(BaRolesConstants.BA_ADMIN)
                    .requestMatchers(BaConstants.URL.BASE_URL + BaConstants.URL.USER + "/{id}/activate").hasAuthority(BaRolesConstants.BA_ADMIN)
                    .requestMatchers(BaConstants.URL.BASE_URL + "/register").hasAuthority(BaRolesConstants.BA_ADMIN)
                    .requestMatchers(HttpMethod.GET, BaConstants.URL.BASE_URL + BaConstants.URL.CATEGORIE).permitAll()
                    .requestMatchers(HttpMethod.GET, BaConstants.URL.BASE_URL + BaConstants.URL.PRODUCT)
                    .hasAuthority(BaRolesConstants.BA_ADMIN)
                    .requestMatchers(BaConstants.URL.BASE_URL + "/**").authenticated();
        });

        http.with(securityConfigurerAdapter(), a -> {

        });


        return http.build();
    }

    /**
     * Build cors filter.
     *
     * @return CorsFilter
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(List.of(JWTFilter.AUTHORIZATION_HEADER));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }

    private JWTConfigurer securityConfigurerAdapter() {
        return new JWTConfigurer(tokenProvider, userDetailsService);
    }

    /**
     * RestTemplate for the profile dev.
     *
     * @param builder RestTemplateBuilder
     * @return restTemplate
     */
    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder builder) {
        return builder.build();
    }

}
