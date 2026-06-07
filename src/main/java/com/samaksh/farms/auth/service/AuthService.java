package com.samaksh.farms.auth.service;

import com.samaksh.farms.auth.dto.LoginRequest;
import com.samaksh.farms.auth.dto.LoginResponse;
import com.samaksh.farms.auth.jwt.JwtService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public LoginResponse login(
            LoginRequest request
    ) {

        User user =
                userRepository
                        .findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Invalid Credentials for", request.getEmail()
                                )
                        );

        boolean valid =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!valid) {

            throw new ResourceNotFoundException(
                    "Invalid Credentials for", request.getEmail()
            );
        }

        String token =
                jwtService.generateToken(
                        user.getEmail()
                );

        return LoginResponse.builder()
                .token(token)
                .role(
                        user.getRole().name()
                )
                .name(
                        user.getName()
                )
                .build();
    }
}