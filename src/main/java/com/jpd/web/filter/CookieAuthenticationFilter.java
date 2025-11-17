package com.jpd.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jpd.web.config.JwtRoleConverted;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

@Component
@Order(1)
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;

    public CookieAuthenticationFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromCookie(request);
        
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                
                Jwt jwt = jwtDecoder.decode(token);
                JwtRoleConverted converter = new JwtRoleConverted();
                Collection<GrantedAuthority> authorities = converter.convert(jwt);

                JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
            } catch (JwtException e) {
                logger.debug("Failed to authenticate with cookie token: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error during JWT authentication", e);
            }
        }
       

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "access_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // Skip filter for auth endpoints and public resources
        return path.startsWith("/api/auth/") 
            || path.startsWith("/swagger-ui/")
            || path.startsWith("/v3/api-docs/")
            || path.startsWith("/actuator/health");
    }
}