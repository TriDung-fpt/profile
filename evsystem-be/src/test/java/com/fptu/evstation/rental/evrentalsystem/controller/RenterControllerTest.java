package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.CancelBookingRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RenterController.class)
@AutoConfigureMockMvc(addFilters = false)// Chỉ định rõ RenterController
class RenterControllerTest {

    @Autowired
    private MockMvc mockMvc; // Dùng để gọi API

    @Autowired
    private ObjectMapper objectMapper; // Dùng để chuyển đổi Java Object <-> JSON

    // SỬA LỖI: Dùng @MockBean để mock service trong Spring Context
    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private AuthService authService;

    // --- Biến dùng chung cho các test case ---
    private User mockRenter;
    private User anotherUser;
    private Booking mockBooking;
    private Booking otherBooking;
    private CancelBookingRequest cancelRequest;
    private final String MOCK_TOKEN_HEADER = "Bearer valid.token";
    private final String RAW_TOKEN = "valid.token";

    @BeforeEach
    void setUp() {
        // --- Setup User ---
        mockRenter = User.builder().userId(1L).fullName("Test Renter").build();
        anotherUser = User.builder().userId(2L).fullName("Another User").build();

        // --- Setup Booking ---
        // Booking (ID 10) của user 1 (mockRenter)
        mockBooking = Booking.builder().bookingId(10L).user(mockRenter).build();
        // Booking (ID 11) của user 2 (anotherUser)
        otherBooking = Booking.builder().bookingId(11L).user(anotherUser).build();

        // --- Setup Request DTO ---
        cancelRequest = new CancelBookingRequest();
        cancelRequest.setBankName("Test Bank");
        cancelRequest.setAccountNumber("123456");
        cancelRequest.setAccountName("Test Renter");

        // --- Giả lập AuthService luôn thành công (happy path) ---
        // Các test case lỗi sẽ override hành vi này
        when(authService.getTokenFromHeader(MOCK_TOKEN_HEADER)).thenReturn(RAW_TOKEN);
        when(authService.validateTokenAndGetUser(RAW_TOKEN)).thenReturn(mockRenter);
    }

    // --- Tests for GET /api/renter/my-bookings ---

    @Test
    void testGetMyBookings_Success() throws Exception {
        // 1. Chuẩn bị
        List<BookingSummaryResponse> bookings = Collections.singletonList(
                BookingSummaryResponse.builder().bookingId(1L).build()
        );
        when(bookingService.getMyBookings(mockRenter)).thenReturn(bookings);

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(get("/api/renter/my-bookings")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingId").value(1L));

        // Xác minh service được gọi đúng 1 lần
        verify(bookingService, times(1)).getMyBookings(mockRenter);
    }

    @Test
    void testGetMyBookings_Fail_AuthError() throws Exception {
        // 1. Chuẩn bị (Override Auth Service để ném lỗi)
        when(authService.validateTokenAndGetUser(RAW_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ"));

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(get("/api/renter/my-bookings")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isUnauthorized()); // Mong đợi 401

        // Xác minh service không bao giờ được gọi
        verify(bookingService, never()).getMyBookings(any(User.class));
    }

    // --- Tests for POST /api/renter/bookings/{bookingId}/cancel ---

    @Test
    void testCancelMyBooking_Success_WithBody() throws Exception {
        // 1. Chuẩn bị
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking); // Trả về booking của user 1
        when(bookingService.cancelBookingByRenter(eq(mockRenter), eq(10L), any(CancelBookingRequest.class)))
                .thenReturn("Hủy thành công và chờ hoàn tiền");

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(post("/api/renter/bookings/10/cancel")
                        .header("Authorization", MOCK_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest))) // Gửi kèm body
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hủy thành công và chờ hoàn tiền"));

        // Xác minh các service được gọi
        verify(bookingService, times(1)).getBookingById(10L);
        verify(bookingService, times(1)).cancelBookingByRenter(eq(mockRenter), eq(10L), any(CancelBookingRequest.class));
    }

    @Test
    void testCancelMyBooking_Success_NoBody() throws Exception {
        // 1. Chuẩn bị
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking); // Trả về booking của user 1
        when(bookingService.cancelBookingByRenter(mockRenter, 10L, null)) // RequestBody là null
                .thenReturn("Hủy thành công");

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(post("/api/renter/bookings/10/cancel")
                        .header("Authorization", MOCK_TOKEN_HEADER)) // Không gửi kèm body
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hủy thành công"));

        verify(bookingService, times(1)).getBookingById(10L);
        verify(bookingService, times(1)).cancelBookingByRenter(mockRenter, 10L, null);
    }

    @Test
    void testCancelMyBooking_Fail_AuthError() throws Exception {
        // 1. Chuẩn bị
        when(authService.validateTokenAndGetUser(RAW_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ"));

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(post("/api/renter/bookings/10/cancel")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isUnauthorized()); // Mong đợi 401

        // Xác minh các service nghiệp vụ không được gọi
        verify(bookingService, never()).getBookingById(anyLong());
        verify(bookingService, never()).cancelBookingByRenter(any(), anyLong(), any());
    }

    @Test
    void testCancelMyBooking_Fail_PermissionDenied() throws Exception {
        // 1. Chuẩn bị
        // User 1 (mockRenter) đã login, nhưng cố gắng hủy booking 11 (của user 2)
        when(bookingService.getBookingById(11L)).thenReturn(otherBooking); // Trả về booking của user 2

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(post("/api/renter/bookings/11/cancel")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isForbidden()); // Mong đợi 403

        // Xác minh đã gọi getBookingById, nhưng không bao giờ gọi cancelBookingByRenter
        verify(bookingService, times(1)).getBookingById(11L);
        verify(bookingService, never()).cancelBookingByRenter(any(), anyLong(), any());
    }

    @Test
    void testCancelMyBooking_Fail_BookingNotFound() throws Exception {
        // 1. Chuẩn bị
        when(bookingService.getBookingById(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy booking"));

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(post("/api/renter/bookings/99/cancel")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isNotFound()); // Mong đợi 404

        verify(bookingService, times(1)).getBookingById(99L);
        verify(bookingService, never()).cancelBookingByRenter(any(), anyLong(), any());
    }

    @Test
    void testCancelMyBooking_Fail_ServiceError() throws Exception {
        // 1. Chuẩn bị
        when(bookingService.getBookingById(10L)).thenReturn(mockBooking); // Trả về booking của user 1
        // Giả lập service ném lỗi nghiệp vụ (ví dụ: không thể hủy)
        when(bookingService.cancelBookingByRenter(mockRenter, 10L, null))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đã quá trễ để hủy"));

        // 2. Thực thi & 3. Xác minh
        mockMvc.perform(post("/api/renter/bookings/10/cancel")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isBadRequest()); // Mong đợi 400

        verify(bookingService, times(1)).getBookingById(10L);
        verify(bookingService, times(1)).cancelBookingByRenter(mockRenter, 10L, null);
    }
}