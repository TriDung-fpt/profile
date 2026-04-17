package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.entity.AuthToken;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.AuthTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// SỬA: Import LocalDateTime thay vì Instant
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private AuthTokenRepository authTokenRepository;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private User mockUser;
    private AuthToken mockToken;
    private AuthToken expiredToken;
    private AuthToken nullExpiryToken;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .fullName("Test User")
                .build();

        // SỬA: Dùng LocalDateTime
        mockToken = AuthToken.builder()
                .id(1L)
                .token("test-token-123")
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plus(2, ChronoUnit.HOURS)) // Token còn hạn
                .build();

        expiredToken = AuthToken.builder()
                .id(2L)
                .token("expired-token")
                .user(mockUser)
                .expiresAt(LocalDateTime.now().minusSeconds(100)) // Token hết hạn
                .build();

        nullExpiryToken = AuthToken.builder()
                .id(3L)
                .token("null-expiry-token")
                .user(mockUser)
                .expiresAt(null) // Token null expiry
                .build();
    }

    // --- Test 1: createToken ---

    @Test
    void testCreateToken_Success_NewToken() {
        // Test trường hợp user chưa có token nào (if (false))
        // 1. Chuẩn bị
        when(authTokenRepository.findByUser(mockUser)).thenReturn(Collections.emptyList());
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Thực thi
        AuthToken result = tokenService.createToken(mockUser);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(mockUser, result.getUser());
        assertNotNull(result.getToken());

        // Xác minh KHÔNG gọi deleteAll
        verify(authTokenRepository, never()).deleteAll(anyList());
        verify(authTokenRepository, times(1)).save(any(AuthToken.class));
    }

    @Test
    void testCreateToken_Success_ReplaceExistingToken() {
        // Test trường hợp user đã có token (if (true))
        // 1. Chuẩn bị
        List<AuthToken> existingTokens = List.of(mockToken, expiredToken);
        when(authTokenRepository.findByUser(mockUser)).thenReturn(existingTokens);
        when(authTokenRepository.save(any(AuthToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Thực thi
        AuthToken result = tokenService.createToken(mockUser);

        // 3. Xác minh
        assertNotNull(result);

        // Xác minh ĐÃ gọi deleteAll
        verify(authTokenRepository, times(1)).deleteAll(existingTokens);
        verify(authTokenRepository, times(1)).save(any(AuthToken.class));
    }

    // --- Test 2: deleteToken ---

    @Test
    void testDeleteToken_Success() {
        // 1. Chuẩn bị
        when(authTokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(mockToken));

        // 2. Thực thi
        assertDoesNotThrow(() -> tokenService.deleteToken("test-token-123"));

        // 3. Xác minh
        verify(authTokenRepository, times(1)).findByToken("test-token-123");
        verify(authTokenRepository, times(1)).delete(mockToken);
    }

    @Test
    void testDeleteToken_Fail_TokenNotFound() {
        // Test luồng orElseThrow()
        // 1. Chuẩn bị
        when(authTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tokenService.deleteToken("invalid-token")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode()); // 400
        assertTrue(ex.getReason().contains("Token không tồn tại"));
        verify(authTokenRepository, never()).delete(any());
    }

    // --- Test 3: validateTokenAndGetUser ---

    @Test
    void testValidateTokenAndGetUser_Success_ValidToken() {
        // Test luồng thành công
        // 1. Chuẩn bị
        when(authTokenRepository.findByToken("test-token-123")).thenReturn(Optional.of(mockToken));

        // 2. Thực thi
        User result = tokenService.validateTokenAndGetUser("test-token-123");

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(mockUser, result);
        verify(authTokenRepository, times(1)).findByToken("test-token-123");
        verify(authTokenRepository, never()).delete(any()); // Không xóa token
    }

    @Test
    void testValidateTokenAndGetUser_Fail_TokenNotFound() {
        // Test luồng if (tokenOpt.isEmpty())
        // 1. Chuẩn bị
        when(authTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tokenService.validateTokenAndGetUser("invalid-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode()); // 401
        assertTrue(ex.getReason().contains("Phiên đăng nhập không hợp lệ"));
        verify(authTokenRepository, never()).delete(any());
    }

    @Test
    void testValidateTokenAndGetUser_Fail_TokenExpired() {
        // Test luồng if (...isBefore(LocalDateTime.now()))
        // 1. Chuẩn bị
        when(authTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tokenService.validateTokenAndGetUser("expired-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode()); // 401
        assertTrue(ex.getReason().contains("Phiên đăng nhập đã hết hạn"));
        verify(authTokenRepository, times(1)).delete(expiredToken); // Phải xóa token
    }

    @Test
    void testValidateTokenAndGetUser_Fail_TokenExpiresAtNull() {
        // Test luồng if (t.getExpiresAt() == null...)
        // 1. Chuẩn bị
        when(authTokenRepository.findByToken("null-expiry-token")).thenReturn(Optional.of(nullExpiryToken));

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                tokenService.validateTokenAndGetUser("null-expiry-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode()); // 401
        assertTrue(ex.getReason().contains("Phiên đăng nhập đã hết hạn"));
        verify(authTokenRepository, times(1)).delete(nullExpiryToken); // Phải xóa token
    }
}