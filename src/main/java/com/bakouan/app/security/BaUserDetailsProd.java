package com.bakouan.app.security;

import com.bakouan.app.utils.BaConstants;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author : <A HREF="mailto:lankoande.yabo.dev@gmail.com">Yienouyaba LANKOANDE (YaboLANK)</A>
 * @version : 1.0
 * Copyright (c) 2021 All rights reserved.
 * @since : 14/05/2021 à 12:18
 */
@Component
@NoArgsConstructor
@Profile("prod")
public class BaUserDetailsProd implements IUserDetailsFacade, CredentialsContainer, Serializable {

    private User user;

    /**
     * Constructeur.
     *
     * @param username
     * @param password
     * @param authorities
     */
    public BaUserDetailsProd(final String username, final String password,
                             final Collection<? extends GrantedAuthority> authorities) {
        this(username, password, true, true,
                true, true, authorities);
    }

    /**
     * Constructeur.
     *
     * @param username
     * @param password
     * @param enabled
     * @param accountNonExpired
     * @param credentialsNonExpired
     * @param accountNonLocked
     * @param authorities
     */
    public BaUserDetailsProd(final String username, final String password,
                             final boolean enabled,
                             final boolean accountNonExpired, final boolean credentialsNonExpired,
                             final boolean accountNonLocked, final Collection<? extends GrantedAuthority> authorities) {

        this.user = new User(username, password, enabled, accountNonExpired,
                credentialsNonExpired, accountNonLocked, authorities);
    }

    /**
     * Récupérer les informations de l'utilisateur.
     *
     * @return Objet avec les détails
     */
    @Override
    public UserDetails getUserDetails() {
        return new UserDetails() {

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return user.getAuthorities();
            }

            @Override
            public String getPassword() {
                return user.getPassword();
            }

            @Override
            public String getUsername() {
                if (user == null) {
                    return BaAuditorAwareImpl.getCurrentUserLogin().orElse(BaConstants.DEFAULT_USER);
                }
                return user.getUsername();
            }

            @Override
            public boolean isAccountNonExpired() {
                return user.isAccountNonExpired();
            }

            @Override
            public boolean isAccountNonLocked() {
                return user.isAccountNonLocked();
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return user.isCredentialsNonExpired();
            }

            @Override
            public boolean isEnabled() {
                return user.isEnabled();
            }
        };
    }

    /**
     * Vider les credentials.
     */
    @Override
    public void eraseCredentials() {
        if (user != null) {
            user.eraseCredentials();
        }
    }


}
