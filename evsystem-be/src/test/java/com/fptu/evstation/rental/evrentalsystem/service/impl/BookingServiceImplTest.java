package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingDetailResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.CancelBookingRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.CheckInRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.ContractRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class BookingServiceImplTest {

    @MockitoBean
    private BookingRepository bookingRepository;
    @MockitoBean
    private VehicleRepository vehicleRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private VehicleService vehicleService;
    @MockitoBean
    private StationService stationService;
    @MockitoBean
    private ContractRepository contractRepository;
    @MockitoBean
    private ContractService contractService;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Sửa lỗi: Mock PayOS
    @MockitoBean
    private PayOS payOS;

    @Mock
    private PaymentRequestsService paymentRequestsService;
    @Mock
    private CreatePaymentLinkResponse paymentResponseMock;

    @Autowired
    private BookingServiceImpl bookingService;

    // --- Dữ liệu Mock ---
    private User mockRenter;
    private User mockStaff;
    private Station mockStation;
    private Model mockModel;
    private Vehicle mockVehicle;
    private BookingRequest mockRequest;
    private Booking mockBooking_Pending;
    private Booking mockBooking_Confirmed;
    private Booking mockBooking_Renting;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Mock cho client con của PayOS

    @BeforeEach
    void setUp() {
        mockRenter = User.builder()
                .userId(1L)
                .email("renter@test.com")
                .status(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.APPROVED)
                .cancellationCount(0)
                .build();

        mockStation = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .status(StationStatus.ACTIVE)
                .build();

        mockModel = Model.builder()
                .modelId(1L)
                .modelName("VinFast VF 3")
                .pricePerHour(100000.0) // 100k/giờ
                .initialValue(20000000.0) // 20 triệu
                .build();

        mockVehicle = Vehicle.builder()
                .vehicleId(101L)
                .status(VehicleStatus.AVAILABLE)
                .model(mockModel)
                .station(mockStation)
                .condition(VehicleCondition.GOOD)
                .build();

        mockStaff = User.builder()
                .userId(10L)
                .fullName("Test Staff")
                .status(AccountStatus.ACTIVE)
                .station(mockStation)
                .build();

        startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        endTime = startTime.plusHours(8);

        mockRequest = BookingRequest.builder()
                .vehicleId(101L)
                .startTime(startTime)
                .endTime(endTime)
                .agreedToTerms(true)
                .build();

        mockBooking_Pending = Booking.builder()
                .bookingId(98L)
                .status(BookingStatus.PENDING)
                .station(mockStation)
                .vehicle(mockVehicle)
                .user(mockRenter)
                .startDate(startTime)
                .endDate(endTime)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        mockBooking_Confirmed = Booking.builder()
                .bookingId(99L)
                .status(BookingStatus.CONFIRMED)
                .station(mockStation)
                .vehicle(mockVehicle)
                .user(mockRenter)
                .startDate(startTime)
                .endDate(endTime)
                .reservationDepositPaid(true)
                .createdAt(LocalDateTime.now().minusHours(1))
                .vehicle(mockVehicle)
                .build();

        mockBooking_Renting = Booking.builder()
                .bookingId(100L)
                .status(BookingStatus.RENTING)
                .station(mockStation)
                .vehicle(mockVehicle)
                .user(mockRenter)
                .startDate(LocalDateTime.now().minusHours(2))
                .endDate(endTime)
                .reservationDepositPaid(true)
                .rentalDepositPaid(true)
                .rentalDeposit(400000.0)
                .build();

        try {
            when(payOS.paymentRequests()).thenReturn(paymentRequestsService);

            when(paymentRequestsService.create(any(CreatePaymentLinkRequest.class)))
                    .thenReturn(paymentResponseMock);

            when(paymentResponseMock.getCheckoutUrl()).thenReturn("http://mock-payment-url.com");

        } catch (PayOSException e) {
        }
    }

    // --- Tests for createBooking ---

    @Test
    void testCreateBooking_Success_HappyPath() throws PayOSException {
        // ARRANGE
        // (Mock PayOS thành công đã được setup trong @BeforeEach)
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);
        when(vehicleRepository.countByModelAndStation(any(Model.class), any(Station.class))).thenReturn(1L);
        when(bookingRepository.countOverlappingBookingsForVehicle(eq(mockVehicle), eq(startTime), eq(endTime), anyList())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking savedBooking = invocation.getArgument(0);
            savedBooking.setBookingId(99L);
            return savedBooking;
        });

        // ACT
        Map<String, Object> result = bookingService.createBooking(mockRenter, mockRequest);

        // ASSERT
        assertNotNull(result);
        assertEquals(99L, result.get("bookingId"));
        assertEquals("http://mock-payment-url.com", result.get("paymentUrl"));
        // 100k/giờ * 8 giờ = 800k
        verify(bookingRepository, times(1)).save(argThat(
                booking -> booking.getFinalFee() == 800000.0 && booking.getStatus() == BookingStatus.PENDING
        ));
        verify(paymentRequestsService, times(1)).create(any(CreatePaymentLinkRequest.class));
    }

    @Test
    void testCreateBooking_Fail_AccountLocked() {
        mockRenter.setStatus(AccountStatus.INACTIVE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tài khoản của bạn đã bị khóa"));
    }

    @Test
    void testCreateBooking_Fail_DidNotAgreeToTerms() {
        mockRequest.setAgreedToTerms(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn phải đồng ý với các điều khoản"));
    }

    @Test
    void testCreateBooking_Fail_StartTimeInPast() {
        mockRequest.setStartTime(LocalDateTime.now().minusHours(1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn chỉ có thể đặt xe trong vòng 2 ngày tới"));
    }

    @Test
    void testCreateBooking_Fail_StartTimeTooFar() {
        LocalDateTime newStartTime = LocalDateTime.now().plusDays(3);
        mockRequest.setStartTime(newStartTime);
        mockRequest.setEndTime(newStartTime.plusHours(8)); // Ensure endTime is after startTime

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn chỉ có thể đặt xe trong vòng 2 ngày tới"));
    }

    @Test
    void testCreateBooking_Fail_EndTimeBeforeStartTime() {
        mockRequest.setEndTime(startTime.minusHours(1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Thời gian trả xe phải sau thời gian nhận xe"));
    }

    @Test
    void testCreateBooking_Fail_DurationTooShort() {
        mockRequest.setEndTime(startTime.plusMinutes(59));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Thời gian thuê tối thiểu là 1 giờ"));
    }

    @Test
    void testCreateBooking_Fail_AccountNotVerified() {
        mockRenter.setVerificationStatus(VerificationStatus.PENDING);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tài khoản của bạn chưa được xác minh"));
    }

    @Test
    void testCreateBooking_Fail_AlreadyHasActiveBooking() {
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn đã có một đơn đặt xe đang hoạt động"));
    }

    @Test
    void testCreateBooking_Fail_StationInactive() {
        mockStation.setStatus(StationStatus.INACTIVE);
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("hiện không hoạt động"));
    }

    @Test
    void testCreateBooking_Fail_VehicleNotAvailable() {
        mockVehicle.setStatus(VehicleStatus.RENTED);
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Xe không khả dụng hoặc đã được thuê"));
    }

    @Test
    void testCreateBooking_Fail_VehicleNotFound() {
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });
    }

    @Test
    void testCreateBooking_Fail_NoVehiclesOfModelInStation() {
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);
        when(vehicleRepository.countByModelAndStation(any(Model.class), any(Station.class))).thenReturn(0L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Trạm này không có loại xe bạn chọn"));
    }

    @Test
    void testCreateBooking_Fail_AllVehiclesBooked_Overlapping() {
        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);
        when(vehicleRepository.countByModelAndStation(any(Model.class), any(Station.class))).thenReturn(1L);
        when(bookingRepository.countOverlappingBookingsForVehicle(eq(mockVehicle), eq(startTime), eq(endTime), anyList())).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Xe này đã bị đặt trong khung giờ bạn chọn"));
    }

    @Test
    void testCreateBooking_Fail_PayOSException() throws PayOSException {
        when(paymentRequestsService.create(any(CreatePaymentLinkRequest.class)))
                .thenThrow(new PayOSException("Lỗi PayOS 500k", new RuntimeException("Mock internal error")));

        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);
        when(vehicleRepository.countByModelAndStation(any(Model.class), any(Station.class))).thenReturn(1L);
        when(bookingRepository.countOverlappingBookingsForVehicle(eq(mockVehicle), eq(startTime), eq(endTime), anyList())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking_Pending);

        // ACT & ASSERT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể tạo link thanh toán"));
    }

    @Test
    void testCreateBooking_Fail_GeneralException() throws PayOSException {
        // ARRANGE
        // Ghi đè mock, giả lập NÉM LỖI
        when(payOS.paymentRequests().create(any(CreatePaymentLinkRequest.class)))
                .thenThrow(new RuntimeException("Lỗi hệ thống"));

        when(bookingRepository.countByUserAndStatusIn(eq(mockRenter), anyList())).thenReturn(0L);
        when(vehicleService.getVehicleById(101L)).thenReturn(mockVehicle);
        when(vehicleRepository.countByModelAndStation(any(Model.class), any(Station.class))).thenReturn(1L);
        when(bookingRepository.countOverlappingBookingsForVehicle(eq(mockVehicle), eq(startTime), eq(endTime), anyList())).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking_Pending);

        // ACT & ASSERT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.createBooking(mockRenter, mockRequest);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Lỗi hệ thống không xác định"));
    }

    // --- Tests for getBookingById ---

    @Test
    void testGetBookingById_Success() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        Booking result = bookingService.getBookingById(99L);
        assertNotNull(result);
        assertEquals(99L, result.getBookingId());
    }

    @Test
    void testGetBookingById_Fail_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.getBookingById(1L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- Tests for getBookingDetailsById ---

    @Test
    void testGetBookingDetailsById_Success_Full() throws JsonProcessingException {
        Contract mockContract = Contract.builder().contractPdfPath("/path/contract.pdf").build();
        when(contractRepository.findByBooking(mockBooking_Renting)).thenReturn(Optional.of(mockContract));

        List<String> mockPhotos = List.of("/path/photo1.jpg");
        String mockPhotosJson = "[\"/path/photo1.jpg\"]";
        mockBooking_Renting.setCheckInPhotoPaths(mockPhotosJson);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting));

        BookingDetailResponse result = bookingService.getBookingDetailsById(100L);

        assertEquals(100L, result.getBookingId());
        assertEquals(mockRenter.getFullName(), result.getRenterName());
        assertEquals(mockVehicle.getLicensePlate(), result.getVehicleLicensePlate());
        assertEquals("/path/contract.pdf", result.getContractPdfPath());
        assertEquals(1, result.getCheckInPhotoPaths().size());
        assertEquals("/path/photo1.jpg", result.getCheckInPhotoPaths().get(0));
        assertEquals(mockModel.getModelName(), result.getModelName());
    }

    @Test
    void testGetBookingDetailsById_Success_NoVehicle() {
        mockBooking_Pending.setVehicle(null);
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));
        when(contractRepository.findByBooking(mockBooking_Pending)).thenReturn(Optional.empty());

        BookingDetailResponse result = bookingService.getBookingDetailsById(98L);

        assertEquals("Chưa nhận xe", result.getVehicleLicensePlate());
        assertNull(result.getContractPdfPath());
        assertTrue(result.getCheckInPhotoPaths().isEmpty());
    }

    @Test
    void testGetBookingDetailsById_CatchJsonError() throws JsonProcessingException {
        String invalidJson = "[/path/photo1.jpg";
        mockBooking_Renting.setCheckInPhotoPaths(invalidJson);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting));
        when(contractRepository.findByBooking(mockBooking_Renting)).thenReturn(Optional.empty());

        BookingDetailResponse result = bookingService.getBookingDetailsById(100L);

        assertNotNull(result);
        assertTrue(result.getCheckInPhotoPaths().isEmpty());
    }

    // --- Tests for getMyBookings ---

    @Test
    void testGetMyBookings_Success() {
        // Cần mock vehicle.getModel()
        mockVehicle.setModel(mockModel);
        mockBooking_Confirmed.setVehicle(mockVehicle);
        mockBooking_Pending.setVehicle(null); // Pending không có xe

        when(bookingRepository.findByUserWithDetails(eq(mockRenter), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, mockBooking_Pending));

        List<BookingSummaryResponse> result = bookingService.getMyBookings(mockRenter);

        assertEquals(2, result.size());
        // Check booking 1 (có xe)
        assertEquals(99L, result.get(0).getBookingId());
        assertEquals(mockVehicle.getLicensePlate(), result.get(0).getVehicleLicensePlate());
        assertEquals(mockModel.getModelName(), result.get(0).getModelName());
        // Check booking 2 (chưa có xe)
        assertEquals(98L, result.get(1).getBookingId());
        assertEquals("Chưa nhận xe", result.get(1).getVehicleLicensePlate());
        assertEquals("N/A", result.get(1).getModelName());
    }

    // --- Tests for getAllBookingsByStation ---

    @Test
    void testGetAllBookingsByStation_Success_NoFilters() {
        mockVehicle.setModel(mockModel);
        mockBooking_Confirmed.setVehicle(mockVehicle);
        mockBooking_Renting.setVehicle(mockVehicle);
        when(bookingRepository.findAllByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, mockBooking_Renting));

        List<BookingSummaryResponse> result = bookingService.getAllBookingsByStation(mockStaff, null, null, null);

        assertEquals(2, result.size());
    }

    @Test
    void testGetAllBookingsByStation_Filter_ByKeywordName() {
        mockVehicle.setModel(mockModel);
        mockRenter.setFullName("Nguyễn Văn A");
        mockBooking_Confirmed.setUser(mockRenter);
        mockBooking_Confirmed.setVehicle(mockVehicle);

        User renterB = User.builder().userId(2L).fullName("Trần Thị B").build();
        mockBooking_Renting.setUser(renterB);
        mockBooking_Renting.setVehicle(mockVehicle);

        when(bookingRepository.findAllByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, mockBooking_Renting));

        List<BookingSummaryResponse> result = bookingService.getAllBookingsByStation(mockStaff, "văn a", null, null);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).getBookingId());
    }

    @Test
    void testGetAllBookingsByStation_Filter_ByKeywordPhone() {
        mockVehicle.setModel(mockModel);
        mockRenter.setPhone("0909123456");
        mockBooking_Confirmed.setUser(mockRenter);
        mockBooking_Confirmed.setVehicle(mockVehicle);

        User renterB = User.builder().userId(2L).phone("0808987654").build();
        mockBooking_Renting.setUser(renterB);
        mockBooking_Renting.setVehicle(mockVehicle);

        when(bookingRepository.findAllByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, mockBooking_Renting));

        List<BookingSummaryResponse> result = bookingService.getAllBookingsByStation(mockStaff, "123456", null, null);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).getBookingId());
    }

    @Test
    void testGetAllBookingsByStation_Filter_ByStatus() {
        mockVehicle.setModel(mockModel);
        mockBooking_Confirmed.setVehicle(mockVehicle);
        mockBooking_Renting.setVehicle(mockVehicle);
        when(bookingRepository.findAllByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, mockBooking_Renting));

        List<BookingSummaryResponse> result = bookingService.getAllBookingsByStation(mockStaff, null, "RENTING", null);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getBookingId());
    }

    @Test
    void testGetAllBookingsByStation_Filter_ByInvalidStatus() {
        mockVehicle.setModel(mockModel);
        mockBooking_Confirmed.setVehicle(mockVehicle);
        mockBooking_Renting.setVehicle(mockVehicle);
        when(bookingRepository.findAllByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, mockBooking_Renting));

        List<BookingSummaryResponse> result = bookingService.getAllBookingsByStation(mockStaff, null, "INVALID_STATUS", null);

        assertEquals(2, result.size());
    }

    @Test
    void testGetAllBookingsByStation_Fail_StaffNoStation() {
        mockStaff.setStation(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.getAllBookingsByStation(mockStaff, null, null, null);
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Nhân viên chưa được gán cho trạm nào"));
    }

    // --- Tests for initiateCheckIn ---

    @Test
    void testInitiateCheckIn_Success() throws PayOSException {
        // ARRANGE
        // (Mock PayOS thành công đã được setup trong @BeforeEach)
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        // ACT
        Map<String, Object> result = bookingService.initiateCheckIn(99L, mockStaff);

        // ASSERT
        assertNotNull(result);
        assertEquals("http://mock-payment-url.com", result.get("paymentUrl"));
        // 2% của 20 triệu = 400k
        assertEquals(400000.0, (Double) result.get("rentalDepositAmount"));
        verify(bookingRepository, times(1)).save(argThat(
                booking -> booking.getRentalDeposit() == 400000.0
        ));
        verify(paymentRequestsService, times(1)).create(any(CreatePaymentLinkRequest.class));
    }

    @Test
    void testInitiateCheckIn_Fail_StaffLocked() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.initiateCheckIn(99L, mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testInitiateCheckIn_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.initiateCheckIn(99L, mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testInitiateCheckIn_Fail_WrongStation() {
        Station otherStation = Station.builder().stationId(2L).build();
        mockStaff.setStation(otherStation);
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.initiateCheckIn(99L, mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testInitiateCheckIn_Fail_WrongBookingStatus() {
        mockBooking_Pending.setVehicle(mockVehicle);
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.initiateCheckIn(98L, mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testInitiateCheckIn_Fail_PayOSException() throws PayOSException {
        // ARRANGE
        // Ghi đè mock, giả lập NÉM LỖI
        when(paymentRequestsService.create(any(CreatePaymentLinkRequest.class)))
                .thenThrow(new PayOSException("Lỗi PayOS 2%", new RuntimeException("Mock internal error")));
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        // ACT & ASSERT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.initiateCheckIn(99L, mockStaff);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể tạo link thanh toán cọc 2%"));
    }

    // --- Tests for cancelBookingByRenter ---

    @Test
    void testCancelBookingByRenter_Fail_AccountLocked() {
        mockRenter.setStatus(AccountStatus.INACTIVE);
        // Test với PENDING
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByRenter(mockRenter, 98L, null);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testCancelBookingByRenter_Fail_WrongStatus() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting)); // Đang RENTING

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByRenter(mockRenter, 100L, null);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể hủy booking ở trạng thái"));
    }

    @Test
    void testCancelBookingByRenter_Fail_TooCloseToPickup() {
        mockBooking_Pending.setStartDate(LocalDateTime.now().plusHours(1)); // Còn 1 tiếng
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByRenter(mockRenter, 98L, null);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể hủy booking quá sát giờ nhận xe"));
    }

    @Test
    void testCancelBookingByRenter_Success_Pending_FirstCancel() {
        mockBooking_Pending.setStartDate(LocalDateTime.now().plusHours(3)); // Hợp lệ
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));

        String message = bookingService.cancelBookingByRenter(mockRenter, 98L, null);

        assertEquals(BookingStatus.CANCELLED, mockBooking_Pending.getStatus());
        assertEquals(1, mockRenter.getCancellationCount());
        assertTrue(message.contains("Đã hủy đơn (chưa thanh toán) thành công"));
        assertTrue(message.contains("Bạn còn 1 lần hủy an toàn"));
        verify(userRepository, times(1)).save(mockRenter);
        verify(bookingRepository, times(1)).save(mockBooking_Pending);
    }

    @Test
    void testCancelBookingByRenter_Success_Pending_SecondCancel_Warn() {
        mockRenter.setCancellationCount(1);
        mockBooking_Pending.setStartDate(LocalDateTime.now().plusHours(3));
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));

        String message = bookingService.cancelBookingByRenter(mockRenter, 98L, null);

        assertEquals(2, mockRenter.getCancellationCount());
        assertTrue(message.contains("CẢNH BÁO: Hủy thêm 1 lần nữa, tài khoản sẽ bị khóa"));
        verify(userRepository, times(1)).save(mockRenter);
    }

    @Test
    void testCancelBookingByRenter_Success_Pending_ThirdCancel_Lock() {
        mockRenter.setCancellationCount(2);
        mockBooking_Pending.setStartDate(LocalDateTime.now().plusHours(3));
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));

        String message = bookingService.cancelBookingByRenter(mockRenter, 98L, null);

        assertEquals(3, mockRenter.getCancellationCount());
        assertEquals(AccountStatus.INACTIVE, mockRenter.getStatus());
        assertTrue(message.contains("TÀI KHOẢN BỊ KHÓA"));
        verify(userRepository, times(1)).save(mockRenter);
    }

    @Test
    void testCancelBookingByRenter_Success_Confirmed_Within12Hours_WithRefund() {
        mockVehicle.setStatus(VehicleStatus.RESERVED);
        mockBooking_Confirmed.setStartDate(LocalDateTime.now().plusHours(3));
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        when(vehicleService.saveVehicle(any(Vehicle.class))).thenReturn(mockVehicle);

        CancelBookingRequest refundReq = new CancelBookingRequest();
        refundReq.setBankName("VCB");
        refundReq.setAccountNumber("123456");
        refundReq.setAccountName("NGUYEN VAN A");

        String message = bookingService.cancelBookingByRenter(mockRenter, 99L, refundReq);

        assertEquals(BookingStatus.CANCELLED_AWAIT_REFUND, mockBooking_Confirmed.getStatus());
        assertEquals(500000.0, mockBooking_Confirmed.getRefund());
        assertTrue(mockBooking_Confirmed.getRefundNote().contains("VCB"));
        assertEquals(VehicleStatus.AVAILABLE, mockVehicle.getStatus());
        assertTrue(message.contains("Yêu cầu hoàn cọc 500k"));

        verify(vehicleService, times(1)).saveVehicle(mockVehicle);
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
    }

    @Test
    void testCancelBookingByRenter_Fail_Confirmed_Within12Hours_NoRefundInfo() {
        mockBooking_Confirmed.setStartDate(LocalDateTime.now().plusHours(3));
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        // Không cần mock vehicleService.saveVehicle vì sẽ ném lỗi trước đó

        CancelBookingRequest refundReq = new CancelBookingRequest(); // Rỗng

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByRenter(mockRenter, 99L, refundReq);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng"));
    }

    @Test
    void testCancelBookingByRenter_Success_Confirmed_After12Hours_NoRefund() {
        mockVehicle.setStatus(VehicleStatus.RESERVED);
        mockBooking_Confirmed.setCreatedAt(LocalDateTime.now().minusHours(13));
        mockBooking_Confirmed.setStartDate(LocalDateTime.now().plusHours(3));
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        when(vehicleService.saveVehicle(any(Vehicle.class))).thenReturn(mockVehicle);

        CancelBookingRequest refundReq = new CancelBookingRequest();

        String message = bookingService.cancelBookingByRenter(mockRenter, 99L, refundReq);

        assertEquals(BookingStatus.CANCELLED, mockBooking_Confirmed.getStatus());
        assertNull(mockBooking_Confirmed.getRefund());
        assertEquals(VehicleStatus.AVAILABLE, mockVehicle.getStatus());
        assertTrue(message.contains("bạn sẽ bị mất cọc"));

        verify(vehicleService, times(1)).saveVehicle(mockVehicle);
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
    }

    @Test
    void testCancelBookingByRenter_Success_Confirmed_VehicleNotInReservedStatus() {
        mockVehicle.setStatus(VehicleStatus.AVAILABLE); // Trạng thái không mong đợi
        mockBooking_Confirmed.setStartDate(LocalDateTime.now().plusHours(3));
        mockBooking_Confirmed.setCreatedAt(LocalDateTime.now().minusHours(13)); // Sau 12h để không cần refund
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        String message = bookingService.cancelBookingByRenter(mockRenter, 99L, new CancelBookingRequest());

        assertEquals(BookingStatus.CANCELLED, mockBooking_Confirmed.getStatus());
        // Xác minh rằng vehicleService.saveVehicle không được gọi
        verify(vehicleService, never()).saveVehicle(any(Vehicle.class));
        assertTrue(message.contains("bạn sẽ bị mất cọc"));
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
    }

    // --- Tests for getPendingRefundsByStation ---

    @Test
    void testGetPendingRefundsByStation_Success() {
        mockVehicle.setModel(mockModel); // Cần mock model cho getModelName()
        Booking bookingRefund = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CANCELLED_AWAIT_REFUND)
                .user(mockRenter)
                .vehicle(mockVehicle)
                .refund(500000.0)
                .refundNote("VCB 123456")
                .build();

        when(bookingRepository.findAllByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockBooking_Confirmed, bookingRefund));

        List<BookingSummaryResponse> result = bookingService.getPendingRefundsByStation(mockStaff);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getBookingId());
        assertEquals(500000.0, result.get(0).getRefundAmount());
        assertEquals("VCB 123456", result.get(0).getRefundInfo());
        assertEquals(mockModel.getModelName(), result.get(0).getModelName());
    }

    @Test
    void testGetPendingRefundsByStation_Fail_StaffLocked() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.getPendingRefundsByStation(mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testGetPendingRefundsByStation_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.getPendingRefundsByStation(mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // --- Tests for confirmRefund ---

    @Test
    void testConfirmRefund_Success() {
        Booking bookingRefund = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CANCELLED_AWAIT_REFUND)
                .station(mockStation)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingRefund));

        bookingService.confirmRefund(mockStaff, 1L);

        assertEquals(BookingStatus.REFUNDED, bookingRefund.getStatus());
        verify(bookingRepository, times(1)).save(bookingRefund);
        verify(paymentService, times(1)).createTransaction(
                eq(bookingRefund),
                eq(-500000.0),
                eq(PaymentMethod.BANK_TRANSFER),
                eq(mockStaff),
                anyString()
        );
    }

    @Test
    void testConfirmRefund_Fail_StaffLocked() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.confirmRefund(mockStaff, 1L);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmRefund_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.confirmRefund(mockStaff, 1L);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmRefund_Fail_WrongStation() {
        Booking bookingRefund = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CANCELLED_AWAIT_REFUND)
                .station(Station.builder().stationId(2L).build())
                .build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(bookingRefund));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.confirmRefund(mockStaff, 1L);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmRefund_Fail_WrongStatus() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.confirmRefund(mockStaff, 99L);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Booking này không ở trạng thái chờ hoàn tiền"));
    }

    // --- Tests for cancelBookingByStaff ---

    @Test
    void testCancelBookingByStaff_Success() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        bookingService.cancelBookingByStaff(99L, mockStaff);

        assertEquals(BookingStatus.CANCELLED, mockBooking_Confirmed.getStatus());
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
    }

    @Test
    void testCancelBookingByStaff_Fail_StaffLocked() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByStaff(99L, mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testCancelBookingByStaff_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByStaff(99L, mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testCancelBookingByStaff_Fail_WrongStation() {
        Booking booking = Booking.builder()
                .bookingId(1L)
                .status(BookingStatus.CONFIRMED)
                .station(Station.builder().stationId(2L).build())
                .build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByStaff(1L, mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testCancelBookingByStaff_Fail_WrongStatus() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.cancelBookingByStaff(100L, mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể hủy booking ở trạng thái"));
    }

    // --- Tests for processCheckIn ---

    @Test
    void testProcessCheckIn_Success_PayOSPaid() throws Exception {
        mockBooking_Confirmed.setRentalDepositPaid(true);
        mockBooking_Confirmed.setRentalDeposit(400000.0);
        mockBooking_Confirmed.setVehicle(mockVehicle); // Đảm bảo xe đã được gán

        MultipartFile mockFile = new MockMultipartFile("checkInPhotos", "photo.jpg", "image/jpeg", "content".getBytes());
        CheckInRequest req = CheckInRequest.builder()
                .depositPaymentMethod(PaymentMethod.GATEWAY)
                .conditionBefore("GOOD")
                .battery(95.0)
                .mileage(1000.0)
                .checkInPhotos(List.of(mockFile))
                .build();

        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        Contract mockContract = Contract.builder().contractPdfPath("/path/contract.pdf").build();
        when(contractService.generateAndSaveContract(eq(mockBooking_Confirmed), eq(mockStaff))).thenReturn(mockContract);

        Contract resultContract = bookingService.processCheckIn(99L, req, mockStaff);

        assertNotNull(resultContract);
        assertEquals("/path/contract.pdf", resultContract.getContractPdfPath());
        assertEquals(BookingStatus.RENTING, mockBooking_Confirmed.getStatus());
        assertEquals(VehicleStatus.RENTED, mockVehicle.getStatus());

        verify(paymentService, never()).createTransaction(any(), anyDouble(), any(), any(), anyString());
        verify(vehicleService, times(1)).saveVehicle(mockVehicle);
        verify(contractService, times(1)).generateAndSaveContract(any(Booking.class), any(User.class));
        verify(vehicleService, times(1)).recordVehicleAction(anyLong(), anyLong(), anyLong(), anyLong(), eq(VehicleActionType.DELIVERY), anyString(), anyString(), isNull(), anyDouble(), anyDouble(), anyString());
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
    }

    @Test
    void testProcessCheckIn_Success_ManualPay() throws Exception {
        mockBooking_Confirmed.setRentalDepositPaid(false);
        mockBooking_Confirmed.setRentalDeposit(400000.0); // Giả lập tiền cọc từ initiate
        mockBooking_Confirmed.setVehicle(mockVehicle);

        MultipartFile mockFile = new MockMultipartFile("checkInPhotos", "photo.jpg", "image/jpeg", "content".getBytes());
        CheckInRequest req = CheckInRequest.builder()
                .depositPaymentMethod(PaymentMethod.CASH)
                .conditionBefore("GOOD")
                .battery(95.0)
                .mileage(1000.0)
                .checkInPhotos(List.of(mockFile))
                .build();

        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        Contract mockContract = Contract.builder().contractPdfPath("/path/contract.pdf").build();
        when(contractService.generateAndSaveContract(eq(mockBooking_Confirmed), eq(mockStaff))).thenReturn(mockContract);


        Contract resultContract = bookingService.processCheckIn(99L, req, mockStaff);

        assertNotNull(resultContract);
        assertTrue(mockBooking_Confirmed.isRentalDepositPaid());

        verify(paymentService, times(1)).createTransaction(
                eq(mockBooking_Confirmed),
                eq(400000.0),
                eq(PaymentMethod.CASH),
                eq(mockStaff),
                anyString()
        );
    }

    @Test
    void testProcessCheckIn_Fail_StaffLocked() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(99L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testProcessCheckIn_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(99L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testProcessCheckIn_Fail_WrongStation() {
        mockStaff.setStation(Station.builder().stationId(2L).build());
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(99L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testProcessCheckIn_Fail_StatusPending() {
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(98L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("chưa được xác nhận thanh toán phí giữ chỗ"));
    }

    @Test
    void testProcessCheckIn_Fail_StatusRenting() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(100L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("đã được thuê"));
    }

    @Test
    void testProcessCheckIn_Fail_StatusCompleted() {
        mockBooking_Renting.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(100L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("đã được hoàn thành"));
    }

    @Test
    void testProcessCheckIn_Fail_StatusCancelled() {
        mockBooking_Renting.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(mockBooking_Renting));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(100L, new CheckInRequest(), mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("đã bị hủy"));
    }

    @Test
    void testProcessCheckIn_Fail_NoPhotos() {
        CheckInRequest req = CheckInRequest.builder().checkInPhotos(null).build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(99L, req, mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ảnh check-in (lúc giao xe) là bắt buộc"));
    }

    @Test
    void testProcessCheckIn_Fail_EmptyPhotos() {
        CheckInRequest req = CheckInRequest.builder().checkInPhotos(List.of(new MockMultipartFile("f", "", "", (byte[]) null))).build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(99L, req, mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ảnh check-in (lúc giao xe) là bắt buộc"));
    }

    @Test
    void testProcessCheckIn_Fail_ManualPay_UsingGateway() {
        mockBooking_Confirmed.setRentalDepositPaid(false);
        CheckInRequest req = CheckInRequest.builder()
                .depositPaymentMethod(PaymentMethod.GATEWAY)
                .checkInPhotos(List.of(new MockMultipartFile("f", "f.jpg", "img/jpg", "c".getBytes())))
                .build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.processCheckIn(99L, req, mockStaff);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Thanh toán qua cổng PayOS chưa được xác nhận"));
    }

    @Test
    void testProcessCheckIn_CatchJsonError() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(anyList()))
                .thenThrow(new JsonProcessingException("Mock JSON Error") {});

        BookingServiceImpl localBookingService = new BookingServiceImpl(
                bookingRepository,
                vehicleRepository,
                userRepository,
                paymentService,
                vehicleService,
                payOS,
                stationService,
                mockMapper,
                contractRepository,
                contractService
        );
        BookingServiceImpl localServiceSpy = Mockito.spy(localBookingService);

        mockBooking_Confirmed.setRentalDepositPaid(true);
        mockBooking_Confirmed.setVehicle(mockVehicle);
        MultipartFile mockFile = new MockMultipartFile(
                "checkInPhotos", "photo.jpg", "image/jpeg", "content".getBytes()
        );
        CheckInRequest req = CheckInRequest.builder()
                .depositPaymentMethod(PaymentMethod.GATEWAY)
                .conditionBefore("GOOD")
                .battery(95.0)
                .mileage(1000.0)
                .checkInPhotos(List.of(mockFile))
                .build();

        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        when(contractService.generateAndSaveContract(eq(mockBooking_Confirmed), eq(mockStaff)))
                .thenReturn(Contract.builder().build());

        doReturn("/mock/path/photo.jpg").when(localServiceSpy)
                .saveHandoverPhoto(any(MultipartFile.class), eq(99L), eq("checkin"), anyInt());

        localServiceSpy.processCheckIn(99L, req, mockStaff);

        assertNull(mockBooking_Confirmed.getCheckInPhotoPaths());
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
    }

    @Test
    void testProcessCheckIn_CatchVehicleActionError() throws Exception {
        mockBooking_Confirmed.setRentalDepositPaid(true);
        MultipartFile mockFile = new MockMultipartFile("checkInPhotos", "photo.jpg", "image/jpeg", "content".getBytes());
        CheckInRequest req = CheckInRequest.builder()
                .depositPaymentMethod(PaymentMethod.GATEWAY)
                .conditionBefore("GOOD")
                .battery(95.0)
                .mileage(1000.0)
                .checkInPhotos(List.of(mockFile))
                .build();

        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Confirmed));
        when(contractService.generateAndSaveContract(eq(mockBooking_Confirmed), eq(mockStaff)))
                .thenReturn(Contract.builder().build());

        doThrow(new RuntimeException("Lỗi ghi lịch sử"))
                .when(vehicleService).recordVehicleAction(anyLong(), anyLong(), anyLong(), anyLong(), eq(VehicleActionType.DELIVERY), anyString(), anyString(), isNull(), anyDouble(), anyDouble(), anyString());


        Contract resultContract = bookingService.processCheckIn(99L, req, mockStaff);
        assertNotNull(resultContract);
        assertEquals(BookingStatus.RENTING, mockBooking_Confirmed.getStatus());
    }

    // --- Tests for saveHandoverPhoto ---

    @Test
    void testSaveHandoverPhoto_Fail_NullFile() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.saveHandoverPhoto(null, 1L, "checkin", 1);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testSaveHandoverPhoto_Fail_EmptyFile() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            bookingService.saveHandoverPhoto(mockFile, 1L, "checkin", 1);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // --- Tests for getPeakHourStatistics ---

    @Test
    void testGetPeakHourStatistics_AllStations() {
        Booking b1 = Booking.builder().startDate(LocalDate.now().atTime(10, 0)).build();
        Booking b2 = Booking.builder().startDate(LocalDate.now().atTime(17, 0)).build();
        Booking b3 = Booking.builder().startDate(LocalDate.now().atTime(17, 0)).build();

        when(bookingRepository.findAllByStartDateBetween(any(), any())).thenReturn(List.of(b1, b2, b3));

        Map<String, Object> result = bookingService.getPeakHourStatistics(null, LocalDate.now(), LocalDate.now());

        assertEquals(3L, result.get("totalRentals"));
        assertEquals("Tất cả các trạm", result.get("scope"));
        assertEquals("17:00 - 18:00", result.get("peakHour"));

        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertEquals(1L, data.get(10).get("rentedVehicles"));
        assertEquals(2L, data.get(17).get("rentedVehicles"));
        assertEquals(100.0, data.get(17).get("percentOfPeak"));
        assertEquals(50.0, data.get(10).get("percentOfPeak"));
    }

    @Test
    void testGetPeakHourStatistics_SpecificStation() {
        Booking b1 = Booking.builder().startDate(LocalDate.now().atTime(10, 0)).build();

        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(bookingRepository.findAllByStationAndStartDateBetween(eq(mockStation), any(), any())).thenReturn(List.of(b1));

        Map<String, Object> result = bookingService.getPeakHourStatistics(1L, LocalDate.now(), LocalDate.now());

        assertEquals(1L, result.get("totalRentals"));
        assertEquals("Trạm ID 1", result.get("scope"));
        assertEquals("10:00 - 11:00", result.get("peakHour"));
    }

    @Test
    void testGetPeakHourStatistics_NoData() {
        when(bookingRepository.findAllByStartDateBetween(any(), any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = bookingService.getPeakHourStatistics(null, LocalDate.now(), LocalDate.now());

        assertEquals(0L, result.get("totalRentals"));
        assertEquals("Không có dữ liệu", result.get("peakHour"));
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertEquals(0.0, data.get(10).get("percentOfPeak"));
    }

    @Test
    void testGetPeakHourStatistics_DefaultDates() {
        // Test kịch bản fromDate và toDate là null
        Booking b1 = Booking.builder().startDate(LocalDate.now().atTime(10, 0)).build();
        when(bookingRepository.findAllByStartDateBetween(any(), any())).thenReturn(List.of(b1));

        bookingService.getPeakHourStatistics(null, null, null);

        // Xác minh rằng nó gọi repo với ngày mặc định (7 ngày trước đến hôm nay)
        LocalDateTime expectedTo = LocalDate.now().atTime(23, 59, 59);
        LocalDateTime expectedFrom = LocalDate.now().minusDays(7).atStartOfDay();
        verify(bookingRepository, times(1)).findAllByStartDateBetween(eq(expectedFrom), eq(expectedTo));
    }
}
