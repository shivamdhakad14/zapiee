package com.Zapiee.staff_service.util;

import com.Zapiee.staff_service.model.Staff;
import com.Zapiee.staff_service.model.StaffRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.owner-issuer}")
    private String ownerIssuer;

    @Value("${jwt.allowed-issuers}")
    private List<String> allowedIssuers;

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

        if (allowedIssuers == null || allowedIssuers.isEmpty()) {
            throw new IllegalStateException(
                    "jwt.allowed-issuers is not configured"
            );
        }

        if (ownerIssuer == null || ownerIssuer.isBlank()
                || !allowedIssuers.contains(ownerIssuer)) {
            throw new IllegalStateException(
                    "jwt.owner-issuer must be present in jwt.allowed-issuers"
            );
        }

        if (accessTokenExpiration == null || accessTokenExpiration <= 0
                || refreshTokenExpiration == null || refreshTokenExpiration <= 0) {
            throw new IllegalStateException(
                    "JWT expiration configuration must be positive"
            );
        }
    }

    private void validateStaff(Staff staff) {
        if (staff == null || staff.getId() == null) {
            throw new IllegalArgumentException("Staff ID is required");
        }

        if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            throw new IllegalArgumentException("Staff email is required");
        }

        if (staff.getRoles() == null) {
            throw new IllegalArgumentException("Staff roles must not be null");
        }
    }

    private List<String> getRoleNames(Staff staff) {
        return staff.getRoles()
                .stream()
                .map(StaffRole::getName)
                .collect(Collectors.toList());
    }

    public String generateAccessToken(Staff staff) {
        validateStaff(staff);
        validateConfiguration();

        long now = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", staff.getId().toString());
        claims.put("userType", "STAFF");
        claims.put("roles", getRoleNames(staff));
        claims.put("tokenType", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(staff.getEmail())
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTokenExpiration))
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(Staff staff) {
        validateStaff(staff);
        validateConfiguration();

        long now = System.currentTimeMillis();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", staff.getId().toString());
        claims.put("userType", "STAFF");
        claims.put("tokenType", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(staff.getEmail())
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

        String tokenIssuer = claims.getIssuer();

        if (tokenIssuer == null || !allowedIssuers.contains(tokenIssuer)) {
            throw new JwtException("Untrusted JWT issuer");
        }

        return claims;
    }

    public Claims validateAccessToken(String token) {
        Claims claims = extractAllClaims(token);

        String tokenType = claims.get("tokenType", String.class);
        String userType = claims.get("userType", String.class);
        String userId = claims.get("userId", String.class);
        String email = claims.getSubject();

        if (!"access".equals(tokenType)) {
            throw new JwtException("Token is not an access token");
        }

        if (userId == null || userId.isBlank()) {
            throw new JwtException("JWT userId is missing");
        }

        if (email == null || email.isBlank()) {
            throw new JwtException("JWT subject is missing");
        }

        if (!"OWNER".equals(userType) && !"STAFF".equals(userType)) {
            throw new JwtException("Invalid JWT user type");
        }

        if ("OWNER".equals(userType)
                && !ownerIssuer.equals(claims.getIssuer())) {
            throw new JwtException("Owner token must come from Auth Service");
        }

        if ("STAFF".equals(userType)
                && !issuer.equals(claims.getIssuer())) {
            throw new JwtException("Staff token must come from Staff Service");
        }

        List<?> roles = claims.get("roles", List.class);

        if (roles == null || roles.stream().anyMatch(role -> !(role instanceof String))) {
            throw new JwtException("Invalid JWT roles");
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

    public boolean isAccessToken(String token) {
        try {
            validateAccessToken(token);
            return true;
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
            ) && "STAFF".equals(
                    claims.get("userType", String.class)
            ) && issuer.equals(claims.getIssuer());

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

    public boolean validateToken(String token, String email) {
        try {
            Claims claims = validateAccessToken(token);

            return email != null
                    && email.equalsIgnoreCase(claims.getSubject())
                    && claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirationInSeconds() {
        return accessTokenExpiration / 1000;
    }

    public long getRefreshTokenExpirationInSeconds() {
        return refreshTokenExpiration / 1000;
    }

    public long getRemainingTimeBeforeExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);

            return Math.max(
                    0,
                    (expiration.getTime() - System.currentTimeMillis()) / 1000
            );

        } catch (JwtException | IllegalArgumentException ex) {
            return 0;
        }
    }
}
