package com.samaksh.farms.user.controller;

import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.user.dto.*;
import com.samaksh.farms.user.service.UserService;
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
            @RequestBody UserRequest request,
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

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<UserResponse> changeRole(
            @PathVariable Long id,
            @RequestBody ChangeRoleRequest request,
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
            @RequestBody ResetPasswordRequest request,
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
}