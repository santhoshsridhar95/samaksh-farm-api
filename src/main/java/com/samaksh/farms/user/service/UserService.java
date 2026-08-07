package com.samaksh.farms.user.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.enums.Role;
import com.samaksh.farms.user.dto.ApproveUserRequest;
import com.samaksh.farms.user.dto.ChangePermissionsRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuditService auditService;

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .filter(user -> user.getApprovalStatus() != ApprovalStatus.DELETED)
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

        String phoneNumber =
                request.getPhoneNumber() == null
                        ? null
                        : request.getPhoneNumber()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (phoneNumber != null &&
                !phoneNumber.isBlank() &&
                userRepository.existsByPhoneNumber(phoneNumber)) {

            throw new RuntimeException(
                    "Phone number already exists"
            );
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .phoneNumber(phoneNumber)
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(request.getRole())
                .emailVerified(true)
                .authProvider("SUPER_ADMIN")
                .extraPermissions(cleanPermissions(request.getExtraPermissions()))
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
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

    public UserResponse approveUser(
            Long userId,
            ApproveUserRequest request,
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

        user.setRole(
                request.getRole() == null
                        ? Role.SALES_EMPLOYEE
                        : request.getRole()
        );
        user.setActive(
                request.getActive() == null
                        ? true
                        : request.getActive()
        );
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "APPROVE_USER",
                savedUser.getEmail(),
                "User approved"
        );

        return mapToUserResponse(savedUser);
    }

    public UserResponse rejectUser(
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

        preventRemovingLastActiveSuperAdmin(user);

        user.setActive(false);
        user.setApprovalStatus(ApprovalStatus.REJECTED);

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "REJECT_USER",
                savedUser.getEmail(),
                "User rejected"
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
        user.setActive(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());

        userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "RESET_PASSWORD",
                user.getEmail(),
                "Password reset"
        );
    }

    public UserResponse softDeleteUser(
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
        user.setApprovalStatus(ApprovalStatus.DELETED);

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "DELETE_USER",
                savedUser.getEmail(),
                "User soft deleted"
        );

        return mapToUserResponse(savedUser);
    }

    public UserResponse changePermissions(
            Long userId,
            ChangePermissionsRequest request,
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

        user.setExtraPermissions(
                cleanPermissions(
                        request.getExtraPermissions()
                )
        );

        User savedUser =
                userRepository.save(user);

        auditService.createAudit(
                authentication,
                "USER",
                "CHANGE_ENTITLEMENTS",
                savedUser.getEmail(),
                "Extra entitlements changed"
        );

        return mapToUserResponse(savedUser);
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
                .emailVerified(
                        Boolean.TRUE.equals(user.getEmailVerified())
                )
                .extraPermissions(
                        cleanPermissions(user.getExtraPermissions())
                )
                .build();
    }

    private List<String> cleanPermissions(
            List<String> permissions
    ) {

        if (permissions == null) {
            return List.of();
        }

        return List.copyOf(
                new LinkedHashSet<>(
                        permissions.stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(permission -> !permission.isBlank())
                                .toList()
                )
        );
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
