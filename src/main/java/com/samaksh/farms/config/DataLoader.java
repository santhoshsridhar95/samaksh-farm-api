package com.samaksh.farms.config;

import com.samaksh.farms.enums.Role;
import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.user.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.name:Samaksh Farms Admin}")
    private String adminName;

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

            if (adminEmail == null
                    || adminEmail.isBlank()
                    || adminPassword == null
                    || adminPassword.isBlank()) {

                log.warn(
                        "No users exist. Configure app.bootstrap.admin.email and app.bootstrap.admin.password to bootstrap the first SUPER_ADMIN."
                );

                return;
            }

            User admin = new User();

            admin.setName(adminName);

            admin.setEmail(
                    adminEmail.trim()
                            .toLowerCase(Locale.ROOT)
            );

            admin.setPassword(
                    passwordEncoder.encode(adminPassword)
            );

            admin.setRole(Role.SUPER_ADMIN);

            admin.setActive(true);

            admin.setCreatedAt(
                    LocalDateTime.now()
            );

            userRepository.save(admin);

            log.info(
                    "Initial SUPER_ADMIN user created for {}",
                    admin.getEmail()
            );
        }
    }
}
