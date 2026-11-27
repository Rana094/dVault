package com.dvault.security;

import com.dvault.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/api/health", "/api/auth/csrf").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(mapper, request, response, 401, "UNAUTHENTICATED", "Authentication required"))
                .accessDeniedHandler((request, response, exception) ->
                    writeError(mapper, request, response, 403, "FORBIDDEN", "Request not permitted")))
            .build();
    }

    private void writeError(ObjectMapper mapper, HttpServletRequest request,
                            HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), ApiError.of(status, code, message, request.getRequestURI()));
    }
}
