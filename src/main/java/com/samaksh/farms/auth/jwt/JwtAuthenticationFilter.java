package com.samaksh.farms.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authHeader.substring(7);

        if (!jwtService.isTokenValid(token)) {

            SecurityContextHolder.clearContext();

            writeUnauthorized(
                    response,
                    "Invalid or expired token"
            );

            return;
        }

        String email =
                jwtService.extractEmail(token);

        User user =
                userRepository
                        .findByEmail(email)
                        .orElse(null);

        if (user == null
                || Boolean.FALSE.equals(user.getActive())) {

            SecurityContextHolder.clearContext();

            writeUnauthorized(
                    response,
                    "Invalid or inactive user"
            );

            return;
        }

        if (SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" +
                                                    user.getRole().name()
                                    )
                            )
                    );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(auth);
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
