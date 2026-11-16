package com.Zapiee.staff_service.filter;

import com.Zapiee.staff_service.model.Staff;
import com.Zapiee.staff_service.repository.StaffRepository;
import com.Zapiee.staff_service.util.JwtUtil;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final StaffRepository staffRepository;

    @Override
    protected boolean shouldNotFilter(
            @NonNull HttpServletRequest request) {

        String path = request.getServletPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return path.equals("/error")
                || path.equals("/api/v1/staffs/login")
                || path.equals("/api/v1/staffs/pin-reset/request")
                || path.equals("/api/v1/staffs/pin-reset/confirm")
                || path.equals("/api/v1/staffs/health")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            writeUnauthorized(response, "Bearer token is empty");
            return;
        }

        SecurityContextHolder.clearContext();

        try {
            // Validate signature, expiry, issuer, user type and token type.
            Claims claims = jwtUtil.validateAccessToken(token);

            String email = claims.getSubject();
            String userId = claims.get("userId", String.class);
            String userType = claims.get("userType", String.class);

            UsernamePasswordAuthenticationToken authentication;

            if ("OWNER".equals(userType)) {

                // Owner is not stored in Staff Service.
                // Never query StaffRepository for an owner.
                List<SimpleGrantedAuthority> authorities =
                        jwtUtil.extractRoles(token)
                                .stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList();

                if (authorities.isEmpty()) {
                    writeUnauthorized(response, "Owner has no assigned roles");
                    return;
                }

                authentication =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                authorities
                        );

                log.debug("Owner authenticated through Auth Service: {}", email);

            } else if ("STAFF".equals(userType)) {

                // Staff token must belong to a local Staff account.
                Optional<Staff> staffOptional =
                        staffRepository.findByEmail(email);

                if (staffOptional.isEmpty()) {
                    writeUnauthorized(response, "Staff account not found");
                    return;
                }

                Staff staff = staffOptional.get();

                if (staff.getId() == null
                        || !String.valueOf(staff.getId()).equals(userId)
                        || !email.equalsIgnoreCase(staff.getEmail())) {

                    writeUnauthorized(response, "Staff identity does not match token");
                    return;
                }

                if (!Boolean.TRUE.equals(staff.getAccountEnabled())
                        || staff.isCurrentlyLocked()) {

                    writeUnauthorized(response, "Staff account is unavailable");
                    return;
                }

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled()
                        || !userDetails.isAccountNonLocked()
                        || !userDetails.isAccountNonExpired()
                        || !userDetails.isCredentialsNonExpired()) {

                    writeUnauthorized(response, "Staff account is unavailable");
                    return;
                }

                if (!jwtUtil.validateToken(token, userDetails.getUsername())) {
                    writeUnauthorized(response, "Invalid or expired access token");
                    return;
                }

                authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                log.debug("Staff authenticated successfully: {}", email);

            } else {
                writeUnauthorized(response, "Unsupported user type");
                return;
            }

            authentication.setDetails(
                    new org.springframework.security.web.authentication
                            .WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (UsernameNotFoundException
                 | JwtException
                 | IllegalArgumentException ex) {

            log.debug(
                    "JWT authentication rejected: {}",
                    ex.getClass().getSimpleName()
            );

            writeUnauthorized(response, "Invalid or expired access token");
            return;

        } catch (Exception ex) {

            log.error("Unexpected JWT authentication error", ex);

            writeServerError(response, "Authentication processing failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            String message) throws IOException {

        SecurityContextHolder.clearContext();

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {
                  "success": false,
                  "message": "%s"
                }
                """.formatted(message)
        );
    }

    private void writeServerError(
            HttpServletResponse response,
            String message) throws IOException {

        SecurityContextHolder.clearContext();

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {
                  "success": false,
                  "message": "%s"
                }
                """.formatted(message)
        );
    }
}