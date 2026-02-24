package com.vedvix.syncledger.config;

import com.vedvix.syncledger.security.JwtAuthenticationFilter;
import com.vedvix.syncledger.security.JwtTokenProvider;
import com.vedvix.syncledger.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for JWT-based authentication and role-based authorization.
 * Supports multi-tenant access control with Super Admin capabilities.
 * 
 * @author vedvix
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/v1/auth/**").permitAll()
                .requestMatchers("/v1/signup", "/v1/signup/**").permitAll()
                .requestMatchers("/v1/subscriptions/plans").permitAll()
                .requestMatchers("/v1/plan-definitions/active").permitAll()
                .requestMatchers("/v1/coupons/validate").permitAll()
                .requestMatchers("/v1/webhooks/**").permitAll()
                .requestMatchers("/v1/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Allow file access for local storage (PDF service needs this)
                // Note: context-path /api is already stripped by Spring Security
                .requestMatchers("/files/**").permitAll()
                // Allow Swagger/OpenAPI (explicit html and index paths)
                .requestMatchers("/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                // Also permit context-path prefixed variants (/api/...)
                .requestMatchers("/api/swagger-ui.html", "/api/swagger-ui/index.html", "/api/swagger-ui/**", "/api/v3/api-docs", "/api/v3/api-docs/**").permitAll()

                // Super Admin only endpoints
                .requestMatchers("/v1/super-admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/v1/organizations/**").hasRole("SUPER_ADMIN")
                
                // Admin endpoints (organization-scoped)
                .requestMatchers("/v1/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // Approval endpoints
                .requestMatchers("/v1/approvals/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "APPROVER")
                .requestMatchers(HttpMethod.POST, "/v1/invoices/*/approve").hasAnyRole("SUPER_ADMIN", "ADMIN", "APPROVER")
                .requestMatchers(HttpMethod.POST, "/v1/invoices/*/reject").hasAnyRole("SUPER_ADMIN", "ADMIN", "APPROVER")
                
                // Invoice management
                .requestMatchers(HttpMethod.GET, "/v1/invoices/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "APPROVER", "VIEWER")
                .requestMatchers(HttpMethod.POST, "/v1/invoices/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/v1/invoices/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/v1/invoices/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                
                // Dashboard - accessible by all authenticated users
                .requestMatchers("/v1/dashboard/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins from environment variable or use defaults
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
        ));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Organization-Id",  // Custom header for org context
            "X-CSRF-Token"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers",
            "Access-Control-Max-Age",
            "Authorization"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // Register for all paths
        return source;
    }
}
