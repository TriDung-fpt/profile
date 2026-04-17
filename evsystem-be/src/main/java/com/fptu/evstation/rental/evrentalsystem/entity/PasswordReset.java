package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordReset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String otpCode;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;
}