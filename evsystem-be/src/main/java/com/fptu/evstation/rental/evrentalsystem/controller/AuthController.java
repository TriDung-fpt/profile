package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.impl.UserServiceImpl;
import com.fptu.evstation.rental.evrentalsystem.service.util.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserServiceImpl userService;
    private final EmailService emailService;
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.register(req);
        return ResponseEntity.ok(Map.of(
                "message", "Đã đăng ký thành công",
                "userName", user.getFullName()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse authResponse = authService.login(req);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody GoogleIdTokenRequest req) {
        AuthResponse resp = authService.loginWithGoogle(req.getIdToken());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization") String authHeader) {
        String token = authService.getTokenFromHeader(authHeader);
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất thành công"));
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống");
        }
        emailService.createPasswordResetToken(email);
        return ResponseEntity.ok(Map.of("message", "OTP đã được gửi tới email (có hiệu lực 5 phút)"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");

        emailService.resetPasswordWithOtp(otp, newPassword, confirmPassword);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
    }
}
