package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
// Bỏ PenaltyFeeRepository vì nó không được inject trực tiếp vào Controller
// import com.fptu.evstation.rental.evrentalsystem.repository.PenaltyFeeRepository;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import com.fptu.evstation.rental.evrentalsystem.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = StaffController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class StaffControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private ContractService contractService;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private UserServiceImpl userService;


    @Autowired
    private ObjectMapper objectMapper;

    private User mockStaff;
    private Station mockStation;
    private Role mockStaffRole; // <-- SỬA LỖI: Thêm biến mock Role
    private String mockAuthHeader;
    private String mockToken;

    @BeforeEach
    void setUp() {
        mockStation = Station.builder().stationId(1L).name("Test Station").build();

        // --- SỬA LỖI: Tạo và gán Role cho mock staff ---
        mockStaffRole = Role.builder().roleId(2L).roleName("STAFF").build();

        mockStaff = User.builder()
                .userId(10L)
                .email("staff@test.com")
                .fullName("Test Staff")
                .station(mockStation) // Gán trạm cho nhân viên
                .role(mockStaffRole)  // <-- SỬA LỖI: Gán Role cho staff
                .build();

        mockAuthHeader = "Bearer mock-token";
        mockToken = "mock-token";

        // Mock 2 hàm auth chính
        when(authService.getTokenFromHeader(mockAuthHeader)).thenReturn(mockToken);
        when(authService.validateTokenAndGetUser(mockToken)).thenReturn(mockStaff);
    }

    // --- /api/staff/bookings ---

    @Test
    void testGetAllBookings_Success() throws Exception {
        List<BookingSummaryResponse> bookings = Arrays.asList(
                BookingSummaryResponse.builder().bookingId(1L).build(),
                BookingSummaryResponse.builder().bookingId(2L).build()
        );
        // Sử dụng eq() để đảm bảo mockStaff được truyền vào
        when(bookingService.getAllBookingsByStation(eq(mockStaff), any(), any(), any()))
                .thenReturn(bookings);

        mockMvc.perform(get("/api/staff/bookings")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(bookingService, times(1)).getAllBookingsByStation(eq(mockStaff), eq(null), eq(null), eq(null));
    }

    @Test
    void testGetAllBookings_WithParams_Success() throws Exception {
        List<BookingSummaryResponse> bookings = Arrays.asList(
                BookingSummaryResponse.builder().bookingId(1L).build()
        );
        when(bookingService.getAllBookingsByStation(eq(mockStaff), eq("test"), eq("CONFIRMED"), eq("2025-11-09")))
                .thenReturn(bookings);

        mockMvc.perform(get("/api/staff/bookings")
                        .param("keyword", "test")
                        .param("status", "CONFIRMED")
                        .param("date", "2025-11-09")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingService, times(1)).getAllBookingsByStation(eq(mockStaff), eq("test"), eq("CONFIRMED"), eq("2025-11-09"));
    }

    @Test
    void testGetAllBookings_Empty() throws Exception {
        when(bookingService.getAllBookingsByStation(eq(mockStaff), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/staff/bookings")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testGetAllBookings_StaffNotAssigned_Forbidden() throws Exception {
        when(bookingService.getAllBookingsByStation(eq(mockStaff), any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào."));

        mockMvc.perform(get("/api/staff/bookings")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllBookings_InvalidToken_Unauthorized() throws Exception {
        // Ghi đè mock chung cho test case này
        when(authService.validateTokenAndGetUser(mockToken))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ"));

        mockMvc.perform(get("/api/staff/bookings")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isUnauthorized());
    }

    // --- /api/staff/refund-requests --- (NEW)

    @Test
    void testGetRefundRequests_Success() throws Exception {
        List<BookingSummaryResponse> refunds = Arrays.asList(
                BookingSummaryResponse.builder().bookingId(1L).refundAmount(500000.0).build()
        );
        when(bookingService.getPendingRefundsByStation(eq(mockStaff))).thenReturn(refunds);

        mockMvc.perform(get("/api/staff/refund-requests")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingService, times(1)).getPendingRefundsByStation(eq(mockStaff));
    }

    @Test
    void testGetRefundRequests_StaffForbidden_Forbidden() throws Exception {
        when(bookingService.getPendingRefundsByStation(eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa."));

        mockMvc.perform(get("/api/staff/refund-requests")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    // --- /api/staff/bookings/{bookingId}/confirm-refund --- (NEW)

    @Test
    void testConfirmRefund_Success() throws Exception {
        doNothing().when(bookingService).confirmRefund(eq(mockStaff), eq(1L));

        mockMvc.perform(post("/api/staff/bookings/1/confirm-refund")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã xác nhận hoàn tiền thành công. Khách hàng sẽ thấy trạng thái 'Đã hoàn tiền'."));

        verify(bookingService, times(1)).confirmRefund(eq(mockStaff), eq(1L));
    }

    @Test
    void testConfirmRefund_WrongState_BadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking này không ở trạng thái chờ hoàn tiền."))
                .when(bookingService).confirmRefund(eq(mockStaff), eq(1L));

        mockMvc.perform(post("/api/staff/bookings/1/confirm-refund")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/bookings/{bookingId}/confirm-deposit --- (NEW)

    @Test
    void testConfirmDeposit_Success() throws Exception {
        doNothing().when(paymentService).confirmDeposit(eq(mockStaff), eq(1L));

        mockMvc.perform(post("/api/staff/bookings/1/confirm-deposit")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xác nhận cọc 500k thành công. Xe đã được giữ chỗ."));

        verify(paymentService, times(1)).confirmDeposit(eq(mockStaff), eq(1L));
    }

    @Test
    void testConfirmDeposit_AlreadyConfirmed_BadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn này đã được xác nhận cọc trước đó (có thể qua PayOS)."))
                .when(paymentService).confirmDeposit(eq(mockStaff), eq(1L));

        mockMvc.perform(post("/api/staff/bookings/1/confirm-deposit")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/rentals/initiate-check-in/{bookingId} ---

    @Test
    void testInitiateCheckIn_Success() throws Exception {
        Map<String, Object> response = Map.of("message", "Check-in initiated", "paymentUrl", "http://pay.os");
        when(bookingService.initiateCheckIn(eq(1L), eq(mockStaff))).thenReturn(response);

        mockMvc.perform(post("/api/staff/rentals/initiate-check-in/1")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Check-in initiated"));

        verify(bookingService, times(1)).initiateCheckIn(eq(1L), eq(mockStaff));
    }

    @Test
    void testInitiateCheckIn_WrongState_BadRequest() throws Exception {
        when(bookingService.initiateCheckIn(eq(1L), eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking này không ở trạng thái sẵn sàng để check-in."));

        mockMvc.perform(post("/api/staff/rentals/initiate-check-in/1")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/rentals/check-in/{bookingId} ---

    @Test
    void testCheckIn_Success() throws Exception {
        Contract contract = Contract.builder().contractPdfPath("/path/to/contract.pdf").build();
        when(bookingService.processCheckIn(eq(1L), any(CheckInRequest.class), eq(mockStaff))).thenReturn(contract);

        MockMultipartFile checkInPhoto = new MockMultipartFile("checkInPhotos", "photo.jpg", "image/jpeg", "photo-content".getBytes());

        mockMvc.perform(multipart("/api/staff/rentals/check-in/1")
                        .file(checkInPhoto)
                        .param("depositPaymentMethod", "CASH")
                        .param("conditionBefore", "GOOD")
                        .param("battery", "90.5")
                        .param("mileage", "1000.0")
                        .header("Authorization", mockAuthHeader)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Check-in thành công. Đã thu cọc thuê xe và tạo hợp đồng."))
                .andExpect(jsonPath("$.contractUrl").value("/path/to/contract.pdf"));

        verify(bookingService, times(1)).processCheckIn(eq(1L), any(CheckInRequest.class), eq(mockStaff));
    }

    @Test
    void testCheckIn_InvalidRequest_BadRequest() throws Exception {
        // Test @Valid: Thiếu 'conditionBefore'
        MockMultipartFile checkInPhoto = new MockMultipartFile("checkInPhotos", "photo.jpg", "image/jpeg", "photo-content".getBytes());

        mockMvc.perform(multipart("/api/staff/rentals/check-in/1")
                        .file(checkInPhoto)
                        .param("depositPaymentMethod", "CASH")
                        // .param("conditionBefore", "GOOD") // Cố ý thiếu
                        .param("battery", "90.5")
                        .param("mileage", "1000.0")
                        .header("Authorization", mockAuthHeader)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest()); // Mong đợi 400 Bad Request do validation
    }

    @Test
    void testCheckIn_ServiceError_BadRequest() throws Exception {
        when(bookingService.processCheckIn(eq(1L), any(CheckInRequest.class), eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh check-in (lúc giao xe) là bắt buộc."));

        MockMultipartFile checkInPhoto = new MockMultipartFile("checkInPhotos", "photo.jpg", "image/jpeg", "photo-content".getBytes());

        mockMvc.perform(multipart("/api/staff/rentals/check-in/1")
                        .file(checkInPhoto)
                        .param("depositPaymentMethod", "CASH")
                        .param("conditionBefore", "GOOD")
                        .param("battery", "90.5")
                        .param("mileage", "1000.0")
                        .header("Authorization", mockAuthHeader)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/contracts ---

    @Test
    void testGetAllContracts_Success() throws Exception {
        List<ContractSummaryResponse> contracts = Arrays.asList(
                ContractSummaryResponse.builder().contractId(1L).build(),
                ContractSummaryResponse.builder().contractId(2L).build()
        );
        when(contractService.getAllContractsByStation(eq(mockStaff)))
                .thenReturn(contracts);

        mockMvc.perform(get("/api/staff/contracts")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(contractService, times(1)).getAllContractsByStation(eq(mockStaff));
    }

    @Test
    void testGetAllContracts_StaffNotAssigned_Forbidden() throws Exception {
        when(contractService.getAllContractsByStation(eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào."));

        mockMvc.perform(get("/api/staff/contracts")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    // --- /api/staff/verifications/pending ---

    @Test
    void testGetPendingVerifications_Success() throws Exception {
        List<User> users = Arrays.asList(User.builder().userId(1L).build(), User.builder().userId(2L).build());
        when(userService.getPendingVerifications()).thenReturn(users);

        mockMvc.perform(get("/api/staff/verifications/pending")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(userService, times(1)).getPendingVerifications();
    }

    // --- /api/staff/verifications/{userId}/process ---

    @Test
    void testVerifyUser_Approve_Success() throws Exception {
        VerifyRequest request = VerifyRequest.builder().approved(true).build();
        when(userService.processVerification(eq(1L), any(VerifyRequest.class))).thenReturn("Xác minh người dùng thành công.");

        mockMvc.perform(post("/api/staff/verifications/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xác minh người dùng thành công."));

        verify(userService, times(1)).processVerification(eq(1L), any(VerifyRequest.class));
    }

    @Test
    void testVerifyUser_Reject_Success() throws Exception {
        VerifyRequest request = VerifyRequest.builder().approved(false).reason("Ảnh mờ").build();
        when(userService.processVerification(eq(1L), any(VerifyRequest.class))).thenReturn("Đã từ chối yêu cầu xác minh. Lý do: Ảnh mờ");

        mockMvc.perform(post("/api/staff/verifications/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã từ chối yêu cầu xác minh. Lý do: Ảnh mờ"));
    }

    @Test
    void testVerifyUser_RejectNoReason_BadRequest() throws Exception {
        // Test logic trong UserServiceImpl
        VerifyRequest request = VerifyRequest.builder().approved(false).reason("").build();
        when(userService.processVerification(eq(1L), any(VerifyRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải nhập lý do khi từ chối"));

        mockMvc.perform(post("/api/staff/verifications/1/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/bookings/{bookingId}/cancel ---

    @Test
    void testCancelBooking_Success() throws Exception {
        Booking booking = Booking.builder().bookingId(1L).station(mockStation).build();
        when(bookingService.getBookingById(1L)).thenReturn(booking);
        doNothing().when(bookingService).cancelBookingByStaff(eq(1L), eq(mockStaff));

        mockMvc.perform(post("/api/staff/bookings/1/cancel")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nhân viên đã hủy booking thành công."));

        verify(bookingService, times(1)).getBookingById(1L);
        verify(bookingService, times(1)).cancelBookingByStaff(eq(1L), eq(mockStaff));
    }

    @Test
    void testCancelBooking_DifferentStation_Forbidden() throws Exception {
        // Test logic if(...) trong controller
        Station otherStation = Station.builder().stationId(2L).build();
        Booking booking = Booking.builder().bookingId(1L).station(otherStation).build();

        when(bookingService.getBookingById(1L)).thenReturn(booking);

        mockMvc.perform(post("/api/staff/bookings/1/cancel")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden()); // 403 Forbidden

        verify(bookingService, times(1)).getBookingById(1L);
        verify(bookingService, times(0)).cancelBookingByStaff(anyLong(), any(User.class)); // Không được gọi
    }

    @Test
    void testCancelBooking_WrongState_BadRequest() throws Exception {
        Booking booking = Booking.builder().bookingId(1L).station(mockStation).build();
        when(bookingService.getBookingById(1L)).thenReturn(booking);

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy booking ở trạng thái COMPLETED"))
                .when(bookingService).cancelBookingByStaff(eq(1L), eq(mockStaff));

        mockMvc.perform(post("/api/staff/bookings/1/cancel")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/bookings/{bookingId}/calculate-bill ---

    @Test
    void testCalculateFinalBill_Success_NoFees() throws Exception {
        BillResponse billResponse = BillResponse.builder().bookingId(1L).paymentDue(100000.0).build();
        when(paymentService.calculateFinalBill(eq(mockStaff), eq(1L), any(PenaltyCalculationRequest.class))).thenReturn(billResponse);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/calculate-bill")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(1L))
                .andExpect(jsonPath("$.paymentDue").value(100000.0));
    }

    @Test
    void testCalculateFinalBill_WithSelectedFeesJson_Success() throws Exception {
        BillResponse billResponse = BillResponse.builder().bookingId(1L).totalPenaltyFee(50000.0).build();
        when(paymentService.calculateFinalBill(eq(mockStaff), eq(1L), any(PenaltyCalculationRequest.class))).thenReturn(billResponse);

        List<PenaltyCalculationRequest.SelectedFee> fees = List.of(new PenaltyCalculationRequest.SelectedFee(1L, 1));
        String selectedFeesJson = objectMapper.writeValueAsString(fees);

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/calculate-bill")
                        .part(new MockPart("selectedFeesJson", selectedFeesJson.getBytes())) // Sử dụng MockPart
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPenaltyFee").value(50000.0));
    }

    @Test
    void testCalculateFinalBill_InvalidSelectedFeesJson_BadRequest() throws Exception {
        // Test logic try-catch trong controller
        String invalidJson = "[{\"feeId\": 1, \"quantity\": \"one\"}]"; // "one" không phải là số

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/calculate-bill")
                        .part(new MockPart("selectedFeesJson", invalidJson.getBytes()))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Định dạng của selectedFees không hợp lệ."));
    }

    @Test
    void testCalculateFinalBill_WithCustomFeeAndPhotos_Success() throws Exception {
        BillResponse billResponse = BillResponse.builder().bookingId(1L).totalPenaltyFee(150000.0).build();
        when(paymentService.calculateFinalBill(eq(mockStaff), eq(1L), any(PenaltyCalculationRequest.class))).thenReturn(billResponse);

        MockMultipartFile customPhoto = new MockMultipartFile("customFee.photoFiles", "damage.jpg", "image/jpeg", "damage-photo".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/calculate-bill")
                        .file(customPhoto)
                        .param("customFee.feeName", "Vỡ gương")
                        .param("customFee.description", "Vỡ gương chiếu hậu")
                        .param("customFee.amount", "150000")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPenaltyFee").value(150000.0));
    }

    @Test
    void testCalculateFinalBill_WrongState_BadRequest() throws Exception {
        when(paymentService.calculateFinalBill(eq(mockStaff), eq(1L), any(PenaltyCalculationRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể tính phí khi đơn đang ở trạng thái RENTING."));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/calculate-bill")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/bookings/{bookingId}/confirm-payment ---

    @Test
    void testConfirmPayment_Success() throws Exception {
        Map<String, Object> response = Map.of("message", "Payment confirmed");
        when(paymentService.confirmFinalPayment(eq(1L), any(PaymentConfirmationRequest.class), eq(mockStaff))).thenReturn(response);

        MockMultipartFile confirmPhoto = new MockMultipartFile("confirmPhotos", "photo.jpg", "image/jpeg", "photo-content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/confirm-payment")
                        .file(confirmPhoto)
                        .param("paymentMethod", "CASH")
                        .param("conditionAfter", "GOOD, trầy xước nhẹ")
                        .param("battery", "80.0")
                        .param("mileage", "1200.0")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment confirmed"));

        verify(paymentService, times(1)).confirmFinalPayment(eq(1L), any(PaymentConfirmationRequest.class), eq(mockStaff));
    }

    @Test
    void testConfirmPayment_InvalidRequest_BadRequest() throws Exception {
        // Test @Valid: Thiếu 'conditionAfter'
        MockMultipartFile confirmPhoto = new MockMultipartFile("confirmPhotos", "photo.jpg", "image/jpeg", "photo-content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/confirm-payment")
                        .file(confirmPhoto)
                        .param("paymentMethod", "CASH")
                        // .param("conditionAfter", "GOOD") // Cố ý thiếu
                        .param("battery", "80.0")
                        .param("mileage", "1200.0")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConfirmPayment_ServiceError_BadRequest() throws Exception {
        when(paymentService.confirmFinalPayment(eq(1L), any(PaymentConfirmationRequest.class), eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh check-out (lúc trả xe) là bắt buộc."));

        // Gửi request hợp lệ, nhưng service sẽ ném lỗi
        MockMultipartFile confirmPhoto = new MockMultipartFile("confirmPhotos", "photo.jpg", "image/jpeg", "photo-content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/bookings/1/confirm-payment")
                        .file(confirmPhoto)
                        .param("paymentMethod", "CASH")
                        .param("conditionAfter", "GOOD")
                        .param("battery", "80.0")
                        .param("mileage", "1200.0")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    // --- /api/staff/penalty-fees ---

    @Test
    void testGetAllPenaltyFees_Success() throws Exception {
        List<PenaltyFee> fees = Arrays.asList(new PenaltyFee(), new PenaltyFee());
        when(paymentService.getAllPenaltyFees()).thenReturn(fees);

        mockMvc.perform(get("/api/staff/penalty-fees")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(paymentService, times(1)).getAllPenaltyFees();
    }

    // --- /api/staff/invoices ---

    @Test
    void testGetAllInvoices_Success() throws Exception {
        List<InvoiceSummaryResponse> invoices = Arrays.asList(
                InvoiceSummaryResponse.builder().bookingId(1L).build()
        );
        when(invoiceService.getAllInvoicesByStation(eq(mockStaff))).thenReturn(invoices);

        mockMvc.perform(get("/api/staff/invoices")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(invoiceService, times(1)).getAllInvoicesByStation(eq(mockStaff));
    }

    @Test
    void testGetAllInvoices_StaffNotAssigned_Forbidden() throws Exception {
        when(invoiceService.getAllInvoicesByStation(eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào."));

        mockMvc.perform(get("/api/staff/invoices")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    // --- /api/staff/my-station/vehicles ---

    @Test
    void testGetVehiclesForStaffStation_Success() throws Exception {
        List<VehicleResponse> vehicles = Arrays.asList(new VehicleResponse(), new VehicleResponse());
        when(vehicleService.getAllVehiclesByStation(eq(mockStation))).thenReturn(vehicles);

        mockMvc.perform(get("/api/staff/my-station/vehicles")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(vehicleService, times(1)).getAllVehiclesByStation(eq(mockStation));
    }

    @Test
    void testGetVehiclesForStaffStation_StaffNotAssigned_Forbidden() throws Exception {
        // Test logic if(staff.getStation() == null) trong controller
        User staffNoStation = User.builder().userId(11L).station(null).role(mockStaffRole).build(); // Sửa: Thêm role
        when(authService.validateTokenAndGetUser(mockToken)).thenReturn(staffNoStation); // Ghi đè mock

        mockMvc.perform(get("/api/staff/my-station/vehicles")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Bạn chưa được gán cho trạm nào."));

        verify(vehicleService, times(0)).getAllVehiclesByStation(any(Station.class)); // Không được gọi
    }

    // --- /api/staff/vehicles/{vehicleId}/details ---

    @Test
    void testUpdateVehicleDetails_Success() throws Exception {
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest();
        request.setBatteryLevel(90);

        when(vehicleService.updateVehicleDetails(eq(mockStaff), eq(1L), any(UpdateVehicleDetailsRequest.class))).thenReturn(new Vehicle());

        mockMvc.perform(put("/api/staff/vehicles/1/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin xe thành công."));

        verify(vehicleService, times(1)).updateVehicleDetails(eq(mockStaff), eq(1L), any(UpdateVehicleDetailsRequest.class));
    }

    @Test
    void testUpdateVehicleDetails_InvalidRequest_BadRequest() throws Exception {
        // Test @Valid: batteryLevel > 100
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest();
        request.setBatteryLevel(101); // Không hợp lệ

        mockMvc.perform(put("/api/staff/vehicles/1/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateVehicleDetails_ServiceError_Forbidden() throws Exception {
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest();
        request.setBatteryLevel(90);

        when(vehicleService.updateVehicleDetails(eq(mockStaff), eq(1L), any(UpdateVehicleDetailsRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật thông tin xe ở trạm khác."));

        mockMvc.perform(put("/api/staff/vehicles/1/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    // --- /api/staff/vehicles/{vehicleId}/report-damage ---

    @Test
    void testReportMajorDamage_Success() throws Exception {
        when(vehicleService.reportMajorDamage(eq(mockStaff), eq(1L), any(ReportDamageRequest.class))).thenReturn(new Vehicle());

        MockMultipartFile photo = new MockMultipartFile("photos", "damage.jpg", "image/jpeg", "damage-photo".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/vehicles/1/report-damage")
                        .file(photo)
                        .param("description", "Test damage")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Báo cáo hư hỏng đã được ghi nhận. Xe đã được chuyển vào trạng thái bảo trì."));

        verify(vehicleService, times(1)).reportMajorDamage(eq(mockStaff), eq(1L), any(ReportDamageRequest.class));
    }

    @Test
    void testReportMajorDamage_InvalidRequest_BadRequest() throws Exception {
        // Test @Valid: description trống
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/vehicles/1/report-damage")
                        .param("description", "") // Cố ý để trống
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testReportMajorDamage_ServiceError_Forbidden() throws Exception {
        when(vehicleService.reportMajorDamage(eq(mockStaff), eq(1L), any(ReportDamageRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa."));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/staff/vehicles/1/report-damage")
                        .param("description", "Test damage")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }

    // --- /api/staff/dashboard ---

    @Test
    void testGetDashboardSummary_Success() throws Exception {
        DashboardSummaryDto summary = DashboardSummaryDto.builder().stationName("Test Station").totalVehicles(10L).build();
        when(dashboardService.getSummaryForStaff(eq(mockStaff))).thenReturn(summary);

        mockMvc.perform(get("/api/staff/dashboard")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationName").value("Test Station"))
                .andExpect(jsonPath("$.totalVehicles").value(10));

        verify(dashboardService, times(1)).getSummaryForStaff(eq(mockStaff));
    }

    @Test
    void testGetDashboardSummary_StaffNotAssigned_Forbidden() throws Exception {
        // Test logic trong DashboardServiceImpl
        when(dashboardService.getSummaryForStaff(eq(mockStaff)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên này chưa được gán cho trạm nào."));

        mockMvc.perform(get("/api/staff/dashboard")
                        .header("Authorization", mockAuthHeader))
                .andExpect(status().isForbidden());
    }
}