package com.samaksh.farms.auth.controller;

import com.samaksh.farms.auth.dto.LoginRequest;
import com.samaksh.farms.auth.dto.LoginResponse;
import com.samaksh.farms.auth.dto.ForgotPasswordRequest;
import com.samaksh.farms.auth.dto.SignupRequest;
import com.samaksh.farms.common.dto.ApiResponse;
import com.samaksh.farms.user.dto.UserResponse;
import com.samaksh.farms.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid
            @RequestBody
            LoginRequest request
    ) {

        return authService.login(
                request
        );
    }

    @PostMapping("/signup")
    public ApiResponse<UserResponse> signup(
            @Valid
            @RequestBody
            SignupRequest request
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("Signup submitted for super admin approval")
                .data(authService.signup(request))
                .build();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(
            @Valid
            @RequestBody
            ForgotPasswordRequest request
    ) {

        authService.requestPasswordReset(request);

        return ApiResponse
                .<String>builder()
                .success(true)
                .message("Password reset request submitted for super admin approval")
                .data("SUCCESS")
                .build();
    }
}
