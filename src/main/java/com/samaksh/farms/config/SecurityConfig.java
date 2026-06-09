package com.samaksh.farms.config;

import com.samaksh.farms.auth.jwt.JwtAuthenticationFilter;
import com.samaksh.farms.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    private static final String FARM_MANAGER = "FARM_MANAGER";

    private static final String LABOUR = "LABOUR";

    private static final String SALES_USER = "SALES_USER";

    private static final String[] FARM_OPERATIONS = {
            SUPER_ADMIN,
            FARM_MANAGER,
            LABOUR
    };

    private static final String[] FARM_MANAGEMENT = {
            SUPER_ADMIN,
            FARM_MANAGER
    };

    private static final String[] SALES_OPERATIONS = {
            SUPER_ADMIN,
            FARM_MANAGER,
            SALES_USER
    };

    private static final String[] DASHBOARD_ROLES = {
            SUPER_ADMIN,
            FARM_MANAGER,
            SALES_USER
    };

    private final JwtAuthenticationFilter jwtFilter;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))

                .authorizeHttpRequests(auth ->
                        auth

                                // Public bootstrap APIs
                                .requestMatchers(
                                        "/api/auth/login"

                                ).permitAll()

                                // SWAGGER
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs",
                                        "/swagger-resources/**",
                                        "/webjars/**"
                                ).permitAll()

                                // User and audit administration
                                .requestMatchers("/api/users/**")
                                .hasRole(SUPER_ADMIN)

                                .requestMatchers("/api/audit/**")
                                .hasAnyRole(FARM_MANAGEMENT)

                                // Dashboards and analytics
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/dashboard/**",
                                        "/api/farm-dashboard/**"
                                ).hasAnyRole(DASHBOARD_ROLES)

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/analytics/**",
                                        "/api/batch-analytics/**",
                                        "/api/inventory-alerts/**"
                                ).hasAnyRole(FARM_MANAGEMENT)

                                // Farm inventory and production workflows
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/inventory/**",
                                        "/api/production/**",
                                        "/api/production-movements/**",
                                        "/api/harvest/**"
                                ).hasAnyRole(FARM_OPERATIONS)

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/inventory/**"
                                ).hasAnyRole(FARM_MANAGEMENT)

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/production/**",
                                        "/api/harvest/**"
                                ).hasAnyRole(FARM_OPERATIONS)

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/production/**"
                                ).hasAnyRole(FARM_OPERATIONS)

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/inventory-transactions/**"
                                ).hasAnyRole(FARM_MANAGEMENT)

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/inventory-transactions/**"
                                ).hasAnyRole(FARM_MANAGEMENT)

                                // Product catalog and sales workflows
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/products/**"
                                ).hasAnyRole(SALES_OPERATIONS)

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/products/**"
                                ).hasAnyRole(FARM_MANAGEMENT)

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/products/**"
                                ).hasAnyRole(FARM_MANAGEMENT)

                                .requestMatchers(
                                        "/api/customers/**",
                                        "/api/orders/**",
                                        "/api/sales/**"
                                ).hasAnyRole(SALES_OPERATIONS)

                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "Authentication required"
                                )
                        )
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "You do not have permission to perform this action"
                                )
                        )
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws java.io.IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Object> body = ApiResponse.builder()
                .success(false)
                .message(message)
                .data(null)
                .build();

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}
