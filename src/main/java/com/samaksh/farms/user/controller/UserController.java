package com.samaksh.farms.user.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.user.dto.*;
import com.samaksh.farms.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {

        return ApiResponse
                .<List<UserResponse>>builder()
                .success(true)
                .message("Users fetched successfully")
                .data(userService.getAllUsers())
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> createUser(
            @Valid @RequestBody UserRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("User created successfully")
                .data(
                        userService.createUser(
                                request,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> disableUser(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("User disabled successfully")
                .data(
                        userService.disableUser(
                                id,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> enableUser(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("User enabled successfully")
                .data(
                        userService.enableUser(
                                id,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> approveUser(
            @PathVariable Long id,
            @RequestBody ApproveUserRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("User approved successfully")
                .data(
                        userService.approveUser(
                                id,
                                request,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> rejectUser(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("User rejected successfully")
                .data(
                        userService.rejectUser(
                                id,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("Role updated successfully")
                .data(
                        userService.changeRole(
                                id,
                                request,
                                authentication
                        )
                )
                .build();
    }

    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<String> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request,
            Authentication authentication
    ) {

        userService.resetPassword(
                id,
                request,
                authentication
        );

        return ApiResponse
                .<String>builder()
                .success(true)
                .message("Password reset successfully")
                .data("SUCCESS")
                .build();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> changePermissions(
            @PathVariable Long id,
            @RequestBody ChangePermissionsRequest request,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("Entitlements updated successfully")
                .data(
                        userService.changePermissions(
                                id,
                                request,
                                authentication
                        )
                )
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> deleteUser(
            @PathVariable Long id,
            Authentication authentication
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("User deleted successfully")
                .data(
                        userService.softDeleteUser(
                                id,
                                authentication
                        )
                )
                .build();
    }
}
