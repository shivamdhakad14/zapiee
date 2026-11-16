package com.zapiee.authservice.util;

import com.zapiee.authservice.entity.Role;
import com.zapiee.authservice.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 64) {
            throw new IllegalStateException(
                    "HS512 requires a secret of at least 64 UTF-8 bytes"
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void validateConfiguration() {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("jwt.issuer is not configured");
        }

        if (accessTokenExpiration == null || accessTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "jwt.access-token-expiration must be positive"
            );
        }

        if (refreshTokenExpiration == null || refreshTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "jwt.refresh-token-expiration must be positive"
            );
        }
    }

    private List<String> getRoles(User user) {
        if (user.getRoles() == null) {
            return List.of();
        }

        return user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    public String generateAccessToken(User user) {
        Objects.requireNonNull(user, "User must not be null");

        if (user.getId() == null || user.getEmail() == null) {
            throw new IllegalArgumentException(
                    "User ID and email are required"
            );
        }

        validateConfiguration();

        long now = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("userType", "OWNER");
        claims.put("roles", getRoles(user));
        claims.put("tokenType", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTokenExpiration))
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Objects.requireNonNull(user, "User must not be null");

        if (user.getId() == null || user.getEmail() == null) {
            throw new IllegalArgumentException(
                    "User ID and email are required"
            );
        }

        validateConfiguration();

        long now = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("userType", "OWNER");
        claims.put("tokenType", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshTokenExpiration))
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token is empty");
        }

        validateConfiguration();

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(30)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // Auth Service locally trusts only its own tokens.
        if (!issuer.equals(claims.getIssuer())) {
            throw new JwtException("Untrusted JWT issuer");
        }

        return claims;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUserId(String token) {
        return extractClaim(
                token,
                claims -> claims.get("userId", String.class)
        );
    }

    public String extractUserType(String token) {
        return extractClaim(
                token,
                claims -> claims.get("userType", String.class)
        );
    }

    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    public String extractTokenType(String token) {
        return extractClaim(
                token,
                claims -> claims.get("tokenType", String.class)
        );
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);

        List<?> roles = claims.get("roles", List.class);

        if (roles == null) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    public <T> T extractClaim(
            String token,
            java.util.function.Function<Claims, T> resolver
    ) {
        return resolver.apply(extractAllClaims(token));
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            return "access".equals(
                    claims.get("tokenType", String.class)
            ) && "OWNER".equals(
                    claims.get("userType", String.class)
            );

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Access token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            return "refresh".equals(
                    claims.get("tokenType", String.class)
            ) && "OWNER".equals(
                    claims.get("userType", String.class)
            );

        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration == null || !expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    public boolean validateToken(String token, String username) {
        try {
            Claims claims = extractAllClaims(token);

            return username != null
                    && username.equalsIgnoreCase(claims.getSubject())
                    && "OWNER".equals(
                    claims.get("userType", String.class)
            )
                    && "access".equals(
                    claims.get("tokenType", String.class)
            )
                    && claims.get("userId", String.class) != null
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public Long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    public Long getRemainingTimeBeforeExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);

            return Math.max(
                    0L,
                    (expiration.getTime() - Instant.now().toEpochMilli()) / 1000
            );

        } catch (Exception ex) {
            return 0L;
        }
    }
}