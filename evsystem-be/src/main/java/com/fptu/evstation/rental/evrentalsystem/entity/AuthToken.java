package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "AuthTokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
