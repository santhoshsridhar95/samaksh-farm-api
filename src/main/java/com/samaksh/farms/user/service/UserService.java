package com.samaksh.farms.user.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.enums.Role;
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
import java.util.Locale;

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

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(
                email
        ).isPresent()) {

            throw new RuntimeException(
                    "Email already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
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

        preventSelfDisable(
                user,
                authentication
        );

        preventRemovingLastActiveSuperAdmin(user);

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

        preventRemovingLastActiveSuperAdmin(
                user,
                request.getRole()
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

    private void preventSelfDisable(
            User targetUser,
            Authentication authentication
    ) {

        User actor = currentUser(authentication);

        if (actor != null
                && actor.getId() != null
                && actor.getId().equals(targetUser.getId())) {

            throw new IllegalStateException(
                    "You cannot disable your own account"
            );
        }
    }

    private void preventRemovingLastActiveSuperAdmin(
            User targetUser
    ) {

        if (targetUser.getRole() == Role.SUPER_ADMIN
                && targetUser.getActive()
                && userRepository.countByRoleAndActiveTrue(
                Role.SUPER_ADMIN
        ) <= 1) {

            throw new IllegalStateException(
                    "At least one active SUPER_ADMIN is required"
            );
        }
    }

    private void preventRemovingLastActiveSuperAdmin(
            User targetUser,
            Role nextRole
    ) {

        boolean removesSuperAdmin =
                targetUser.getRole() == Role.SUPER_ADMIN
                        && targetUser.getActive()
                        && nextRole != Role.SUPER_ADMIN;

        if (removesSuperAdmin
                && userRepository.countByRoleAndActiveTrue(
                Role.SUPER_ADMIN
        ) <= 1) {

            throw new IllegalStateException(
                    "At least one active SUPER_ADMIN is required"
            );
        }
    }

    private User currentUser(
            Authentication authentication
    ) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)) {

            return null;
        }

        return user;
    }
}
