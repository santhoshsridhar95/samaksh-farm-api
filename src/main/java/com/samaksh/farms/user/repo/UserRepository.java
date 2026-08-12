package com.samaksh.farms.user.repo;

import com.samaksh.farms.user.entity.User;
import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(
            String email
    );

    Optional<User> findByPhoneNumber(
            String phoneNumber
    );

    Optional<User> findByEmailVerificationToken(
            String emailVerificationToken
    );

    boolean existsByEmail(
            String email
    );

    boolean existsByPhoneNumber(
            String phoneNumber
    );

    long countByRoleAndActiveTrue(
            Role role
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Query(
            """
            UPDATE User user
            SET user.active = true,
                user.emailVerified = true,
                user.emailVerificationOtp = null,
                user.emailVerificationToken = null,
                user.emailVerificationExpiresAt = null,
                user.approvalStatus = :approvalStatus,
                user.approvedAt = :approvedAt
            WHERE user.id = :id
            """
    )
    int approveById(
            @Param("id") Long id,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("approvedAt") LocalDateTime approvedAt
    );
}
