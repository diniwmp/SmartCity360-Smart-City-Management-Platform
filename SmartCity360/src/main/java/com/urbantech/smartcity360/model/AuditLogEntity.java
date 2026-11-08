package com.urbantech.smartcity360.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLogEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 80) private String action;
    @Column(length = 50) private String entityType;
    private Long entityId;
    @Column(columnDefinition = "TEXT") private String details;
    @Column(length = 45) private String ipAddress;
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
}