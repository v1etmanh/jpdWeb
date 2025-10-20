package com.jpd.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class JaenConfig {
    @Bean
    @Profile("!dev")
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        CsrfTokenRequestAttributeHandler csrfTokenHandler = new CsrfTokenRequestAttributeHandler();
        http.csrf(csrfConfig -> csrfConfig.csrfTokenRequestHandler(csrfTokenHandler)
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/*", "/webhook/**"));
        http.cors(corsCongif -> corsCongif.configurationSource(new CorsConfigurationSource() {

                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration corsF = new CorsConfiguration();
                        corsF.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                        corsF.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE"));
                        corsF.setAllowCredentials(true);
                        corsF.setAllowedHeaders(Collections.singletonList("*"));
                        corsF.setMaxAge(3600L);
                        return corsF;
                    }
                })
        );

        http.formLogin(AbstractHttpConfigurer::disable);
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverted());
        http.authorizeHttpRequests(auth -> auth

                //.requestMatchers("/actuator","/actuator/health","/actuator/health/**" ,"/actuator/error","/actuator/health","/actuator/info","/actuator/beans").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/homepage/**", "/api/**").permitAll() // Public course listing
                .requestMatchers("/course/**", "/account/**", "/upDirect/**").hasRole("USER")
                .requestMatchers("/webhook/**").permitAll().
                requestMatchers("/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/actuator/**").permitAll()
                .anyRequest().authenticated()
        );
        http.oauth2ResourceServer(rsc -> rsc.jwt(JwtConfigurer -> JwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    public InMemoryHttpExchangeRepository httpExchangeRepository() {
        return new InMemoryHttpExchangeRepository();
    }


    /**
     * Cấu hình này CHỈ chạy khi profile là "dev"
     * -> Vô hiệu hóa bảo mật
     */@Bean
    public JavaMailSender javaMailSender(){
         return new JavaMailSenderImpl();
    }


}




