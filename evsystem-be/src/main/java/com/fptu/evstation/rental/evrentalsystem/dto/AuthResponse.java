package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private LocalDateTime expiresAt;
    private String fullName;
}

