package com.samaksh.farms.user.service;

import com.samaksh.farms.audit.service.AuditService;
import com.samaksh.farms.common.exception.ResourceNotFoundException;
import com.samaksh.farms.config.DatabaseConstraintRepair;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final DatabaseConstraintRepair databaseConstraintRepair;

    private final JdbcTemplate jdbcTemplate;

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
                .role(primaryRole(request.getRoles(), request.getRole()))
                .extraRoles(extraRoles(request.getRoles(), request.getRole()))
                .emailVerified(true)
                .authProvider("SUPER_ADMIN")
                .extraPermissions(cleanPermissions(request.getExtraPermissions()))
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser =
                saveUserWithConstraintRepair(user);

        auditService.createAudit(
                authentication,
                "USER",
                "CREATE_USER",
                savedUser.getEmail(),
                "User created"
        );

        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse approveUser(
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

        int updated =
                userRepository.approveById(
                        userId,
                        ApprovalStatus.APPROVED,
                        LocalDateTime.now()
                );

        if (updated == 0) {
            throw new ResourceNotFoundException(
                    "User",
                    userId
            );
        }

        User savedUser =
                userRepository.findById(userId)
                        .orElse(user);

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
                saveUserWithConstraintRepair(user);

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
                saveUserWithConstraintRepair(user);

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
                saveUserWithConstraintRepair(user);

        auditService.createAudit(
                authentication,
                "USER",
                "ENABLE_USER",
                savedUser.getEmail(),
                "User enabled"
        );

        return mapToUserResponse(savedUser);
    }

    @Transactional
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

        List<Role> roles =
                cleanRoles(request.getRoles(), request.getRole());

        preventRemovingLastActiveSuperAdmin(
                user,
                roles
        );

        updateUserRolesDirectly(
                userId,
                roles
        );

        User savedUser =
                userRepository.findById(userId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User",
                                                userId
                                        )
                        );

        auditService.createAudit(
                authentication,
                "USER",
                "CHANGE_ROLE",
                savedUser.getEmail(),
                "Role changed to "
                        + roles
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
        user.setEmailVerified(true);
        user.setEmailVerificationOtp(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());

        saveUserWithConstraintRepair(user);

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
                saveUserWithConstraintRepair(user);

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
                .roles(allRoles(user))
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

    private User saveUserWithConstraintRepair(
            User user
    ) {

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            databaseConstraintRepair.repairUserConstraints();
            return userRepository.saveAndFlush(user);
        }
    }

    private void updateUserRolesDirectly(
            Long userId,
            List<Role> roles
    ) {

        try {
            databaseConstraintRepair.repairUserConstraints();
            doUpdateUserRolesDirectly(
                    userId,
                    roles
            );
        } catch (DataIntegrityViolationException ex) {
            databaseConstraintRepair.repairUserConstraints();
            doUpdateUserRolesDirectly(
                    userId,
                    roles
            );
        }
    }

    private void doUpdateUserRolesDirectly(
            Long userId,
            List<Role> roles
    ) {

        jdbcTemplate.update(
                "UPDATE users SET role = ? WHERE id = ?",
                roles.getFirst().name(),
                userId
        );

        jdbcTemplate.update(
                "DELETE FROM user_extra_roles WHERE user_id = ?",
                userId
        );

        for (Role role : roles.subList(1, roles.size())) {
            jdbcTemplate.update(
                    "INSERT INTO user_extra_roles (user_id, role) VALUES (?, ?)",
                    userId,
                    role.name()
            );
        }
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

    private Role primaryRole(
            List<?> roles,
            Object fallback
    ) {

        return cleanRoles(roles, fallback).getFirst();
    }

    private List<Role> extraRoles(
            List<?> roles,
            Object fallback
    ) {

        List<Role> cleanRoles =
                cleanRoles(roles, fallback);

        return cleanRoles.size() > 1
                ? List.copyOf(cleanRoles.subList(1, cleanRoles.size()))
                : List.of();
    }

    private List<Role> approvalRoles(
            User user,
            ApproveUserRequest request
    ) {

        if (request != null) {
            try {
                return cleanRoles(
                        request.getRoles(),
                        request.getRole() == null
                                ? user.getRole()
                                : request.getRole()
                );
            } catch (RuntimeException ignored) {
                // Approval should not be blocked by a stale or malformed role payload.
            }
        }

        List<Role> existingRoles =
                allRoles(user);

        if (!existingRoles.isEmpty()) {
            return existingRoles;
        }

        return List.of(Role.SALES_EMPLOYEE);
    }

    private List<Role> cleanRoles(
            List<?> roles,
            Object fallback
    ) {

        LinkedHashSet<Role> result =
                new LinkedHashSet<>();

        if (roles != null) {
            result.addAll(
                    roles.stream()
                            .filter(Objects::nonNull)
                            .map(this::normalizeRole)
                            .toList()
            );
        }

        if (result.isEmpty()) {
            result.add(
                    fallback == null
                            ? Role.SALES_EMPLOYEE
                            : normalizeRole(fallback)
            );
        }

        return List.copyOf(result);
    }

    private Role normalizeRole(
            Object value
    ) {

        if (value instanceof Role role) {
            return role;
        }

        String normalized =
                String.valueOf(value)
                        .trim()
                        .toUpperCase(Locale.ROOT)
                        .replaceAll("[\\s-]+", "_");

        if (normalized.isBlank()) {
            throw new RuntimeException("Access role is required");
        }

        try {
            return Role.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(
                    "Invalid access role: " + value
            );
        }
    }

    private List<Role> allRoles(
            User user
    ) {

        LinkedHashSet<Role> roles =
                new LinkedHashSet<>();

        if (user.getRole() != null) {
            roles.add(user.getRole());
        }

        if (user.getExtraRoles() != null) {
            roles.addAll(user.getExtraRoles());
        }

        return List.copyOf(roles);
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

    private void preventRemovingLastActiveSuperAdmin(
            User targetUser,
            List<Role> nextRoles
    ) {

        boolean currentlySuperAdmin =
                allRoles(targetUser).contains(Role.SUPER_ADMIN)
                        && targetUser.getActive();

        boolean removesSuperAdmin =
                currentlySuperAdmin &&
                        (nextRoles == null ||
                                !nextRoles.contains(Role.SUPER_ADMIN));

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
