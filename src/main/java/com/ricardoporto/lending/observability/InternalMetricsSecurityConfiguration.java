package com.ricardoporto.lending.observability;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "api.observability.internal-metrics", havingValue = "true")
public class InternalMetricsSecurityConfiguration {
  @Bean
  @Order(1)
  SecurityFilterChain internalMetricsFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher(EndpointRequest.to("health", "prometheus"))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .build();
  }
}
