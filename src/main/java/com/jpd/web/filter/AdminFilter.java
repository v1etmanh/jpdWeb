package com.jpd.web.filter;

import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@Component
@Order(2)
public class AdminFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Check if user is authenticated
            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Un authenticated user attempted to access admin endpoint");
                sendError(response, 401, "Unauthorized. Please login.");
                return;
            }

            // Check if principal is JWT token
            if (authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String email = jwt.getClaimAsString("email");

                // Check for ADMIN role
                Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                boolean hasAdminRole = authorities.stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                if (!hasAdminRole) {
                    log.warn("User {} attempted to access admin endpoint without ADMIN role", email);
                    sendError(response, 403, "Access denied. Admin role required");
                    return;
                }

                log.debug("Admin access granted for user: {}", email);
            } else {
                // Authentication exists but not JWT token
                log.warn("Invalid authentication type for admin endpoint");
                sendError(response, 401, "Invalid authentication");
                return;
            }

        } catch (Exception e) {
            log.error("Error in AdminFilter: {}", e.getMessage(), e);
            sendError(response, 500, "Internal server error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                String.format("{\"error\": \"%s\", \"status\": %d}", message, status));
    }

    // Only apply to /api/admin/** paths
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/admin/");
    }
}
