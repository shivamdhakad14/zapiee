package com.zapiee.authservice.filter;

import com.zapiee.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        return path.equals("/error")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/verify-otp")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            writeUnauthorized(response, "Bearer token is empty");
            return;
        }

        SecurityContextHolder.clearContext();

        try {
            if (!jwtUtil.isAccessToken(token)) {
                writeUnauthorized(response, "Invalid owner access token");
                return;
            }

            String email = jwtUtil.extractUsername(token);

            if (email == null || email.isBlank()) {
                writeUnauthorized(response, "Token subject is missing");
                return;
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);

            if (!userDetails.isEnabled()
                    || !userDetails.isAccountNonLocked()
                    || !userDetails.isAccountNonExpired()
                    || !userDetails.isCredentialsNonExpired()) {

                writeUnauthorized(response, "Owner account is unavailable");
                return;
            }

            if (!jwtUtil.validateToken(token, userDetails.getUsername())) {
                writeUnauthorized(response, "Invalid or expired owner token");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (UsernameNotFoundException
                 | JwtException
                 | IllegalArgumentException ex) {

            log.debug("Owner JWT rejected: {}", ex.getClass().getSimpleName());
            writeUnauthorized(response, "Invalid or expired owner token");
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
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {"success":false,"message":"%s"}
                """.formatted(message)
        );
    }

    private void writeServerError(
            HttpServletResponse response,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {"success":false,"message":"%s"}
                """.formatted(message)
        );
    }
}