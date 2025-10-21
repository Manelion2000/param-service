package com.bakouan.app.security.jwt;

import com.bakouan.app.dto.BaJWTTokenDto;
import com.bakouan.app.utils.BaConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private static final long TOKEN_VALIDITY_IN_MILLISECONDS = 86400 * 1000;
    private static final long TOKEN_VALIDITY_IN_MILLISECONDS_FOR_REMEMBER_ME = 259200 * 1000;
    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);
    private Key key;

    /**
     * Fonction d'initialisation.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes;
        String secret = "Ryo3KVpyVFJMVyUlRjRtZjcjdFp0YjM2Wl5wKWR4U3JRRmZUIzVuZThYNkJ3SDZDeGZFUyQqdFhuSTg4OW1DVQ==";
        log.debug("Using a Base64-encoded JWT secret key");
        keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Créer un token.
     *
     * @param authentication
     * @param rememberMe
     * @return retourne le token
     */
    public BaJWTTokenDto createToken(final Authentication authentication, final boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity;
        long expiredIn = 0;
        if (rememberMe) {
            expiredIn = TOKEN_VALIDITY_IN_MILLISECONDS_FOR_REMEMBER_ME;
        } else {
            expiredIn = TOKEN_VALIDITY_IN_MILLISECONDS;
        }
        validity = new Date(now + expiredIn);

        final String token = Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
        return new BaJWTTokenDto(Instant.now(), token, expiredIn);
    }

    /**
     * Retourne l'objet d'authentification.
     *
     * @param token
     * @return L'objet
     */
    public Authentication getAuthentication(final String token) {
        Claims claims = parseToken(token);
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(r -> {
                            SimpleGrantedAuthority rv
                                    = new SimpleGrantedAuthority(r);
                            return rv;
                        })
                        .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * Parse token.
     *
     * @param token
     * @return a claims
     */
    private Claims parseToken(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token).getBody();
    }

    /**
     * Extract username from token.
     *
     * @param token
     * @return username
     */
    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token.
     *
     * @param token
     * @return expiration date
     */
    public Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract user information from token.
     *
     * @param token
     * @param claimsResolver
     * @param <T>
     * @return claims
     */
    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse token.
     *
     * @param token
     * @return claims
     */
    private Claims extractAllClaims(final String token) {
        return parseToken(token);
    }

    /**
     * Vérifie si le token a expiré ou pas.
     *
     * @param token
     * @return un boolean
     */
    private Boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Verifie la validité du token.
     *
     * @param token
     * @param username
     * @return un boolean
     */
    public Boolean validateToken(final String token, final String username) {
        String tknUsername = "";
        try {
            tknUsername = extractUsername(token);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature.");
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token.");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token.");
        } catch (IllegalArgumentException e) {
            log.info("JWT token compact of handler are invalid.");
        }
        return (tknUsername.equals(username) && !isTokenExpired(token));
    }
}
