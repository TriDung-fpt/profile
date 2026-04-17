package com.fptu.evstation.rental.evrentalsystem.service.scheduling;

import com.fptu.evstation.rental.evrentalsystem.repository.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenCleanupServiceTest {

    @Mock
    private AuthTokenRepository tokenRepo;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Test
    void testCleanupExpiredTokens() {
        // Given
        // No specific setup needed as we are just verifying the method call

        // When
        tokenCleanupService.cleanupExpiredTokens();

        // Then
        // Verify that the deleteExpiredBefore method was called once
        // We use any(LocalDateTime.class) because the exact time will vary
        verify(tokenRepo, times(1)).deleteExpiredBefore(any(LocalDateTime.class));
    }
}
