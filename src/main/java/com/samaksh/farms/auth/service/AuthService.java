package com.samaksh.farms.auth.service;

import com.samaksh.farms.auth.dto.LoginRequest;
import com.samaksh.farms.auth.dto.LoginResponse;
import com.samaksh.farms.auth.dto.ForgotPasswordRequest;
import com.samaksh.farms.auth.dto.SignupRequest;
import com.samaksh.farms.auth.jwt.JwtService;
import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.enums.Role;
import com.samaksh.farms.user.dto.UserResponse;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        String login =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        User user =
                userRepository.findByEmail(login)
                        .or(() -> userRepository.findByPhoneNumber(login))
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
                    "User account is not approved or is disabled"
            );
        }

        if (user.getApprovalStatus() != null &&
                user.getApprovalStatus() != ApprovalStatus.APPROVED) {

            throw new DisabledException(
                    "User account is awaiting super admin approval"
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

    public UserResponse signup(
            SignupRequest request
    ) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        String phoneNumber =
                request.getPhoneNumber()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {

            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {

            throw new RuntimeException("Phone number already exists");
        }

        User user =
                User.builder()
                        .name(request.getName())
                        .email(email)
                        .phoneNumber(phoneNumber)
                        .password(
                                passwordEncoder.encode(
                                        request.getPassword()
                                )
                        )
                        .role(Role.SALES_EMPLOYEE)
                        .active(false)
                        .approvalStatus(ApprovalStatus.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

        return mapToUserResponse(
                userRepository.save(user)
        );
    }

    public void requestPasswordReset(
            ForgotPasswordRequest request
    ) {

        String login =
                request.getLogin()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        userRepository.findByEmail(login)
                .or(() -> userRepository.findByPhoneNumber(login))
                .ifPresent(user -> {
                    user.setApprovalStatus(
                            ApprovalStatus.RESET_REQUESTED
                    );
                    user.setActive(false);
                    userRepository.save(user);
                });
    }

    private UserResponse mapToUserResponse(
            User user
    ) {

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .active(user.getActive())
                .approvalStatus(
                        user.getApprovalStatus() == null
                                ? ApprovalStatus.APPROVED
                                : user.getApprovalStatus()
                )
                .build();
    }
}
