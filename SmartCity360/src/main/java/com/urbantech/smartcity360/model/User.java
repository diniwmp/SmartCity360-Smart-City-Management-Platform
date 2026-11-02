
package com.urbantech.smartcity360.model;

import com.urbantech.smartcity360.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User extends BaseEntity {
    @Column(nullable = false, length = 100) private String fullName;
    @Column(nullable = false, unique = true, length = 50) private String username;
    @Column(nullable = false, unique = true, length = 120) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(length = 20) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserRole role;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
}