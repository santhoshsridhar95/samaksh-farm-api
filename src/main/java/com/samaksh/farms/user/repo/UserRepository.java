package com.samaksh.farms.user.repo;

import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(
            String email
    );

    long countByRoleAndActiveTrue(
            Role role
    );
}
