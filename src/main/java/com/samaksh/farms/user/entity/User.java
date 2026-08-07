package com.samaksh.farms.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.samaksh.farms.enums.ApprovalStatus;
import com.samaksh.farms.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {
        "password",
        "extraPermissions"
})
public class User implements Principal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(
            nullable = false,
            unique = true
    )
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    @JsonIgnore
    private String password;

    private Boolean emailVerified;

    private String emailVerificationOtp;

    private String emailVerificationToken;

    private LocalDateTime emailVerificationExpiresAt;

    private String authProvider;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_extra_permissions",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "permission_key")
    private List<String> extraPermissions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Boolean active;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
