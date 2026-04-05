package com.share.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/api/auth/**", "/error", "/ws/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/content/list",
                                "/content/detail/**",
                                "/content/categories",
                                "/content/tags",
                                "/content/search",
                                "/comment/list",
                                "/comment/replies",
                                "/comment/like/count",
                                "/collection/count",
                                "/follow/follower-count",
                                "/follow/following-count",
                                "/announcements/active",
                                "/ai-brief/today",
                                "/ai-brief/history",
                                "/users/*/contents",
                                "/users/*/public",
                                "/api/content/list",
                                "/api/content/detail/**",
                                "/api/content/categories",
                                "/api/content/tags",
                                "/api/content/search",
                                "/api/comment/list",
                                "/api/comment/replies",
                                "/api/comment/like/count",
                                "/api/collection/count",
                                "/api/follow/follower-count",
                                "/api/follow/following-count",
                                "/api/announcements/active",
                                "/api/ai-brief/today",
                                "/api/ai-brief/history",
                                "/api/users/*/contents",
                                "/api/users/*/public").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/content/view/**",
                                "/api/content/view/**",
                                "/analytics/uv/ping",
                                "/api/analytics/uv/ping").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) ->
                                handleAuthenticationEntryPoint(request, response))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void handleAuthenticationEntryPoint(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
        if (Boolean.TRUE.equals(request.getAttribute(JwtAuthenticationFilter.ATTR_ACCOUNT_BLOCKED))) {
            String reason = normalizeReason((String) request.getAttribute(JwtAuthenticationFilter.ATTR_ACCOUNT_BLOCK_REASON));
            response.setHeader("X-Account-Status", "blocked");
            if (reason != null) {
                response.setHeader("X-Block-Reason", URLEncoder.encode(reason, StandardCharsets.UTF_8));
            }
            String message = reason == null ? "ACCOUNT_BLOCKED" : "ACCOUNT_BLOCKED|" + reason;
            writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, message);
            return;
        }

        writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED");
    }

    private String normalizeReason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String reason = value.trim();
        return reason.length() > 500 ? reason.substring(0, 500) : reason;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse error = ApiResponse.error(status, message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
