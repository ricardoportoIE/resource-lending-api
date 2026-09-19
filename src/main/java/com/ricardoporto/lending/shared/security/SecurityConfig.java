package com.ricardoporto.lending.shared.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
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
    configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(java.util.List.of("Location"));
    configuration.setAllowCredentials(!origins.isEmpty());
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(
            sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/logout")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/v1/exemplares/**",
                        "/api/v1/clientes/**",
                        "/api/v1/resources/**",
                        "/api/v1/resource-items/**")
                    .hasAnyRole("STAFF", "ADMIN")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/v1/exemplares/**",
                        "/api/v1/clientes/**",
                        "/api/v1/emprestimos/**",
                        "/api/v1/resources/**",
                        "/api/v1/resource-items/**")
                    .hasAnyRole("STAFF", "ADMIN")
                    .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/v1/exemplares/**",
                        "/api/v1/clientes/**",
                        "/api/v1/emprestimos/**")
                    .hasAnyRole("STAFF", "ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
