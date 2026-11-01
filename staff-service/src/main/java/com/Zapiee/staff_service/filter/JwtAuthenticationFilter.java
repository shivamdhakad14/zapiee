
package com.Zapiee.staff_service.filter;

import com.Zapiee.staff_service.model.Staff;
import com.Zapiee.staff_service.repository.StaffRepository;
import com.Zapiee.staff_service.util.JwtUtil;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
                || path.equals("/api/staffs/login")
                || path.equals("/api/staffs/create")
                || path.equals("/api/staffs/refresh")
                || path.equals("/api/staffs/health")
                || path.equals("/api/staffs/password-reset/request")
                || path.equals("/api/staffs/password-reset/confirm")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/actuator/");
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
            writeUnauthorized(response, "Invalid or expired access token");
            return;
        }

        // Never preserve an existing authentication when a supplied
        // Bearer token fails validation.
        SecurityContextHolder.clearContext();

        final UserDetails userDetails;

        try {
            // 1. Validate the signature, issuer, and token type.
            if (!jwtUtil.isAccessToken(token)) {
                writeUnauthorized(response, "Invalid or expired access token");
                return;
            }

            // 2. Extract claims using your actual JwtUtil methods.
            String email = jwtUtil.extractUsername(token);
            String userId = jwtUtil.extractUserId(token);

            if (email == null || email.isBlank()
                    || userId == null || userId.isBlank()) {

                writeUnauthorized(response, "Invalid access token claims");
                return;
            }

            // 3. Load the user using the email in the JWT subject.
            userDetails =
                    userDetailsService.loadUserByUsername(email);

            // 4. Check Spring Security account status.
            if (!userDetails.isEnabled()
                    || !userDetails.isAccountNonLocked()
                    || !userDetails.isAccountNonExpired()
                    || !userDetails.isCredentialsNonExpired()) {

                writeUnauthorized(response, "Staff account is unavailable");
                return;
            }

            // 5. Confirm the staff record exists.
            Optional<Staff> staffOptional =
                    staffRepository.findByEmail(email);

            if (staffOptional.isEmpty()) {
                writeUnauthorized(response, "Staff account not found");
                return;
            }

            Staff staff = staffOptional.get();

            // 6. Ensure the JWT belongs to this exact staff account.
            if (staff.getId() == null
                    || !String.valueOf(staff.getId()).equals(userId)
                    || !email.equalsIgnoreCase(staff.getEmail())) {

                writeUnauthorized(response, "Invalid access token");
                return;
            }

            // 7. Check your Staff entity's account state.
            if (!Boolean.TRUE.equals(staff.getAccountEnabled())
                    || staff.isCurrentlyLocked()) {

                writeUnauthorized(response, "Staff account is unavailable");
                return;
            }

            // 8. Validate the JWT using your actual method:
            // validateToken(String token, String email).
            if (!jwtUtil.validateToken(token, email)) {
                writeUnauthorized(response, "Invalid or expired access token");
                return;
            }

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

            log.error("Unexpected error during JWT authentication", ex);

            writeServerError(
                    response,
                    "Authentication processing failed"
            );
            return;
        }

        // 9. Create authentication only after all checks pass.
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        // IMPORTANT: keep this outside the try-catch.
        // Controller exceptions must not be mistaken for JWT errors.
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

        response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        );
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




