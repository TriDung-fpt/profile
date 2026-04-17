package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.dto.ProfileResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateProfileRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UploadVerificationRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;
    private final UserServiceImpl userService;

    @GetMapping("/profile/me")
    public ResponseEntity<User> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authService.getTokenFromHeader(authHeader);
        User user = authService.validateTokenAndGetUser(token);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UpdateProfileRequest req) {
        String token = authService.getTokenFromHeader(authHeader);
        User user = authService.validateTokenAndGetUser(token);
        User updatedUser = userService.updateUserProfile(user, req);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công", "user", updatedUser));
    }

    @PostMapping("/profile/verification/upload")
    public ResponseEntity<?> uploadVerification(
            @RequestHeader("Authorization") String authHeader,
            @ModelAttribute UploadVerificationRequest req) {
        String token = authService.getTokenFromHeader(authHeader);
        User user = authService.validateTokenAndGetUser(token);
        String message = userService.uploadVerificationDocuments(user, req);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/profile/verification/status")
    public ResponseEntity<?> getVerificationStatus(@RequestHeader("Authorization") String authHeader) {
        User user = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        Map<String, Object> status = userService.getVerificationStatus(user);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/profile/role")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        User user = (User) authentication.getPrincipal();
        String fullName = user.getFullName();
        String role = user.getRole().getRoleName();

        return ResponseEntity.ok(new ProfileResponse(fullName, role));
    }

    @GetMapping("/utils/banks")
    public ResponseEntity<List<String>> getBankList() {
        List<String> bankList = Arrays.asList(
                "Vietcombank",
                "Techcombank",
                "MB Bank",
                "VietinBank",
                "BIDV",
                "ACB",
                "VPBank",
                "Sacombank",
                "TPBank",
                "Agribank"
        );
        return ResponseEntity.ok(bankList);
    }
}