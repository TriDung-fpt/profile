package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.RegisterRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateProfileRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UploadVerificationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.VerifyRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.AccountStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.Role;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import org.mockito.quality.Strictness;
import com.fptu.evstation.rental.evrentalsystem.entity.VerificationStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.RoleRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.StationRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
// Xóa import MockMultipartFile vì không còn dùng
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private Role mockRenterRole;
    private Station mockStation;

    // Biến MockedStatic để giả lập hàm static của Files và Paths
    private MockedStatic<Paths> mockedPaths;
    private MockedStatic<Files> mockedFiles;

    // Các mock cho Path (vẫn cần thiết)
    @Mock
    private Path mockBasePath;
    @Mock
    private Path mockUserDir;
    @Mock
    private Path mockFilePath;

    @BeforeEach
    void setUp() {
        mockRenterRole = Role.builder().roleId(1L).roleName("EV_RENTER").build();
        mockStation = Station.builder().stationId(1L).name("Station 1").build();

        // --- SỬA LỖI: KHÔNG mock static ở đây nữa ---
    }

    // --- SỬA LỖI: Tạo hàm helper để mock static chỉ khi cần ---
    void setupFileMocks() {
        // Mock Paths.get()
        mockedPaths = mockStatic(Paths.class);
        mockedPaths.when(() -> Paths.get(anyString())).thenReturn(mockBasePath);
        mockedPaths.when(() -> Paths.get(anyString(), anyString())).thenReturn(mockBasePath);

        // Mock các đường dẫn con
        when(mockBasePath.resolve(anyString())).thenReturn(mockUserDir);
        when(mockUserDir.resolve(anyString())).thenReturn(mockFilePath);
        when(mockUserDir.getFileName()).thenReturn(mockBasePath); // Chỉ để lấy toString()
        when(mockBasePath.toString()).thenReturn("user_1");

        // Mock Files.createDirectories()
        mockedFiles = mockStatic(Files.class);
    }

    @AfterEach
    void tearDown() {
        // --- SỬA LỖI: Đóng mock static một cách an toàn ---
        if (mockedPaths != null) {
            mockedPaths.close();
        }
        if (mockedFiles != null) {
            mockedFiles.close();
        }
    }

    // --- SỬA LỖI: Helper tạo MultipartFile ĐÃ ĐƯỢC MOCK ---
    private MultipartFile createMockMultipartFile(String originalFilename, String contentType, long size) throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
        when(mockFile.getContentType()).thenReturn(contentType);
        when(mockFile.getSize()).thenReturn(size);
        // Quan trọng: Stub hàm transferTo để nó không làm gì cả
        doNothing().when(mockFile).transferTo(any(Path.class));
        return mockFile;
    }

    // --- Test 1: register ---

    @ParameterizedTest
    @CsvFileSource(resources = "/register_test_cases.csv", numLinesToSkip = 1)
    void testRegister_DataDriven(
            String fullName, String password, String confirmPassword, String email,
            String phone, boolean agreedToTerms, String expectedStatus, String expectedMessage) {

        RegisterRequest req = new RegisterRequest(fullName, password, confirmPassword, email, phone, agreedToTerms);

        if ("Email đã tồn tại".equals(expectedMessage)) {
            when(userRepository.existsByEmail(email)).thenReturn(true);
        } else {
            when(userRepository.existsByEmail(email)).thenReturn(false);
        }

        if ("Số điện thoại đã tồn tại".equals(expectedMessage)) {
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(userRepository.existsByPhone(phone)).thenReturn(true);
        } else {
            if (!"Email đã tồn tại".equals(expectedMessage)) {
                when(userRepository.existsByPhone(phone)).thenReturn(false);
            }
        }

        if ("SUCCESS".equals(expectedStatus)) {
            when(roleRepository.findByRoleName("EV_RENTER")).thenReturn(Optional.of(mockRenterRole));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }

        if ("SUCCESS".equals(expectedStatus)) {
            User registeredUser = assertDoesNotThrow(() -> userService.register(req));

            assertNotNull(registeredUser);
            assertEquals(email, registeredUser.getEmail());
            assertEquals(phone, registeredUser.getPhone());
            verify(userRepository, times(1)).save(any(User.class));
            verify(roleRepository, times(1)).findByRoleName("EV_RENTER");
        } else {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
                userService.register(req);
            });

            assertTrue(ex.getReason().contains(expectedMessage));

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Test
    void testRegister_Fail_NoRoleFound() {
        // Test luồng lỗi 500 khi không tìm thấy Role
        RegisterRequest req = new RegisterRequest("Test User", "password123", "password123", "test@test.com", "0900000000", true);

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByPhone("0900000000")).thenReturn(false);
        when(roleRepository.findByRoleName("EV_RENTER")).thenReturn(Optional.empty()); // Không tìm thấy Role

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userService.register(req);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Test 2: updateUserRole ---

    @Test
    void testUpdateUserRole_Success() {
        User mockUser = User.builder().userId(1L).build();
        Role newRole = Role.builder().roleId(2L).roleName("STAFF").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(newRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUserRole(1L, 2L);

        assertEquals(newRole, updatedUser.getRole());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void testUpdateUserRole_Fail_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userService.updateUserRole(999L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserRole_Fail_RoleNotFound() {
        User mockUser = User.builder().userId(1L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userService.updateUserRole(1L, 999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Test 3: updateUserStation ---

    @Test
    void testUpdateUserStation_Success() {
        User mockUser = User.builder().userId(1L).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUserStation(1L, 1L);

        assertEquals(mockStation, updatedUser.getStation());
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void testUpdateUserStation_Fail_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userService.updateUserStation(999L, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUserStation_Fail_StationNotFound() {
        User mockUser = User.builder().userId(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userService.updateUserStation(1L, 999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    // --- Test 4: updateUserProfile ---

    @Test
    void testUpdateUserProfile_Success() {
        User mockUser = User.builder()
                .userId(1L)
                .email("old@gmail.com")
                .phone("0900000000")
                .cccd("111")
                .gplx("222")
                .status(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING) // Chưa duyệt
                .build();

        UpdateProfileRequest req = new UpdateProfileRequest("New Name", "new@gmail.com", "0911111111", "12345", "54321");

        when(userRepository.existsByEmail("new@gmail.com")).thenReturn(false);
        when(userRepository.existsByPhone("0911111111")).thenReturn(false);
        when(userRepository.existsByCccd("12345")).thenReturn(false);
        when(userRepository.existsByGplx("54321")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateUserProfile(mockUser, req);

        assertEquals("New Name", updatedUser.getFullName());
        assertEquals("new@gmail.com", updatedUser.getEmail());
        assertEquals("0911111111", updatedUser.getPhone());
        assertEquals("12345", updatedUser.getCccd());
        assertEquals("54321", updatedUser.getGplx());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateUserProfile_Fail_AccountLocked() {
        User mockUser = User.builder().status(AccountStatus.INACTIVE).build(); // Bị khóa
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Fail_EmailExists() {
        User mockUser = User.builder().email("old@gmail.com").status(AccountStatus.ACTIVE).build();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("existing@gmail.com"); // Email đã tồn tại

        when(userRepository.existsByEmail("existing@gmail.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Fail_PhoneExists() {
        User mockUser = User.builder().phone("0900").status(AccountStatus.ACTIVE).build();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhone("0911"); // Phone đã tồn tại

        when(userRepository.existsByPhone("0911")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Fail_CccdExists() {
        User mockUser = User.builder().cccd("111").status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setCccd("987"); // CCCD đã tồn tại

        when(userRepository.existsByCccd("987")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Fail_GplxExists() {
        User mockUser = User.builder().gplx("111").status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setGplx("987"); // GPLX đã tồn tại

        when(userRepository.existsByGplx("987")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Fail_ApprovedUserCannotEditCccd() {
        User mockUser = User.builder().status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.APPROVED).build(); // Đã duyệt
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setCccd("987"); // Cố gắng sửa CCCD

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Fail_ApprovedUserCannotEditGplx() {
        User mockUser = User.builder().status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.APPROVED).build(); // Đã duyệt
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setGplx("987"); // Cố gắng sửa GPLX

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.updateUserProfile(mockUser, req)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testUpdateUserProfile_Success_WithBlankValues() {
        // Test trường hợp các trường trong req là blank/null, không gây lỗi
        User mockUser = User.builder().fullName("Old Name").status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UpdateProfileRequest req = new UpdateProfileRequest("   ", "", null, "   ", null); // Gửi blank

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User updatedUser = userService.updateUserProfile(mockUser, req);

        assertEquals("Old Name", updatedUser.getFullName()); // Giữ tên cũ
        verify(userRepository, times(1)).save(any(User.class));
    }

    // --- Test 5: uploadVerificationDocuments ---

    // --- SỬA LỖI: Cập nhật hàm helper này để dùng mockFile helper ---
    private UploadVerificationRequest createValidUploadRequest() throws IOException {
        UploadVerificationRequest req = new UploadVerificationRequest();
        req.setCccd("123456789");
        req.setGplx("987654321");
        req.setCccdFile1(createMockMultipartFile("cccd1.jpg", "image/jpeg", 1024));
        req.setCccdFile2(createMockMultipartFile("cccd2.png", "image/png", 1024));
        req.setGplxFile1(createMockMultipartFile("gplx1.jpg", "image/jpeg", 1024));
        req.setGplxFile2(createMockMultipartFile("gplx2.jpg", "image/jpeg", 1024));
        req.setSelfieFile(createMockMultipartFile("selfie.jpg", "image/jpeg", 1024));
        return req;
    }

    @Test
    void testUploadVerificationDocuments_Success() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        User mockUser = User.builder()
                .userId(1L).status(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING) // Đang chờ
                .cccd("111").gplx("222")
                .build();
        UploadVerificationRequest req = createValidUploadRequest();

        when(userRepository.existsByCccd(req.getCccd())).thenReturn(false);
        when(userRepository.existsByGplx(req.getGplx())).thenReturn(false);

        // Giả lập Files.createDirectories() thành công
        mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(mockUserDir);

        String result = userService.uploadVerificationDocuments(mockUser, req);

        assertEquals("Yêu cầu đã gửi. Vui lòng chờ nhân viên xác nhận trong vòng 24 giờ.", result);
        assertEquals(VerificationStatus.PENDING, mockUser.getVerificationStatus());

        // --- SỬA LỖI 1: Thêm dấu gạch dưới vào tên file ---
        assertTrue(mockUser.getCccdPath1().contains("cccd_1.jpg"));
        assertTrue(mockUser.getSelfiePath().contains("selfie.jpg"));
        verify(userRepository, times(1)).save(mockUser);
    }


    @Test
    void testUploadVerificationDocuments_Success_FromRejected() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng if (user.getVerificationStatus() == VerificationStatus.REJECTED)
        User mockUser = User.builder()
                .userId(1L).status(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.REJECTED) // Bị từ chối
                .rejectionReason("Ảnh mờ")
                .build();
        UploadVerificationRequest req = createValidUploadRequest();

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        userService.uploadVerificationDocuments(mockUser, req);

        assertEquals(VerificationStatus.PENDING, mockUser.getVerificationStatus()); // Chuyển về PENDING
        assertNull(mockUser.getRejectionReason()); // Xóa lý do từ chối cũ
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void testUploadVerificationDocuments_Fail_AccountLocked() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();
        User mockUser = User.builder().status(AccountStatus.INACTIVE).build(); // Bị khóa
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, createValidUploadRequest())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testUploadVerificationDocuments_Fail_AlreadyApproved() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();
        User mockUser = User.builder().status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.APPROVED).build(); // Đã duyệt
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, createValidUploadRequest())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testUploadVerificationDocuments_Fail_CccdExists() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).cccd("111").build();
        UploadVerificationRequest req = createValidUploadRequest();
        req.setCccd("999"); // CCCD mới

        when(userRepository.existsByCccd("999")).thenReturn(true); // CCCD này đã tồn tại

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Số CCCD này đã được sử dụng"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_GplxExists() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).gplx("111").build();
        UploadVerificationRequest req = createValidUploadRequest();
        req.setGplx("999"); // GPLX mới

        when(userRepository.existsByGplx("999")).thenReturn(true); // GPLX này đã tồn tại

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Số GPLX này đã được sử dụng"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_DirectoryCreationError() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng catch (IOException e)
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();

        // --- SỬA LỖI: Dùng any(Path.class) để đảm bảo mock được kích hoạt ---
        // Giả lập Files.createDirectories ném lỗi
        mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenThrow(new IOException("Lỗi IO"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, createValidUploadRequest())
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Lỗi hệ thống khi xử lý thư mục"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_MissingFiles() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UploadVerificationRequest req = createValidUploadRequest();
        req.setSelfieFile(null); // Thiếu file selfie

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Vui lòng tải lên đầy đủ 5 ảnh"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_InvalidFileType() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng private method validateFile (ContentType null)
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UploadVerificationRequest req = createValidUploadRequest();
        // --- SỬA LỖI: Dùng mock helper ---
        req.setCccdFile1(createMockMultipartFile("test.txt", null, 1024)); // ContentType null

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("phải là một file ảnh"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_InvalidContentType() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng private method validateFile (!startsWith("image/"))
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UploadVerificationRequest req = createValidUploadRequest();
        // --- SỬA LỖI: Dùng mock helper ---
        req.setCccdFile1(createMockMultipartFile("test.txt", "text/plain", 1024)); // ContentType text

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("phải là một file ảnh"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_FileSizeTooLarge() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng private method validateFile (Size > 5MB)
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UploadVerificationRequest req = createValidUploadRequest();
        // --- SỬA LỖI: Dùng mock helper ---
        req.setCccdFile1(createMockMultipartFile("test.jpg", "image/jpeg", 6 * 1024 * 1024)); // 6MB

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("không được vượt quá 5MB"));
    }

    @Test
    void testUploadVerificationDocuments_Fail_SaveFileError() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng catch (IOException e) trong private method saveFile
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UploadVerificationRequest req = createValidUploadRequest();

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        // --- SỬA LỖI: Giả lập transferTo ném lỗi (giờ đã có thể làm) ---
        doThrow(new IOException("Lỗi ghi file")).when(req.getCccdFile1()).transferTo(any(Path.class));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.uploadVerificationDocuments(mockUser, req)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Lỗi hệ thống khi lưu file"));
    }

    @Test
    void testSaveFile_Success_NoExtension() throws IOException {
        // --- SỬA LỖI: Gọi setupFileMocks ---
        setupFileMocks();

        // Test luồng (originalFilename != null && originalFilename.contains(".")) là false
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.PENDING).build();
        UploadVerificationRequest req = createValidUploadRequest();
        // Tạo file không có đuôi
        // --- SỬA LỖI: Dùng mock helper ---
        req.setSelfieFile(createMockMultipartFile("filename_no_extension", "image/jpeg", 1024));

        mockedFiles.when(() -> Files.createDirectories(mockUserDir)).thenReturn(mockUserDir);

        userService.uploadVerificationDocuments(mockUser, req);

        // Xác minh file được lưu không có đuôi
        assertTrue(mockUser.getSelfiePath().endsWith("selfie"));
    }

    // --- Test 6: getPendingVerifications ---

    @Test
    void testGetPendingVerifications_Success() {
        User user1 = User.builder().userId(1L).verificationStatus(VerificationStatus.PENDING).build();
        when(userRepository.findByVerificationStatus(VerificationStatus.PENDING)).thenReturn(List.of(user1));
        List<User> result = userService.getPendingVerifications();
        assertEquals(1, result.size());
    }

    // --- Test 7: processVerification ---

    @Test
    void testProcessVerification_Approve_Success() {
        User mockUser = User.builder().userId(1L).verificationStatus(VerificationStatus.PENDING).build();
        VerifyRequest req = new VerifyRequest(true, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = userService.processVerification(1L, req);
        assertEquals(VerificationStatus.APPROVED, mockUser.getVerificationStatus());
        assertNull(mockUser.getRejectionReason());
        assertTrue(result.contains("thành công"));
    }

    @Test
    void testProcessVerification_Reject_Success() {
        User mockUser = User.builder().userId(1L).verificationStatus(VerificationStatus.PENDING).build();
        VerifyRequest req = new VerifyRequest(false, "Ảnh mờ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = userService.processVerification(1L, req);
        assertEquals(VerificationStatus.REJECTED, mockUser.getVerificationStatus());
        assertEquals("Ảnh mờ", mockUser.getRejectionReason());
        assertTrue(result.contains("từ chối"));
    }

    @Test
    void testProcessVerification_Fail_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.processVerification(99L, new VerifyRequest(true, null))
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testProcessVerification_Fail_NotPending() {
        User mockUser = User.builder().userId(1L).verificationStatus(VerificationStatus.APPROVED).build(); // Đã duyệt
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.processVerification(1L, new VerifyRequest(true, null))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Chỉ có thể xử lý các yêu cầu đang ở trạng thái PENDING"));
    }

    @Test
    void testProcessVerification_Fail_NoReasonForRejection() {
        User mockUser = User.builder().userId(1L).verificationStatus(VerificationStatus.PENDING).build();
        VerifyRequest req = new VerifyRequest(false, "   "); // Lý do rỗng

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.processVerification(1L, req)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Phải nhập lý do khi từ chối"));
    }

    // --- Test 8: getVerificationStatus ---

    @Test
    void testGetVerificationStatus_Success_Approved() {
        User mockUser = User.builder().status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.APPROVED).build();
        Map<String, Object> result = userService.getVerificationStatus(mockUser);
        assertEquals("APPROVED", result.get("status"));
        assertFalse(result.containsKey("reason"));
    }

    @Test
    void testGetVerificationStatus_Success_Rejected() {
        User mockUser = User.builder().status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.REJECTED).rejectionReason("Ảnh mờ").build();
        Map<String, Object> result = userService.getVerificationStatus(mockUser);
        assertEquals("REJECTED", result.get("status"));
        assertEquals("Ảnh mờ", result.get("reason"));
    }

    @Test
    void testGetVerificationStatus_Success_Rejected_NoReason() {
        // Test luồng if (user.getVerificationStatus() == VerificationStatus.REJECTED && user.getRejectionReason() != null)
        User mockUser = User.builder().status(AccountStatus.ACTIVE).verificationStatus(VerificationStatus.REJECTED).rejectionReason(null).build();
        Map<String, Object> result = userService.getVerificationStatus(mockUser);
        assertEquals("REJECTED", result.get("status"));
        assertFalse(result.containsKey("reason")); // Không có lý do
    }

    @Test
    void testGetVerificationStatus_Fail_AccountLocked() {
        User mockUser = User.builder().status(AccountStatus.INACTIVE).build(); // Bị khóa
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.getVerificationStatus(mockUser)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- Test 9: unlockUserAccount ---

    @Test
    void testUnlockUserAccount_Success() {
        User mockUser = User.builder().userId(1L).status(AccountStatus.INACTIVE).cancellationCount(3).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User unlockedUser = userService.unlockUserAccount(1L);
        assertEquals(AccountStatus.ACTIVE, unlockedUser.getStatus());
        assertEquals(0, unlockedUser.getCancellationCount());
    }

    @Test
    void testUnlockUserAccount_Fail_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.unlockUserAccount(999L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testUnlockUserAccount_Fail_AlreadyActive() {
        User mockUser = User.builder().userId(1L).status(AccountStatus.ACTIVE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.unlockUserAccount(1L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // --- Test 10, 11, 12: Các hàm đơn giản ---
    @Test
    void testGetAllUsers_Success() {
        when(userRepository.findAll()).thenReturn(List.of(new User(), new User()));
        List<User> result = userService.getAllUsers();
        assertEquals(2, result.size());
    }

    @Test
    void testGetUserById_Success() {
        User mockUser = User.builder().userId(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        User result = userService.getUserById(1L);
        assertEquals(1L, result.getUserId());
    }

    @Test
    void testGetUserById_Fail_NotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> userService.getUserById(999L));
    }

    @Test
    void testSaveUser_Success() {
        User mockUser = User.builder().userId(1L).build();
        when(userRepository.save(mockUser)).thenReturn(mockUser);
        User result = userService.saveUser(mockUser);
        assertEquals(1L, result.getUserId());
    }
}