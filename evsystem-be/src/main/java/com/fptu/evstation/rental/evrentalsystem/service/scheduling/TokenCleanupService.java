package com.fptu.evstation.rental.evrentalsystem.service.scheduling;

import com.fptu.evstation.rental.evrentalsystem.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenCleanupService {
    private final AuthTokenRepository tokenRepo;

    @Scheduled(cron = "0 0 * * * ?")  // Chạy hàng giờ
    public void cleanupExpiredTokens() {
        tokenRepo.deleteExpiredBefore(LocalDateTime.now());
    }
}