//package com.OrderZap.staff_service.filter;
//
//import com.OrderZap.staff_service.model.Staff;
//import com.OrderZap.staff_service.repository.StaffRepository;
//import com.OrderZap.staff_service.util.JwtUtil;
//import io.jsonwebtoken.JwtException;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.NonNull;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Set;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtUtil jwtUtil;
//    private final UserDetailsService userDetailsService;
//    private final StaffRepository staffRepository;
//
//    private static final Set<String> PUBLIC_PATHS = Set.of(
//            "/api/staffs/create",
//            "/api/staffs/login",
//            "/api/staffs/refresh",
//            "/api/staffs/password-reset/request",
//            "/api/staffs/password-reset/confirm",
//            "/api/staffs/health",
//            "/v3/api-docs",
//            "/swagger-ui.html",
//            "/actuator/health",
//            "/actuator/info"
//    );
//
//
//    @Override
//    protected boolean shouldNotFilter(
//            @NonNull HttpServletRequest request) {
//
//        String path = request.getServletPath();
//        String method = request.getMethod();
//
//        if ("OPTIONS".equalsIgnoreCase(method)) {
//            return true;
//        }
//
//        if (path.equals("/error")
//                || path.equals("/api/staffs/create")
//                || path.equals("/api/staffs/login")
//                || path.equals("/api/staffs/refresh")
//                || path.equals("/api/staffs/health")
//                || path.equals("/actuator/health")
//                || path.equals("/actuator/info")
//                || path.equals("/swagger-ui.html")
//                || path.equals("/v3/api-docs")) {
//            return true;
//        }
//
//        return path.startsWith("/api/staffs/password-reset/")
//                || path.startsWith("/v3/api-docs/")
//                || path.startsWith("/swagger-ui/");
//    }
//
//
//    @Override
//    protected void doFilterInternal(
//            @NonNull HttpServletRequest request,
//            @NonNull HttpServletResponse response,
//            @NonNull FilterChain filterChain
//    ) throws ServletException, IOException {
//
//        String authHeader = request.getHeader("Authorization");
//
//        // No token: let Spring Security protect restricted endpoints.
//        if (authHeader == null
//                || !authHeader.startsWith("Bearer ")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        String token = authHeader.substring(7).trim();
//
//        if (token.isEmpty()) {
//            writeUnauthorized(response, "Bearer token is empty.");
//            return;
//        }
//
//        try {
//            // Never accept a refresh token for normal API access.
//            if (!jwtUtil.isAccessToken(token)) {
//                writeUnauthorized(response, "Invalid access token.");
//                return;
//            }
//
//            String email = jwtUtil.extractUsername(token);
//            String tokenUserId = jwtUtil.extractUserId(token);
//
//            if (email == null || email.isBlank()
//                    || tokenUserId == null || tokenUserId.isBlank()) {
//                writeUnauthorized(response, "Required JWT claims are missing.");
//                return;
//            }
//
//            Authentication existing =
//                    SecurityContextHolder.getContext().getAuthentication();
//
//            if (existing != null && existing.isAuthenticated()) {
//                filterChain.doFilter(request, response);
//                return;
//            }
//
//            UserDetails userDetails =
//                    userDetailsService.loadUserByUsername(email);
//
//            if (!userDetails.isEnabled()
//                    || !userDetails.isAccountNonLocked()
//                    || !userDetails.isAccountNonExpired()
//                    || !userDetails.isCredentialsNonExpired()) {
//                writeUnauthorized(response, "Staff account is disabled or locked.");
//                return;
//            }
//
//            Staff staff = staffRepository.findByEmail(email).orElse(null);
//
//            if (staff == null
//                    || !staff.getId().toString().equals(tokenUserId)) {
//                writeUnauthorized(
//                        response,
//                        "Token does not match the staff account."
//                );
//                return;
//            }
//
//            if (!Boolean.TRUE.equals(staff.getAccountEnabled())
//                    || !Boolean.TRUE.equals(staff.getAccountNonLocked())
//                    || staff.isCurrentlyLocked()) {
//                writeUnauthorized(response, "Staff account is disabled or locked.");
//                return;
//            }
//
//            if (!jwtUtil.validateToken(token, userDetails.getUsername())) {
//                writeUnauthorized(response, "JWT is invalid or expired.");
//                return;
//            }
//
//            UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken(
//                            userDetails,
//                            null,
//                            userDetails.getAuthorities()
//                    );
//
//            authentication.setDetails(
//                    new WebAuthenticationDetailsSource()
//                            .buildDetails(request)
//            );
//
//            SecurityContextHolder.getContext()
//                    .setAuthentication(authentication);
//
//            filterChain.doFilter(request, response);
//
//        } catch (UsernameNotFoundException ex) {
//            SecurityContextHolder.clearContext();
//            writeUnauthorized(response, "Staff account not found.");
//
//        } catch (JwtException | IllegalArgumentException ex) {
//            SecurityContextHolder.clearContext();
//            log.debug("JWT rejected: {}", ex.getMessage());
//            writeUnauthorized(response, "Invalid or expired JWT.");
//
//        } catch (Exception ex) {
//            SecurityContextHolder.clearContext();
//            log.error("Unexpected error during JWT authentication.", ex);
//
//            if (!response.isCommitted()) {
//                response.setStatus(
//                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR
//                );
//                response.setContentType("application/json");
//                response.setCharacterEncoding("UTF-8");
//                response.getWriter().write(
//                        "{\"success\":false,"
//                                + "\"message\":\"Authentication processing failed.\"}"
//                );
//            }
//        }
//    }
//
//    private void writeUnauthorized(
//            HttpServletResponse response,
//            String message
//    ) throws IOException {
//
//        SecurityContextHolder.clearContext();
//
//        if (response.isCommitted()) {
//            return;
//        }
//
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//
//        String safeMessage = message
//                .replace("\\", "\\\\")
//                .replace("\"", "\\\"");
//
//        response.getWriter().write(
//                "{\"success\":false,\"message\":\""
//                        + safeMessage
//                        + "\"}"
//        );
//    }
//}
