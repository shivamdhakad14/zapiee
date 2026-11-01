
package com.Zapiee.staff_service.util;

import com.Zapiee.staff_service.model.Staff;
import com.Zapiee.staff_service.model.StaffRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private SecretKey getSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured."
            );
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length < 64) {
            throw new IllegalStateException(
                    "JWT secret must contain at least 64 UTF-8 bytes for HS512."
            );
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private void validateConfiguration() {
        if (accessTokenExpiration == null || accessTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "jwt.access-token-expiration must be positive."
            );
        }

        if (refreshTokenExpiration == null || refreshTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "jwt.refresh-token-expiration must be positive."
            );
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException(
                    "jwt.issuer is not configured."
            );
        }
    }

    private void validateStaff(Staff staff) {
        if (staff == null || staff.getId() == null) {
            throw new IllegalArgumentException(
                    "Staff and staff ID are required."
            );
        }

        if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Staff email is required."
            );
        }

        if (staff.getRoles() == null) {
            throw new IllegalArgumentException(
                    "Staff roles must not be null."
            );
        }
    }

    public String generateAccessToken(Staff staff) {
        validateStaff(staff);
        validateConfiguration();

        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", staff.getId().toString());
        claims.put(
                "roles",
                staff.getRoles()
                        .stream()
                        .map(StaffRole::getName)
                        .collect(Collectors.toList())
        );
        claims.put("tokenType", "access");

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(staff.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(
                        new Date(now + accessTokenExpiration)
                )
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Staff staff) {
        validateStaff(staff);
        validateConfiguration();

        Map<String, Object> claims = new HashMap<>();

        claims.put("userId", staff.getId().toString());
        claims.put("tokenType", "refresh");

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(staff.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(
                        new Date(now + refreshTokenExpiration)
                )
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
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

    public String extractTokenType(String token) {
        return extractClaim(
                token,
                claims -> claims.get("tokenType", String.class)
        );
    }

    public String extractTokenId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
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
            Function<Claims, T> resolver
    ) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT token must not be empty."
            );
        }

        validateConfiguration();

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(extractTokenType(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractTokenType(token));
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);

            return expiration == null
                    || !expiration.after(new Date());

        } catch (JwtException | IllegalArgumentException ex) {
            return true;
        }
    }

    public boolean validateToken(String token, String email) {
        try {
            if (email == null || email.isBlank()) {
                return false;
            }

            Claims claims = extractAllClaims(token);

            String subject = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String tokenType = claims.get("tokenType", String.class);
            Date expiration = claims.getExpiration();

            return email.equals(subject)
                    && userId != null
                    && !userId.isBlank()
                    && "access".equals(tokenType)
                    && expiration != null
                    && expiration.after(new Date());

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirationInSeconds() {
        validateConfiguration();
        return accessTokenExpiration / 1000;
    }

    public long getRefreshTokenExpirationInSeconds() {
        validateConfiguration();
        return refreshTokenExpiration / 1000;
    }

    public long getRemainingTimeBeforeExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);

            if (expiration == null) {
                return 0;
            }

            return Math.max(
                    0,
                    (expiration.getTime() - System.currentTimeMillis()) / 1000
            );

        } catch (JwtException | IllegalArgumentException ex) {
            return 0;
        }
    }
}
