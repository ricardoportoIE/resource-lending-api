package com.ricardoporto.lending.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricardoporto.lending.idempotency.IdempotencyFilter;
import com.ricardoporto.lending.idempotency.IdempotencyService;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  private final SecurityFilter securityFilter;
  private final ApiAuthenticationEntryPoint authenticationEntryPoint;
  private final ApiAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      SecurityFilter securityFilter,
      ApiAuthenticationEntryPoint authenticationEntryPoint,
      ApiAccessDeniedHandler accessDeniedHandler) {
    this.securityFilter = securityFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public IdempotencyFilter idempotencyFilter(
      IdempotencyService idempotencyService, ObjectMapper objectMapper) {
    return new IdempotencyFilter(idempotencyService, objectMapper);
  }

  @Bean
  public FilterRegistrationBean<IdempotencyFilter> disableIdempotencyFilterAutoRegistration(
      IdempotencyFilter filter) {
    var registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${api.security.cors.allowed-origins:}") String configuredOrigins) {
    var origins =
        Arrays.stream(configuredOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList();
    var configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(
        java.util.List.of("Authorization", "Content-Type", "Idempotency-Key"));
    configuration.setExposedHeaders(
        java.util.List.of("Location", "Idempotency-Replayed", "X-Correlation-ID"));
    configuration.setAllowCredentials(!origins.isEmpty());
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, IdempotencyFilter idempotencyFilter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/resources",
                        "/api/v1/resources/*/items",
                        "/api/v1/resource-items/**",
                        "/api/v1/loans/*/approve",
                        "/api/v1/loans/*/reject",
                        "/api/v1/loans/*/collect",
                        "/api/v1/loans/*/return")
                    .hasAnyRole("STAFF", "ADMIN")
                    .requestMatchers(
                        HttpMethod.PATCH, "/api/v1/resources/**", "/api/v1/resource-items/**")
                    .hasAnyRole("STAFF", "ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(idempotencyFilter, SecurityFilter.class);
    return http.build();
  }
}
