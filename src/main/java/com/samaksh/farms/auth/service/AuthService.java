package com.samaksh.farms.auth.service;

import com.samaksh.farms.auth.dto.LoginRequest;
import com.samaksh.farms.auth.dto.LoginResponse;
import com.samaksh.farms.auth.dto.ForgotPasswordRequest;
import com.samaksh.farms.auth.dto.GoogleAuthRequest;
import com.samaksh.farms.auth.dto.SignupRequest;
import com.samaksh.farms.auth.dto.VerifyEmailRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final EmailVerificationService emailVerificationService;

    private final GoogleTokenVerifier googleTokenVerifier;

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
                    loginBlockedMessage(user.getApprovalStatus())
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
                .roles(
                        roleNames(user)
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
                        .emailVerified(false)
                        .emailVerificationOtp(generateOtp())
                        .emailVerificationToken(UUID.randomUUID().toString())
                        .emailVerificationExpiresAt(LocalDateTime.now().plusMinutes(15))
                        .authProvider("LOCAL")
                        .approvalStatus(ApprovalStatus.EMAIL_VERIFICATION_PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();

        User savedUser =
                userRepository.save(user);

        emailVerificationService.sendVerification(savedUser);

        return mapToUserResponse(savedUser);
    }

    public UserResponse verifyEmail(
            VerifyEmailRequest request
    ) {

        User user =
                findVerificationUser(request);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return mapToUserResponse(user);
        }

        if (user.getEmailVerificationExpiresAt() == null ||
                user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {

            user.setEmailVerificationOtp(generateOtp());
            user.setEmailVerificationToken(UUID.randomUUID().toString());
            user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(15));
            User savedUser = userRepository.save(user);
            emailVerificationService.sendVerification(savedUser);

            throw new RuntimeException(
                    "Verification expired. A new OTP has been sent."
            );
        }

        user.setEmailVerified(true);
        user.setEmailVerificationOtp(null);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationExpiresAt(null);
        user.setApprovalStatus(ApprovalStatus.PENDING);
        user.setActive(false);

        return mapToUserResponse(
                userRepository.save(user)
        );
    }

    public LoginResponse googleAuth(
            GoogleAuthRequest request
    ) {

        GoogleTokenVerifier.GoogleProfile profile =
                googleTokenVerifier.verify(
                        request.getIdToken()
                );

        String email =
                profile.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        boolean signupIntent =
                "SIGNUP".equalsIgnoreCase(request.getIntent());

        User user =
                userRepository.findByEmail(email)
                        .orElse(null);

        if (user == null) {
            if (!signupIntent) {
                throw new BadCredentialsException(
                        "Google account is verified, but this user does not exist in our system"
                );
            }

            user =
                    userRepository.save(
                            User.builder()
                                    .name(profile.getName() == null || profile.getName().isBlank()
                                            ? email
                                            : profile.getName())
                                    .email(email)
                                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                    .role(Role.SALES_EMPLOYEE)
                                    .active(false)
                                    .emailVerified(true)
                                    .authProvider("GOOGLE")
                                    .approvalStatus(ApprovalStatus.PENDING)
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );

            throw new DisabledException(
                    "Google signup submitted for super admin approval"
            );
        }

        if (signupIntent) {
            throw new RuntimeException(
                    "Email already exists. Use Google login instead."
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
                    loginBlockedMessage(user.getApprovalStatus())
            );
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            user.setAuthProvider("GOOGLE");
            userRepository.save(user);
        }

        String token =
                jwtService.generateToken(user);

        return LoginResponse.builder()
                .userId(user.getId())
                .token(token)
                .role(user.getRole().name())
                .roles(roleNames(user))
                .name(user.getName())
                .active(user.getActive())
                .build();
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
                .roles(
                        userRoles(user)
                )
                .active(user.getActive())
                .approvalStatus(
                        user.getApprovalStatus() == null
                                ? ApprovalStatus.APPROVED
                                : user.getApprovalStatus()
                )
                .emailVerified(
                        Boolean.TRUE.equals(user.getEmailVerified())
                )
                .build();
    }

    private User findVerificationUser(
            VerifyEmailRequest request
    ) {

        if (request.getToken() != null &&
                !request.getToken().isBlank()) {

            return userRepository.findByEmailVerificationToken(
                    request.getToken().trim()
            ).orElseThrow(
                    () -> new RuntimeException("Invalid verification link")
            );
        }

        if (request.getEmail() == null ||
                request.getEmail().isBlank() ||
                request.getOtp() == null ||
                request.getOtp().isBlank()) {

            throw new RuntimeException(
                    "Email and OTP are required"
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(
                                () -> new RuntimeException("Invalid email or OTP")
                        );

        if (!request.getOtp().trim().equals(user.getEmailVerificationOtp())) {
            throw new RuntimeException("Invalid email or OTP");
        }

        return user;
    }

    private String generateOtp() {

        return String.valueOf(
                ThreadLocalRandom.current().nextInt(100000, 1000000)
        );
    }

    private List<String> roleNames(
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

        return roles.stream()
                .map(Role::name)
                .toList();
    }

    private List<Role> userRoles(
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

    private String loginBlockedMessage(
            ApprovalStatus status
    ) {

        if (status == ApprovalStatus.EMAIL_VERIFICATION_PENDING) {
            return "Please verify your email before super admin approval";
        }

        if (status == ApprovalStatus.RESET_REQUESTED) {
            return "Password reset is awaiting super admin approval";
        }

        return "User account is awaiting super admin approval";
    }
}
