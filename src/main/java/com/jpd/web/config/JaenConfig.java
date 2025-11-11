package com.jpd.web.config;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.jpd.web.filter.CreatorFilter;

import jakarta.servlet.http.HttpServletRequest;
@Configuration
public class JaenConfig {
  
	@Bean
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http.sessionManagement(sessionConfig -> sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		CsrfTokenRequestAttributeHandler csrfTokenHandler = new CsrfTokenRequestAttributeHandler();
		http.csrf(csrfConfig -> csrfConfig.csrfTokenRequestHandler(csrfTokenHandler)
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.ignoringRequestMatchers("/api/*", "/webhook/**","/api/admin/**","/api/redis-test/*","/api/quiz/*"));
		http.cors(corsCongif -> corsCongif.configurationSource(new CorsConfigurationSource() {

			@Override
			public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
				CorsConfiguration corsF = new CorsConfiguration();
				corsF.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
				corsF.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS"));
				corsF.setAllowCredentials(true);
				corsF.setAllowedHeaders(Collections.singletonList("*"));
				corsF.setMaxAge(3600L);
				return corsF;
			}
		}));

		http.formLogin(AbstractHttpConfigurer::disable);
		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverted());
		http.authorizeHttpRequests(auth -> auth

				// .requestMatchers("/actuator","/actuator/health","/actuator/health/**"
				// ,"/actuator/error","/actuator/health","/actuator/info","/actuator/beans").permitAll()
				.requestMatchers("/actuator/**").hasRole("ADMIN")
				.requestMatchers("/api/quiz/join","/api/course/*","/api/dictionary/*").permitAll()
				.requestMatchers("/api/admin/**").hasRole("ADMIN") // Admin endpoints
				.requestMatchers("/quiz/**").permitAll()
				.requestMatchers("/homepage/**", "/api/**").authenticated() // Public course listing
				.requestMatchers("/webhook/**").permitAll().requestMatchers("/swagger-ui/**",
						"/swagger-ui.html",
						"/v3/api-docs/**",
						"/v3/api-docs.yaml",
						"/actuator/**")
				.permitAll()
				.anyRequest().authenticated());
		http.oauth2ResourceServer(
				rsc -> rsc.jwt(JwtConfigurer -> JwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		// Add custom filters

		  
		return http.build();
	}

	@Bean
	public InMemoryHttpExchangeRepository httpExchangeRepository() {
		return new InMemoryHttpExchangeRepository();
	}
	 @Bean
	 @Order(0) // Execute đầu tiên
	 SecurityFilterChain websocketSecurityFilterChain(HttpSecurity http) throws Exception {
	     return http
	         .securityMatcher("/ws-quiz/**")
	         .csrf(AbstractHttpConfigurer::disable)
	         .cors(AbstractHttpConfigurer::disable)
	         .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
	         .build();
	 }
}
