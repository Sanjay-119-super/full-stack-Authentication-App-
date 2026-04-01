package com.sanjay.auth.auth_app.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh-token", indexes = {
        @Index(name = "refresh_token_jti_idx" ,columnList = "jti", unique = true),
        @Index(name = "refresh_token_user_id_idx" ,columnList = "user_id")
})

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "jti",unique = true, nullable = false , updatable = false)
    private String jti; // jti=token

    @ManyToOne
    @JoinColumn(name = "user_id" , nullable = false, updatable = false)
    private User user;

    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private String replacedByToken;
}
