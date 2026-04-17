package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingDetailResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.Role;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
 // Hoặc dùng: org.springframework.test.context.bean.override.mockito.MockitoBean
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class) // Chỉ định controller cần test
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // SỬA LỖI: Dùng @MockBean để inject mock vào Spring Context
    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private AuthService authService;

    // (Không cần mock QrCodeService vì BookingController không dùng nó)

    private BookingRequest validBookingRequest;
    private BookingDetailResponse bookingDetailResponse;
    private User mockUser;
    private final String MOCK_TOKEN = "Bearer valid.token";
    private final String RAW_TOKEN = "valid.token";

    @BeforeEach
    void setUp() {
        // Tạo một request hợp lệ
        validBookingRequest = new BookingRequest();
        validBookingRequest.setVehicleId(1L);
        validBookingRequest.setStartTime(LocalDateTime.now().plusDays(1));
        validBookingRequest.setEndTime(LocalDateTime.now().plusDays(2));
        validBookingRequest.setAgreedToTerms(true); // Giả sử DTO có trường này

        // Tạo một response mẫu
        bookingDetailResponse = BookingDetailResponse.builder()
                .bookingId(1L)
                .status(BookingStatus.CONFIRMED)
                .build();

        // Tạo user mẫu
        Role mockRole = Role.builder()
                .roleId(1L)
                .roleName("EV_RENTER")
                .build();

        // Tạo user mẫu VÀ GÁN ROLE
        mockUser = User.builder()
                .userId(1L)
                .role(mockRole)
                .build();

        // Giả lập hành vi mặc định của AuthService (khi token hợp lệ)
        when(authService.getTokenFromHeader(MOCK_TOKEN)).thenReturn(RAW_TOKEN);
        when(authService.validateTokenAndGetUser(RAW_TOKEN)).thenReturn(mockUser);

        // Giả lập hành vi mặc định của AuthService (khi token hợp lệ)
        when(authService.getTokenFromHeader(MOCK_TOKEN)).thenReturn(RAW_TOKEN);
        when(authService.validateTokenAndGetUser(RAW_TOKEN)).thenReturn(mockUser);
    }

    // --- Test 1: createBooking (POST /api/bookings) ---

    @Test
    void testCreateBooking_Success() throws Exception {
        // Giả lập kết quả trả về từ service
        Map<String, Object> serviceResult = Map.of(
                "message", "Yêu cầu đặt xe thành công.",
                "bookingId", 1L,
                "paymentUrl", "http://payment-url.com"
        );
        when(bookingService.createBooking(any(User.class), any(BookingRequest.class)))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest))
                        .header("Authorization", MOCK_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1L))
                .andExpect(jsonPath("$.paymentUrl").value("http://payment-url.com"));

        // Xác minh service được gọi
        verify(bookingService, times(1)).createBooking(eq(mockUser), any(BookingRequest.class));
    }

    @Test
    void testCreateBooking_Fail_AuthGetToken() throws Exception {
        // Giả lập AuthService ném lỗi khi lấy token
        when(authService.getTokenFromHeader(MOCK_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest))
                        .header("Authorization", MOCK_TOKEN))
                .andExpect(status().isUnauthorized()); // Mong đợi lỗi 401

        // Xác minh bookingService không bao giờ được gọi
        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    void testCreateBooking_Fail_AuthValidateUser() throws Exception {
        // Giả lập AuthService ném lỗi khi xác thực user
        when(authService.validateTokenAndGetUser(RAW_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest))
                        .header("Authorization", MOCK_TOKEN))
                .andExpect(status().isUnauthorized()); // Mong đợi lỗi 401

        // Xác minh bookingService không bao giờ được gọi
        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    void testCreateBooking_Fail_ServiceError() throws Exception {
        // Giả lập bookingService ném lỗi (ví dụ: xe không khả dụng)
        when(bookingService.createBooking(any(User.class), any(BookingRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xe không khả dụng"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBookingRequest))
                        .header("Authorization", MOCK_TOKEN))
                .andExpect(status().isBadRequest()); // Mong đợi lỗi 400
    }

    @Test
    void testCreateBooking_Fail_Validation() throws Exception {
        // Tạo một request KHÔNG hợp lệ (vi phạm @Valid)
        // Dựa trên file BookingRequest.java, vehicleId là @NotNull
        BookingRequest invalidRequest = new BookingRequest();
        invalidRequest.setStartTime(LocalDateTime.now().plusDays(1)); // Các trường khác hợp lệ
        invalidRequest.setEndTime(LocalDateTime.now().plusDays(2));
        // Nhưng vehicleId là null (mặc định)

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .header("Authorization", MOCK_TOKEN))
                .andExpect(status().isBadRequest()); // Mong đợi lỗi 400 từ @Valid

        // Xác minh bookingService không bao giờ được gọi
        verify(bookingService, never()).createBooking(any(), any());
    }

    // --- Test 2: getBookingDetails (GET /api/bookings/{bookingId}) ---

    @Test
    void testGetBookingDetails_Success() throws Exception {
        // Giả lập service trả về chi tiết
        when(bookingService.getBookingDetailsById(1L)).thenReturn(bookingDetailResponse);

        mockMvc.perform(get("/api/bookings/1").header("Authorization", MOCK_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));


        verify(bookingService, times(1)).getBookingDetailsById(1L);
    }

    @Test
    void testGetBookingDetails_Fail_NotFound() throws Exception {
        // Giả lập service ném lỗi không tìm thấy
        when(bookingService.getBookingDetailsById(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt xe"));

        mockMvc.perform(get("/api/bookings/999").header("Authorization", MOCK_TOKEN))
                .andExpect(status().isNotFound());

        verify(bookingService, times(1)).getBookingDetailsById(999L);
    }
}