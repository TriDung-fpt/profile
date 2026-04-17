package com.fptu.evstation.rental.evrentalsystem.service.util;

import com.fptu.evstation.rental.evrentalsystem.entity.PasswordReset;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.PasswordResetRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;
    @Mock
    private PasswordResetRepository tokenRepo;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private EmailService emailService;

    @Test
    void testResetPasswordWithOtp_Success() {
        User mockUser = User.builder().userId(1L).password("oldPassword").build();
        PasswordReset mockToken = new PasswordReset(1L, "123456", LocalDateTime.now().plusMinutes(5), mockUser);

        when(tokenRepo.findByOtpCode("123456")).thenReturn(Optional.of(mockToken));

        assertDoesNotThrow(() -> {
            emailService.resetPasswordWithOtp("123456", "NewValidPass@1", "NewValidPass@1");
        });

        assertEquals("NewValidPass@1", mockUser.getPassword());

        verify(userRepo, times(1)).save(mockUser);
        verify(tokenRepo, times(1)).deleteByUser(mockUser);
    }

    @Test
    void testResetPasswordWithOtp_Fail_OtpExpired() {
        User mockUser = User.builder().userId(1L).build();
        PasswordReset mockToken = new PasswordReset(1L, "123456", LocalDateTime.now().minusMinutes(5), mockUser);
        when(tokenRepo.findByOtpCode("123456")).thenReturn(Optional.of(mockToken));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            emailService.resetPasswordWithOtp("123456", "NewValidPass@1", "NewValidPass@1");
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("OTP đã hết hạn"));

        verify(tokenRepo, times(1)).delete(mockToken);
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void testResetPasswordWithOtp_Fail_PasswordMismatch() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            emailService.resetPasswordWithOtp("123456", "NewValidPass@1", "WRONG_PASSWORD");
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Mật khẩu nhập lại không khớp"));

        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void testResetPasswordWithOtp_Fail_InvalidPasswordPolicy() {
        User mockUser = User.builder().userId(2L).password("old").build();
        PasswordReset mockToken = new PasswordReset(2L, "654321", LocalDateTime.now().plusMinutes(5), mockUser);
        when(tokenRepo.findByOtpCode("654321")).thenReturn(Optional.of(mockToken));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.resetPasswordWithOtp("654321", "short", "short")
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Mật khẩu phải có ít nhất 6 ký tự"));
        verify(userRepo, never()).save(any(User.class));
        verify(tokenRepo, never()).deleteByUser(any());
    }

    @Test
    void testResetPasswordWithOtp_Fail_OtpBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.resetPasswordWithOtp(" ", "NewValidPass@1", "NewValidPass@1")
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("OTP không được để trống"));
        verifyNoInteractions(userRepo, tokenRepo);
    }

    @Test
    void testResetPasswordWithOtp_Fail_PasswordsBlank() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.resetPasswordWithOtp("123456", "", "")
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Mật khẩu mới và nhập lại mật khẩu không được để trống"));
        verifyNoInteractions(userRepo);
    }

    @Test
    void testResetPasswordWithOtp_Fail_OtpNotFound() {
        when(tokenRepo.findByOtpCode("000000")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.resetPasswordWithOtp("000000", "NewValidPass@1", "NewValidPass@1")
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("OTP không hợp lệ"));
        verify(userRepo, never()).save(any());
    }

    @Test
    void testCreatePasswordResetToken_SendsEmailAndSavesToken() {
        User user = User.builder().userId(5L).email("test@example.com").fullName("Test User").build();
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(tokenRepo.findByUser(user)).thenReturn(Optional.empty());

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        String otp = emailService.createPasswordResetToken("test@example.com");

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(tokenRepo).save(any(PasswordReset.class));
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sent = messageCaptor.getValue();
        assertArrayEquals(new String[]{"test@example.com"}, sent.getTo());
        assertTrue(sent.getSubject().contains("Yêu cầu đặt lại mật khẩu"));
        assertTrue(sent.getText().contains("Mã xác thực của bạn là"));
    }

    @Test
    void testCreatePasswordResetToken_ExistingValidToken_Throws() {
        User user = User.builder().userId(6L).email("exists@example.com").fullName("User Exists").build();
        when(userRepo.findByEmail("exists@example.com")).thenReturn(Optional.of(user));
        PasswordReset existing = new PasswordReset(10L, "999999", LocalDateTime.now().plusSeconds(30), user);
        when(tokenRepo.findByUser(user)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                emailService.createPasswordResetToken("exists@example.com")
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("OTP đã được gửi"));
        verify(tokenRepo, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testCreatePasswordResetToken_ExistingExpiredToken_Replaces() {
        User user = User.builder().userId(7L).email("expired@example.com").fullName("Expired User").build();
        when(userRepo.findByEmail("expired@example.com")).thenReturn(Optional.of(user));
        PasswordReset existing = new PasswordReset(11L, "111111", LocalDateTime.now().minusSeconds(10), user);
        when(tokenRepo.findByUser(user)).thenReturn(Optional.of(existing));

        String otp = emailService.createPasswordResetToken("expired@example.com");

        assertNotNull(otp);
        verify(tokenRepo).delete(existing);
        verify(tokenRepo).save(any(PasswordReset.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
