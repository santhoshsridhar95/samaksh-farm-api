package com.samaksh.farms.auth.controller;

import com.samaksh.farms.auth.dto.LoginRequest;
import com.samaksh.farms.auth.dto.LoginResponse;
import com.samaksh.farms.auth.dto.ForgotPasswordRequest;
import com.samaksh.farms.auth.dto.GoogleAuthRequest;
import com.samaksh.farms.auth.dto.SignupRequest;
import com.samaksh.farms.auth.dto.VerifyEmailRequest;
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

    @PostMapping("/verify-email")
    public ApiResponse<UserResponse> verifyEmail(
            @RequestBody VerifyEmailRequest request
    ) {

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("Email verified. Signup is now waiting for super admin approval.")
                .data(authService.verifyEmail(request))
                .build();
    }

    @GetMapping("/verify-email")
    public ApiResponse<UserResponse> verifyEmailLink(
            @RequestParam String token
    ) {

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(token);

        return ApiResponse
                .<UserResponse>builder()
                .success(true)
                .message("Email verified. Signup is now waiting for super admin approval.")
                .data(authService.verifyEmail(request))
                .build();
    }

    @PostMapping("/google")
    public LoginResponse googleAuth(
            @Valid
            @RequestBody GoogleAuthRequest request
    ) {

        return authService.googleAuth(request);
    }
}
