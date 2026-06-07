package com.samaksh.farms.user.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.user.dto.ChangeRoleRequest;
import com.samaksh.farms.user.dto.ResetPasswordRequest;
import com.samaksh.farms.user.dto.UserRequest;
import com.samaksh.farms.user.dto.UserResponse;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuditService auditService;

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    public UserResponse createUser(
            UserRequest request,
            Authentication authentication
    ) {

        if (userRepository.findByEmail(
                request.getEmail()
        ).isPresent()) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(request.getRole())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "CREATE_USER",
                savedUser.getEmail(),
                "User created"
        );

        return mapToUserResponse(savedUser);
    }

    public UserResponse disableUser(
            Long userId,
            Authentication authentication
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User",
                                                userId
                                        )
                        );

        user.setActive(false);

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "DISABLE_USER",
                savedUser.getEmail(),
                "User disabled"
        );

        return mapToUserResponse(savedUser);
    }

    public UserResponse enableUser(
            Long userId,
            Authentication authentication
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User",
                                                userId
                                        )
                        );

        user.setActive(true);

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "ENABLE_USER",
                savedUser.getEmail(),
                "User enabled"
        );

        return mapToUserResponse(savedUser);
    }

    public UserResponse changeRole(
            Long userId,
            ChangeRoleRequest request,
            Authentication authentication
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User",
                                                userId
                                        )
                        );

        user.setRole(request.getRole());

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "CHANGE_ROLE",
                savedUser.getEmail(),
                "Role changed to "
                        + request.getRole()
        );

        return mapToUserResponse(savedUser);
    }

    public void resetPassword(
            Long userId,
            ResetPasswordRequest request,
            Authentication authentication
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User",
                                                userId
                                        )
                        );

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "RESET_PASSWORD",
                user.getEmail(),
                "Password reset"
        );
    }

    private UserResponse mapToUserResponse(
            User user
    ) {

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .active(user.getActive())
                .build();
    }
}