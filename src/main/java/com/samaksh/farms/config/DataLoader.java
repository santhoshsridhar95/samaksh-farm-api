package com.samaksh.farms.config;

import com.samaksh.farms.enums.Role;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public DataLoader(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.count() == 0) {

            User admin = new User();

            admin.setName("Santhosh");

            admin.setEmail("admin@samaksh.com");

            admin.setPassword(
                    passwordEncoder.encode("admin123")
            );

            admin.setRole(Role.SUPER_ADMIN);

            admin.setActive(true);

            admin.setCreatedAt(
                    LocalDateTime.now()
            );

            userRepository.save(admin);

            System.out.println(
                    "SUPER ADMIN CREATED"
            );
        }
    }
}