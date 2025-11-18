package com.jpd.web.config;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.jpd.web.filter.CookieAuthenticationFilter;


import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class JaenConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;



    @Autowired
    @Lazy
    private CookieAuthenticationFilter cookieAuthenticationFilter;

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        CsrfTokenRequestAttributeHandler csrfTokenHandler = new CsrfTokenRequestAttributeHandler();
        http.csrf(csrfConfig -> csrfConfig
        	    .csrfTokenRequestHandler(csrfTokenHandler)
        	    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        	    // ✅ Đổi "/api/*" thành "/api/**" (2 dấu *)
        	    .ignoringRequestMatchers(
        	        "/api/**",           // ✅ Match tất cả sub-paths
        	        "/webhook/**",
        	        "/quiz/**"
        	    )
        	);
        http.cors(corsCongif -> corsCongif.configurationSource(new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration corsF = new CorsConfiguration();
                corsF.setAllowedOriginPatterns(Arrays.asList(
                        "http://localhost:3000",
                        "http://13.159.212.77"
                ));// ✅ Đổi sang Pattern
                corsF.setAllowCredentials(true);
                corsF.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));

                corsF.setAllowedHeaders(Collections.singletonList("*"));
                corsF.setExposedHeaders(Arrays.asList("Set-Cookie"));
                corsF.setMaxAge(3600L);
                return corsF;
            }
        }));

        http.formLogin(AbstractHttpConfigurer::disable);

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverted());

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**","/api/tts").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/api/quiz/join", "/api/course/*", "/api/dictionary/*").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/quiz/**").permitAll()
                .requestMatchers("/webhook/**", "/api/paypal/**", "/api/vnpay/**","/api/customer/account_infor").permitAll()

                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers("/homepage/**", "/api/**").authenticated()
                .anyRequest().authenticated());

        http.oauth2ResourceServer(rsc -> rsc.jwt(
                JwtConfigurer -> JwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        http.addFilterBefore(cookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public InMemoryHttpExchangeRepository httpExchangeRepository() {
        return new InMemoryHttpExchangeRepository();
    }

    @Bean
    @Order(0)
    SecurityFilterChain websocketSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/ws-quiz/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
