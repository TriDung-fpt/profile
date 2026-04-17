package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateProfileRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UploadVerificationRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.AccountStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.Role;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.entity.VerificationStatus;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = ProfileController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserServiceImpl userService;

    private User mockUser;
    private String mockAuthHeader;
    private String mockToken;

    @BeforeEach
    void setUp() {
        // Mock Role (Vì file Role.java không được cung cấp, ta mock nó)
        // Dựa trên code, Role cần phương thức getRoleName()
        Role mockRole = mock(Role.class);
        when(mockRole.getRoleName()).thenReturn("EV_RENTER");

        // Mock User
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setFullName("Test User");
        mockUser.setEmail("test@example.com");
        mockUser.setRole(mockRole);
        mockUser.setStatus(AccountStatus.ACTIVE);
        mockUser.setVerificationStatus(VerificationStatus.PENDING);

        mockAuthHeader = "Bearer mock-token-123";
        mockToken = "mock-token-123";

        // Mock auth service behavior
        when(authService.getTokenFromHeader(mockAuthHeader)).thenReturn(mockToken);
        when(authService.validateTokenAndGetUser(mockToken)).thenReturn(mockUser);
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp SecurityContextHolder sau mỗi test
        SecurityContextHolder.clearContext();
    }

    // --- GET /profile/me ---

    @Test
    void testGetMyProfile_Success() throws Exception {
        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    void testGetMyProfile_InvalidToken() throws Exception {
        when(authService.validateTokenAndGetUser(mockToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token"));

        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMyProfile_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isBadRequest()); // Thiếu @RequestHeader
    }

    // --- PUT /profile/update ---

    @Test
    void testUpdateProfile_Success() throws Exception {
        UpdateProfileRequest req = UpdateProfileRequest.builder()
                .fullName("New Name")
                .email("new@example.com")
                .build();

        User updatedUser = new User();
        updatedUser.setUserId(1L);
        updatedUser.setFullName("New Name");
        updatedUser.setEmail("new@example.com");

        when(userService.updateUserProfile(any(User.class), any(UpdateProfileRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/profile/update")
                        .header("Authorization", mockAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin thành công"))
                .andExpect(jsonPath("$.user.fullName").value("New Name"));
    }

    @Test
    void testUpdateProfile_Forbidden() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        when(userService.updateUserProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản bị khóa"));

        mockMvc.perform(put("/api/profile/update")
                        .header("Authorization", mockAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // --- POST /profile/verification/upload ---

    @Test
    void testUploadVerification_Success() throws Exception {
        MockMultipartFile cccd1 = new MockMultipartFile("cccdFile1", "cccd1.jpg", "image/jpeg", "img1".getBytes());
        MockMultipartFile cccd2 = new MockMultipartFile("cccdFile2", "cccd2.jpg", "image/jpeg", "img2".getBytes());
        MockMultipartFile gplx1 = new MockMultipartFile("gplxFile1", "gplx1.jpg", "image/jpeg", "img3".getBytes());
        MockMultipartFile gplx2 = new MockMultipartFile("gplxFile2", "gplx2.jpg", "image/jpeg", "img4".getBytes());
        MockMultipartFile selfie = new MockMultipartFile("selfieFile", "selfie.jpg", "image/jpeg", "img5".getBytes());

        when(userService.uploadVerificationDocuments(any(User.class), any(UploadVerificationRequest.class)))
                .thenReturn("Yêu cầu đã gửi. Vui lòng chờ nhân viên xác nhận trong vòng 24 giờ.");

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/profile/verification/upload")
                        .file(cccd1)
                        .file(cccd2)
                        .file(gplx1)
                        .file(gplx2)
                        .file(selfie)
                        .param("cccd", "123456789") // Phải dùng .param() cho @ModelAttribute
                        .param("gplx", "987654321")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Yêu cầu đã gửi. Vui lòng chờ nhân viên xác nhận trong vòng 24 giờ."));
    }

    @Test
    void testUploadVerification_AlreadyApproved() throws Exception {
        when(userService.uploadVerificationDocuments(any(User.class), any(UploadVerificationRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực"));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/profile/verification/upload")
                        .param("cccd", "123456789")
                        .param("gplx", "987654321")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- GET /profile/verification/status ---

    @Test
    void testGetVerificationStatus_Success_Pending() throws Exception {
        Map<String, Object> statusMap = Map.of("status", "PENDING");
        when(userService.getVerificationStatus(mockUser)).thenReturn(statusMap);

        mockMvc.perform(get("/api/profile/verification/status")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void testGetVerificationStatus_Success_Rejected() throws Exception {
        Map<String, Object> statusMap = Map.of("status", "REJECTED", "reason", "Ảnh chụp mờ");
        when(userService.getVerificationStatus(mockUser)).thenReturn(statusMap);

        mockMvc.perform(get("/api/profile/verification/status")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Ảnh chụp mờ"));
    }

    @Test
    void testGetVerificationStatus_AccountLocked() throws Exception {
        when(userService.getVerificationStatus(mockUser))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa."));

        mockMvc.perform(get("/api/profile/verification/status")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    // --- GET /profile/role ---

    @Test
    void testGetCurrentUser_Success() throws Exception {
        // Cần mock SecurityContextHolder cho endpoint này
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.isAuthenticated()).thenReturn(true);
        // Quan trọng: Phải trả về đối tượng User thật (hoặc mock) vì code có ép kiểu
        when(authentication.getPrincipal()).thenReturn(mockUser);

        mockMvc.perform(get("/api/profile/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("EV_RENTER"));
    }

    @Test
    void testGetCurrentUser_Unauthenticated() throws Exception {
        // Mặc định SecurityContextHolder là null
        mockMvc.perform(get("/api/profile/role"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void testGetCurrentUser_AuthenticationNotAuthenticated() throws Exception {
        // Mock trường hợp có Authentication nhưng isAuthenticated() = false
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/profile/role"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    // --- GET /utils/banks ---

    @Test
    void testGetBankList() throws Exception {
        mockMvc.perform(get("/api/utils/banks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0]").value("Vietcombank"))
                .andExpect(jsonPath("$[9]").value("Agribank"));
    }
}