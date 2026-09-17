package com.familytree.familytree.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                // ====================================================
                // DISABLE CSRF
                // ====================================================

                .csrf(csrf -> csrf.disable())


                // ====================================================
                // ENABLE CORS
                // ====================================================

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )


                // ====================================================
                // API PERMISSIONS
                // ====================================================

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login"
                        )
                        .permitAll()

                        .anyRequest()
                        .permitAll()
                );


        return http.build();
    }


    // ================================================================
    // CORS CONFIGURATION
    // ================================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        // ============================================================
        // ALLOW ANY LOCALHOST PORT
        // ============================================================

        configuration.setAllowedOriginPatterns(
                List.of(
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
        );


        // ============================================================
        // ALLOWED HTTP METHODS
        // ============================================================

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );


        // ============================================================
        // ALLOWED HEADERS
        // ============================================================

        configuration.setAllowedHeaders(
                List.of("*")
        );


        // ============================================================
        // CREDENTIALS
        // ============================================================

        configuration.setAllowCredentials(true);


        // ============================================================
        // APPLY TO ALL API ENDPOINTS
        // ============================================================

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration);


        return source;
    }
}