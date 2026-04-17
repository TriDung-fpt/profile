package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.AuthResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.LoginRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.RoleRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Inject the mocked RestTemplate into the service
        ReflectionTestUtils.setField(authService, "restTemplate", restTemplate);
        // Set a mock Google Client ID
        ReflectionTestUtils.setField(authService, "googleClientId", "test-google-client-id");
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/login_test_cases.csv", numLinesToSkip = 1)
    void testLogin_DataDriven(
            String identifier, String password, boolean mockUserExists,
            String mockPassword, String mockUserStatus,
            String expectedStatus, String expectedMessage) {

        LoginRequest req = new LoginRequest(identifier, password);
        User mockUser = User.builder()
                .userId(1L)
                .email(identifier)
                .password(mockPassword)
                .status(AccountStatus.valueOf(mockUserStatus))
                .build();

        if (!mockUserExists) {
            when(userRepository.findByEmail(identifier)).thenReturn(Optional.empty());
            when(userRepository.findByPhone(identifier)).thenReturn(Optional.empty());
        } else {
            when(userRepository.findByEmail(identifier)).thenReturn(Optional.of(mockUser));
        }

        if ("SUCCESS".equals(expectedStatus)) {
            AuthToken mockToken = AuthToken.builder()
                    .token("fake-jwt-token")
                    .expiresAt(LocalDateTime.now().plusSeconds(3600))
                    .build();
            when(tokenService.createToken(any(User.class))).thenReturn(mockToken);
        }

        if ("SUCCESS".equals(expectedStatus)) {
            AuthResponse response = assertDoesNotThrow(() -> authService.login(req));

            assertNotNull(response);
            assertEquals("fake-jwt-token", response.getToken());
            verify(tokenService, times(1)).createToken(any(User.class));
        } else {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
                authService.login(req);
            });

            assertTrue(ex.getReason().contains(expectedMessage));
            verify(tokenService, never()).createToken(any(User.class));
        }
    }

    @Test
    void testLogin_Success_WithPhoneNumber() {
        String phoneNumber = "0987654321";
        String password = "password123";
        LoginRequest req = new LoginRequest(phoneNumber, password);

        User mockUser = User.builder()
                .userId(1L)
                .phone(phoneNumber)
                .password(password)
                .status(AccountStatus.ACTIVE)
                .fullName("Test User")
                .build();

        when(userRepository.findByEmail(phoneNumber)).thenReturn(Optional.empty());
        when(userRepository.findByPhone(phoneNumber)).thenReturn(Optional.of(mockUser));

        AuthToken mockToken = AuthToken.builder()
                .token("fake-jwt-token-phone")
                .expiresAt(LocalDateTime.now().plusSeconds(3600))
                .build();
        when(tokenService.createToken(any(User.class))).thenReturn(mockToken);

        AuthResponse response = assertDoesNotThrow(() -> authService.login(req));

        assertNotNull(response);
        assertEquals("fake-jwt-token-phone", response.getToken());
        verify(userRepository, times(1)).findByEmail(phoneNumber);
        verify(userRepository, times(1)).findByPhone(phoneNumber);
        verify(tokenService, times(1)).createToken(any(User.class));
    }

    // --- Google Login Tests ---

    @Test
    void loginWithGoogle_Success_ExistingUser() {
        String idToken = "valid-id-token";
        String email = "existing@google.com";
        Map<String, Object> googlePayload = Map.of(
                "aud", "test-google-client-id",
                "email", email,
                "name", "Existing Google User"
        );
        ResponseEntity<Map> googleResponse = new ResponseEntity<>(googlePayload, HttpStatus.OK);

        User existingUser = User.builder().userId(1L).email(email).fullName("Existing Google User").build();
        AuthToken mockAuthToken = AuthToken.builder().token("auth-token").expiresAt(LocalDateTime.now().plusHours(1)).build();

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(googleResponse);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(tokenService.createToken(existingUser)).thenReturn(mockAuthToken);

        AuthResponse response = authService.loginWithGoogle(idToken);

        assertNotNull(response);
        assertEquals("auth-token", response.getToken());
        assertEquals("Existing Google User", response.getFullName());
        verify(userRepository, never()).save(any(User.class)); // Should not create a new user
    }

    @Test
    void loginWithGoogle_Success_NewUser() {
        String idToken = "valid-id-token-new";
        String email = "new@google.com";
        Map<String, Object> googlePayload = Map.of(
                "aud", "test-google-client-id",
                "email", email,
                "name", "New Google User"
        );
        ResponseEntity<Map> googleResponse = new ResponseEntity<>(googlePayload, HttpStatus.OK);

        Role renterRole = new Role();
        renterRole.setRoleName("EV_RENTER");

        User newUser = User.builder().email(email).fullName("New Google User").role(renterRole).build();
        AuthToken mockAuthToken = AuthToken.builder().token("new-auth-token").expiresAt(LocalDateTime.now().plusHours(1)).build();

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(googleResponse);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("EV_RENTER")).thenReturn(Optional.of(renterRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(tokenService.createToken(newUser)).thenReturn(mockAuthToken);

        AuthResponse response = authService.loginWithGoogle(idToken);

        assertNotNull(response);
        assertEquals("new-auth-token", response.getToken());
        assertEquals("New Google User", response.getFullName());
        verify(userRepository, times(1)).save(argThat(user ->
                user.getEmail().equals(email) && user.getRole().equals(renterRole)
        ));
    }

    @Test
    void loginWithGoogle_Fail_InvalidToken() {
        String idToken = "invalid-id-token";
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Google API error"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginWithGoogle(idToken));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Google id_token không hợp lệ"));
    }

    @Test
    void loginWithGoogle_Fail_InvalidAudience() {
        String idToken = "valid-id-token-wrong-aud";
        Map<String, Object> googlePayload = Map.of("aud", "wrong-client-id");
        ResponseEntity<Map> googleResponse = new ResponseEntity<>(googlePayload, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(googleResponse);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginWithGoogle(idToken));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid audience"));
    }

    @Test
    void loginWithGoogle_Fail_NoEmail() {
        String idToken = "valid-id-token-no-email";
        Map<String, Object> googlePayload = Map.of("aud", "test-google-client-id"); // No email
        ResponseEntity<Map> googleResponse = new ResponseEntity<>(googlePayload, HttpStatus.OK);

        when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenReturn(googleResponse);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.loginWithGoogle(idToken));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tài khoản Google không có email"));
    }

    // --- Other Tests ---

    @Test
    void testGetTokenFromHeader_Success() {
        String token = authService.getTokenFromHeader("Bearer abc.def.ghi");
        assertEquals("abc.def.ghi", token);
    }

    @Test
    void testGetTokenFromHeader_MissingOrInvalid() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () -> authService.getTokenFromHeader(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () -> authService.getTokenFromHeader("Basic token"));
        assertEquals(HttpStatus.UNAUTHORIZED, ex2.getStatusCode());

        ResponseStatusException ex3 = assertThrows(ResponseStatusException.class, () -> authService.getTokenFromHeader("Bearer")); // No token part
        assertEquals(HttpStatus.UNAUTHORIZED, ex3.getStatusCode());
    }

    @Test
    void testValidateTokenAndGetUser_DelegatesToTokenService() {
        User u = User.builder().userId(99L).email("x@y.z").build();
        when(tokenService.validateTokenAndGetUser("tkn")).thenReturn(u);
        User result = authService.validateTokenAndGetUser("tkn");
        assertEquals(u, result);
        verify(tokenService, times(1)).validateTokenAndGetUser("tkn");
    }

    @Test
    void testLogout_DelegatesDeleteToken() {
        assertDoesNotThrow(() -> authService.logout("tkn"));
        verify(tokenService, times(1)).deleteToken("tkn");
    }
}
