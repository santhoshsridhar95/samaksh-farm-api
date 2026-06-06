package com.samaksh.farms.user.controller;

import com.samaksh.farms.user.dto.UserRequest;
import com.samaksh.farms.user.dto.UserResponse;
import com.samaksh.farms.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> getUsers() {

        return userService.getAllUsers();
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public UserResponse createUser(
            @RequestBody UserRequest request
    ) {

        return userService.createUser(
                request
        );
    }
}