package com.samaksh.farms.auth.service;

import com.samaksh.farms.auth.dto.LoginRequest;
import com.samaksh.farms.auth.dto.LoginResponse;
import com.samaksh.farms.auth.jwt.JwtService;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    public LoginResponse login(
            LoginRequest request
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        User user =
                userRepository
                        .findByEmail(
                                email
                        )
                        .orElseThrow(
                                () -> new BadCredentialsException(
                                        "Invalid email or password"
                                )
                        );

        boolean valid =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!valid) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        if (Boolean.FALSE.equals(user.getActive())) {

            throw new DisabledException(
                    "User account is disabled"
            );
        }

        String token =
                jwtService.generateToken(
                        user
                );

        return LoginResponse.builder()
                .userId(user.getId())
                .token(token)
                .role(
                        user.getRole().name()
                )
                .name(
                        user.getName()
                )
                .active(user.getActive())
                .build();
    }
}
