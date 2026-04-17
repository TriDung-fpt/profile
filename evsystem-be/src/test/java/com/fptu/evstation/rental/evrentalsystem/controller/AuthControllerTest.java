package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.AuthResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.LoginRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.RegisterRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Role;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.impl.UserServiceImpl;
import com.fptu.evstation.rental.evrentalsystem.service.util.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserServiceImpl userService;

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private AuthResponse authResponse;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("test@example.com", "password123");
        authResponse = new AuthResponse("jwt-token-123", LocalDateTime.now().plusSeconds(3600), "Test User");
        registerRequest = new RegisterRequest("Test User", "Password123@", "Password123@", "test@example.com", "0987654321", true);
    }

    // --- Register Tests ---

    @Test
    void testRegister_Success() throws Exception {
        User user = User.builder().fullName("Test User").build();
        when(userService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã đăng ký thành công"))
                .andExpect(jsonPath("$.userName").value("Test User"));

        verify(userService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void testRegister_EmailExists() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRegister_InvalidRequest_BlankFullName() throws Exception {
        // Test @Valid annotation
        registerRequest.setFullName("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    // --- Login Tests ---

    @Test
    void testLogin_Success() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.fullName").value("Test User"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testLogin_AccountLocked() throws Exception {
        // Test case cho tài khoản bị khóa (403 Forbidden)
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Account locked"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLogin_EmptyIdentifier() throws Exception {
        // Đổi tên test case cho rõ nghĩa
        LoginRequest invalidRequest = new LoginRequest("", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_EmptyPassword() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("test@example.com", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // --- Logout Tests ---

    @Test
    void testLogout_Success() throws Exception {
        when(authService.getTokenFromHeader(anyString())).thenReturn("jwt-token-123");

        // 👇 Tạo user và role để tránh NullPointerException trong AuthTokenFilter
        Role mockRole = new Role();
        mockRole.setRoleName("USER");

        User mockUser = new User();
        mockUser.setUserId(10L);
        mockUser.setEmail("test@example.com");
        mockUser.setRole(mockRole);

        // 👇 Giả lập hành vi lấy user từ token JWT
        when(authService.validateTokenAndGetUser("jwt-token-123")).thenReturn(mockUser);

        // 👇 Giả lập hành vi logout (không làm gì)
        doNothing().when(authService).logout("jwt-token-123");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer jwt-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã đăng xuất thành công"));

        verify(authService, times(1)).logout("jwt-token-123");
    }




    @Test
    void testLogout_MissingHeader() throws Exception {
        // Test trường hợp thiếu header Authorization
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isBadRequest()); // Spring ném 400 khi thiếu @RequestHeader
    }

    @Test
    void testLogout_InvalidHeader() throws Exception {
        // Test trường hợp service ném lỗi (vd: header không có "Bearer ")
        when(authService.getTokenFromHeader(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu thiếu token xác thực"));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Token-invalid"))
                .andExpect(status().isUnauthorized());
    }

    // --- Google Login Tests ---

    @Test
    void testLoginWithGoogle_Success() throws Exception {
        String idToken = "google-id-token-123";
        // Sử dụng Map và ObjectMapper để tạo JSON body
        Map<String, String> requestBody = Map.of("idToken", idToken);

        when(authService.loginWithGoogle(idToken)).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"));

        verify(authService, times(1)).loginWithGoogle(idToken);
    }

    @Test
    void testLoginWithGoogle_InvalidToken() throws Exception {
        String idToken = "invalid-token";
        Map<String, String> requestBody = Map.of("idToken", idToken);

        when(authService.loginWithGoogle(idToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized());
    }

    // --- Forgot Password Tests (NEW) ---


    @Test
    void testForgotPassword_BlankEmail() throws Exception {
        // Test check `email == null || email.isBlank()` trong controller
        Map<String, String> payload = Map.of("email", "");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testForgotPassword_NullEmail() throws Exception {
        // Test check `email == null || email.isBlank()` trong controller
        Map<String, String> payload = Map.of(); // Không có key "email"

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testForgotPassword_UserNotFound() throws Exception {
        Map<String, String> payload = Map.of("email", "notfound@example.com");

        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(emailService).createPasswordResetToken("notfound@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    // --- Reset Password Tests (NEW) ---

    @Test
    void testResetPassword_Success() throws Exception {
        Map<String, String> payload = Map.of(
                "otp", "123456",
                "newPassword", "NewPassword123!",
                "confirmPassword", "NewPassword123!"
        );

        doNothing().when(emailService).resetPasswordWithOtp("123456", "NewPassword123!", "NewPassword123!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));

        verify(emailService, times(1)).resetPasswordWithOtp("123456", "NewPassword123!", "NewPassword123!");
    }

    @Test
    void testResetPassword_InvalidOtp() throws Exception {
        Map<String, String> payload = Map.of(
                "otp", "invalid-otp",
                "newPassword", "NewPassword123!",
                "confirmPassword", "NewPassword123!"
        );

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP không hợp lệ"))
                .when(emailService).resetPasswordWithOtp(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResetPassword_PasswordMismatch() throws Exception {
        Map<String, String> payload = Map.of(
                "otp", "123456",
                "newPassword", "NewPassword123!",
                "confirmPassword", "WrongPassword!"
        );

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu nhập lại không khớp"))
                .when(emailService).resetPasswordWithOtp(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }
}