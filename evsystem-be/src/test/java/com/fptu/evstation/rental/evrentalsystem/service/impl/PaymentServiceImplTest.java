package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.BillResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.PaymentConfirmationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.PenaltyCalculationRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.*;
import com.fptu.evstation.rental.evrentalsystem.service.InvoiceService;
import com.fptu.evstation.rental.evrentalsystem.service.VehicleService;
import com.fptu.evstation.rental.evrentalsystem.service.util.QrCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PenaltyFeeRepository penaltyFeeRepository;
    @Mock
    private TransactionDetailRepository transactionDetailRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private VehicleHistoryRepository historyRepository;
    @Mock
    private VehicleService vehicleService;
    @Mock
    private QrCodeService qrCodeService;
    @Mock
    private InvoiceService invoiceService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // --- Mock Data ---
    private User mockStaff;
    private Booking mockBooking_Renting;
    private Booking mockBooking_Pending;
    private Booking mockBooking_Confirmed;
    private Vehicle mockVehicle;
    private Model mockModel;
    private PenaltyFee mockFee_Vesinh;
    private PenaltyFee mockFee_Adjustment;
    @Mock
    private MultipartFile mockPhoto;
    private VehicleHistory mockCheckInHistory;

    @BeforeEach
    void setUp() throws IOException {
        mockStaff = User.builder()
                .userId(10L)
                .status(AccountStatus.ACTIVE)
                .fullName("Test Staff")
                .station(Station.builder().stationId(1L).build())
                .build();

        mockModel = Model.builder().modelId(1L).pricePerHour(10000.0).initialValue(10000000.0).build();

        mockVehicle = Vehicle.builder().vehicleId(101L).model(mockModel)
                .station(mockStaff.getStation()).build();

        mockBooking_Renting = Booking.builder()
                .bookingId(99L)
                .status(BookingStatus.RENTING)
                .station(mockStaff.getStation())
                .user(User.builder().userId(1L).build())
                .vehicle(mockVehicle)
                .startDate(LocalDateTime.now().minusHours(2))
                .endDate(LocalDateTime.now().plusHours(6))
                .rentalDeposit(200000.0)
                .reservationDepositPaid(true)
                .build();

        mockBooking_Pending = Booking.builder()
                .bookingId(98L)
                .status(BookingStatus.PENDING)
                .station(mockStaff.getStation())
                .user(User.builder().userId(2L).build())
                .vehicle(mockVehicle)
                .build();

        mockBooking_Confirmed = Booking.builder()
                .bookingId(97L)
                .status(BookingStatus.CONFIRMED)
                .station(mockStaff.getStation())
                .rentalDeposit(200000.0)
                .reservationDepositPaid(true)
                .rentalDepositPaid(false)
                .build();

        mockFee_Vesinh = PenaltyFee.builder().feeId(1L).feeName("Phí vệ sinh").fixedAmount(50000.0).isAdjustment(false).build();
        mockFee_Adjustment = PenaltyFee.builder().feeId(99L).feeName("Phí tùy chỉnh").isAdjustment(true).build();

        mockCheckInHistory = VehicleHistory.builder().conditionBefore("EXCELLENT").build();

        lenient().when(mockPhoto.getOriginalFilename()).thenReturn("test.png");
        lenient().when(mockPhoto.isEmpty()).thenReturn(false);
        lenient().when(mockPhoto.getBytes()).thenReturn("test-data".getBytes());
        lenient().doNothing().when(mockPhoto).transferTo(any(Path.class));

        when(bookingRepository.findById(99L)).thenReturn(Optional.of(mockBooking_Renting));
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));
        when(bookingRepository.findById(97L)).thenReturn(Optional.of(mockBooking_Confirmed));

        when(vehicleService.saveVehicle(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void setupMocksForCalculateBill() {
        when(transactionDetailRepository.findByBooking(any(Booking.class))).thenReturn(Collections.emptyList());
        when(qrCodeService.generateQrCodeBase64(anyDouble(), anyLong())).thenReturn("base64_qr_code_string");
        when(invoiceService.generateAndSaveInvoicePdf(any(BillResponse.class))).thenReturn("/path/to/invoice.pdf");
    }

    private PaymentConfirmationRequest setupMockRequestForConfirm() {
        PaymentConfirmationRequest req = new PaymentConfirmationRequest();
        req.setPaymentMethod(PaymentMethod.CASH);
        req.setConditionAfter("GOOD");
        req.setBattery(50.0);
        req.setMileage(1600.0);
        req.setConfirmPhotos(List.of(mockPhoto));
        return req;
    }

    @Test
    void testCalculateFinalBill_Success_OnTime_NoFees_NoDeposits() {
        setupMocksForCalculateBill();
        mockBooking_Renting.setRentalDeposit(null);
        mockBooking_Renting.setReservationDepositPaid(false);
        mockBooking_Renting.setStartDate(LocalDateTime.now().minusHours(2));

        PenaltyCalculationRequest req = new PenaltyCalculationRequest();
        BillResponse bill = paymentService.calculateFinalBill(mockStaff, 99L, req);

        assertNotNull(bill);
        assertEquals(20000.0, bill.getBaseRentalFee());
        assertEquals(0.0, bill.getTotalPenaltyFee());
        assertEquals(0.0, bill.getDownpayPaid());
        assertEquals(0.0, bill.getTotalDiscount());
        assertEquals(20000.0, bill.getPaymentDue());
        assertEquals(0.0, bill.getRefundToCustomer());
        assertEquals("/path/to/invoice.pdf", bill.getInvoicePdfPath());
        assertNotNull(bill.getQrCodeUrl());

        verify(transactionDetailRepository, times(1)).deleteByBooking(any(Booking.class));
        verify(invoiceService, times(1)).generateAndSaveInvoicePdf(any(BillResponse.class));
    }

    @Test
    void testCalculateFinalBill_Success_OnTime_NoFees_WithDeposits() {
        setupMocksForCalculateBill();
        mockBooking_Renting.setStartDate(LocalDateTime.now().minusHours(2));

        PenaltyCalculationRequest req = new PenaltyCalculationRequest();
        BillResponse bill = paymentService.calculateFinalBill(mockStaff, 99L, req);

        assertNotNull(bill);
        assertEquals(20000.0, bill.getBaseRentalFee());
        assertEquals(0.0, bill.getTotalPenaltyFee());
        assertEquals(700000.0, bill.getDownpayPaid());
        assertEquals(0.0, bill.getTotalDiscount());
        assertEquals(0.0, bill.getPaymentDue());
        assertEquals(680000.0, bill.getRefundToCustomer());
        assertNull(bill.getQrCodeUrl());
    }

    @Test
    void testCalculateFinalBill_Success_WithLateFee() {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest req = new PenaltyCalculationRequest();
        mockBooking_Renting.setStartDate(LocalDateTime.now().minusHours(3));
        mockBooking_Renting.setEndDate(LocalDateTime.now().minusHours(1));

        BillResponse bill = paymentService.calculateFinalBill(mockStaff, 99L, req);

        assertNotNull(bill);
        assertEquals(130000.0, bill.getBaseRentalFee());
        assertEquals(0.0, bill.getTotalPenaltyFee());
        assertEquals(700000.0, bill.getDownpayPaid());
        assertEquals(0.0, bill.getPaymentDue());
        assertEquals(570000.0, bill.getRefundToCustomer());
    }

    @Test
    void testCalculateFinalBill_Success_WithSelectedFees() {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.SelectedFee fee = new PenaltyCalculationRequest.SelectedFee(1L, 2);
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(List.of(fee), null);

        when(penaltyFeeRepository.findById(1L)).thenReturn(Optional.of(mockFee_Vesinh));
        mockBooking_Renting.setStartDate(LocalDateTime.now().minusHours(2));
        BillResponse bill = paymentService.calculateFinalBill(mockStaff, 99L, req);

        assertEquals(20000.0, bill.getBaseRentalFee());
        assertEquals(100000.0, bill.getTotalPenaltyFee());
        assertEquals(700000.0, bill.getDownpayPaid());
        assertEquals(0.0, bill.getPaymentDue());
        assertEquals(580000.0, bill.getRefundToCustomer());

        verify(transactionDetailRepository, times(1)).save(any(TransactionDetail.class));
    }

    @Test
    void testCalculateFinalBill_Success_WithCustomFee_Damage() throws Exception {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.CustomFee customFee = new PenaltyCalculationRequest.CustomFee("Vỡ gương", "Vỡ gương phải", 100000.0, List.of(mockPhoto));
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(null, customFee);

        when(penaltyFeeRepository.findByIsAdjustmentTrue()).thenReturn(Optional.of(mockFee_Adjustment));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            mockBooking_Renting.setStartDate(LocalDateTime.now().minusHours(2));
            BillResponse bill = paymentService.calculateFinalBill(mockStaff, 99L, req);

            assertEquals(20000.0, bill.getBaseRentalFee());
            assertEquals(100000.0, bill.getTotalPenaltyFee());
            assertEquals(700000.0, bill.getDownpayPaid());
            assertEquals(0.0, bill.getPaymentDue());
            assertEquals(580000.0, bill.getRefundToCustomer());

            ArgumentCaptor<TransactionDetail> detailCaptor = ArgumentCaptor.forClass(TransactionDetail.class);
            verify(transactionDetailRepository, times(1)).save(detailCaptor.capture());
            assertNotNull(detailCaptor.getValue().getPhotoPaths());
            assertTrue(detailCaptor.getValue().getPhotoPaths().contains("adjustment-1.png"));
        }
    }

    @Test
    void testCalculateFinalBill_Success_WithCustomFee_Discount() {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.CustomFee customFee = new PenaltyCalculationRequest.CustomFee("Khuyến mãi", "Giảm giá", -10000.0, null);
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(null, customFee);

        when(penaltyFeeRepository.findByIsAdjustmentTrue()).thenReturn(Optional.of(mockFee_Adjustment));
        mockBooking_Renting.setStartDate(LocalDateTime.now().minusHours(2));
        BillResponse bill = paymentService.calculateFinalBill(mockStaff, 99L, req);

        assertEquals(20000.0, bill.getBaseRentalFee());
        assertEquals(0.0, bill.getTotalPenaltyFee());
        assertEquals(10000.0, bill.getTotalDiscount());
        assertEquals(700000.0, bill.getDownpayPaid());
        assertEquals(0.0, bill.getPaymentDue());
        assertEquals(690000.0, bill.getRefundToCustomer());

        verify(transactionDetailRepository, times(1)).save(any(TransactionDetail.class));
    }

    @Test
    void testCalculateFinalBill_Success_WithOldPhotosCleanup() throws Exception {
        setupMocksForCalculateBill();
        String oldPhotoPath = "/uploads/adjustments/booking_99/old_photo.png";
        TransactionDetail oldDetail = TransactionDetail.builder()
                .penaltyFee(mockFee_Adjustment)
                .photoPaths("[\"" + oldPhotoPath + "\"]")
                .build();
        when(transactionDetailRepository.findByBooking(any(Booking.class))).thenReturn(List.of(oldDetail));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            paymentService.calculateFinalBill(mockStaff, 99L, new PenaltyCalculationRequest());
            mockedFiles.verify(() -> Files.deleteIfExists(argThat(path -> path.toString().endsWith("old_photo.png"))), times(1));
        }
    }

    @Test
    void testCalculateFinalBill_Fail_OldPhotoCleanupError() throws Exception {
        setupMocksForCalculateBill();
        String oldPhotoPath = "/uploads/adjustments/booking_99/old_photo.png";
        TransactionDetail oldDetail = TransactionDetail.builder()
                .penaltyFee(mockFee_Adjustment)
                .photoPaths("[\"" + oldPhotoPath + "\"]")
                .build();
        when(transactionDetailRepository.findByBooking(any(Booking.class))).thenReturn(List.of(oldDetail));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenThrow(new IOException("Access Denied"));
            // The method should not throw an exception, but log a warning.
            assertDoesNotThrow(() -> paymentService.calculateFinalBill(mockStaff, 99L, new PenaltyCalculationRequest()));
            // Verify that the rest of the method continues
            verify(invoiceService, times(1)).generateAndSaveInvoicePdf(any(BillResponse.class));
        }
    }

    @Test
    void testCalculateFinalBill_Fail_StaffLocked() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.calculateFinalBill(mockStaff, 99L, new PenaltyCalculationRequest())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tài khoản nhân viên của bạn đã bị khóa"));
    }

    @Test
    void testCalculateFinalBill_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.calculateFinalBill(mockStaff, 99L, new PenaltyCalculationRequest())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Nhân viên chưa được gán cho trạm nào"));
    }

    @Test
    void testCalculateFinalBill_Fail_WrongStation() {
        mockStaff.setStation(Station.builder().stationId(2L).build());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.calculateFinalBill(mockStaff, 99L, new PenaltyCalculationRequest())
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn không có quyền thao tác trên đơn hàng của trạm khác"));
    }

    @Test
    void testCalculateFinalBill_Fail_WrongBookingStatus() {
        mockBooking_Renting.setStatus(BookingStatus.COMPLETED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.calculateFinalBill(mockStaff, 99L, new PenaltyCalculationRequest())
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Chỉ có thể tính phí khi đơn đang ở trạng thái RENTING"));
    }

    @Test
    void testCalculateFinalBill_Fail_PenaltyFeeNotFound() {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.SelectedFee fee = new PenaltyCalculationRequest.SelectedFee(1L, 1);
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(List.of(fee), null);
        when(penaltyFeeRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.calculateFinalBill(mockStaff, 99L, req)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không tìm thấy loại phí với ID: 1"));
    }

    @Test
    void testCalculateFinalBill_Fail_AdjustmentFeeNotFound() {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.CustomFee customFee = new PenaltyCalculationRequest.CustomFee("Phí", "Desc", 100.0, null);
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(null, customFee);
        when(penaltyFeeRepository.findByIsAdjustmentTrue()).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.calculateFinalBill(mockStaff, 99L, req)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Thiếu cấu hình phí tùy chỉnh trong DB"));
    }

    @Test
    void testCalculateFinalBill_Fail_SaveAdjustmentPhotoError() {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.CustomFee customFee = new PenaltyCalculationRequest.CustomFee("Vỡ gương", "Vỡ gương phải", 100000.0, List.of(mockPhoto));
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(null, customFee);
        when(penaltyFeeRepository.findByIsAdjustmentTrue()).thenReturn(Optional.of(mockFee_Adjustment));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenThrow(new IOException("Lỗi tạo thư mục"));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    paymentService.calculateFinalBill(mockStaff, 99L, req)
            );
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Lỗi hệ thống khi lưu file ảnh"));
        }
    }

    @Test
    void testCalculateFinalBill_Fail_ObjectMapperError() throws Exception {
        setupMocksForCalculateBill();
        PenaltyCalculationRequest.CustomFee customFee = new PenaltyCalculationRequest.CustomFee("Vỡ gương", "Vỡ gương phải", 100000.0, List.of(mockPhoto));
        PenaltyCalculationRequest req = new PenaltyCalculationRequest(null, customFee);
        when(penaltyFeeRepository.findByIsAdjustmentTrue()).thenReturn(Optional.of(mockFee_Adjustment));
        doThrow(new JsonProcessingException("Lỗi JSON") {}).when(objectMapper).writeValueAsString(any());

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    paymentService.calculateFinalBill(mockStaff, 99L, req)
            );
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Lỗi khi xử lý ảnh phạt"));
        }
    }

    @Test
    void testConfirmDeposit_Success() {
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking_Pending);
        mockBooking_Pending.getVehicle().setStatus(VehicleStatus.AVAILABLE);

        paymentService.confirmDeposit(mockStaff, 98L);

        assertEquals(VehicleStatus.RESERVED, mockBooking_Pending.getVehicle().getStatus());
        assertEquals(BookingStatus.CONFIRMED, mockBooking_Pending.getStatus());
        assertTrue(mockBooking_Pending.isReservationDepositPaid());
        verify(vehicleService, times(1)).saveVehicle(any(Vehicle.class));
        verify(bookingRepository, times(1)).save(any(Booking.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testConfirmDeposit_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmDeposit(mockStaff, 98L)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmDeposit_Fail_WrongStation() {
        mockStaff.setStation(Station.builder().stationId(2L).build());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmDeposit(mockStaff, 98L)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmDeposit_Fail_AlreadyConfirmed() {
        mockBooking_Pending.setStatus(BookingStatus.CONFIRMED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmDeposit(mockStaff, 98L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Đơn này đã được xác nhận cọc"));
    }

    @Test
    void testConfirmDeposit_Fail_NotPending() {
        mockBooking_Pending.setStatus(BookingStatus.RENTING);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmDeposit(mockStaff, 98L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Chỉ có thể xác nhận cọc cho đơn ở trạng thái PENDING"));
    }

    @Test
    void testAutoConfirmDeposit_Success() {
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(mockBooking_Pending));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking_Pending);
        mockBooking_Pending.getVehicle().setStatus(VehicleStatus.AVAILABLE);

        paymentService.autoConfirmDeposit(98L);

        assertEquals(VehicleStatus.RESERVED, mockBooking_Pending.getVehicle().getStatus());
        assertEquals(BookingStatus.CONFIRMED, mockBooking_Pending.getStatus());
        assertTrue(mockBooking_Pending.isReservationDepositPaid());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testAutoConfirmDeposit_Fail_BookingNotFound() {
        when(bookingRepository.findById(98L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.autoConfirmDeposit(98L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testAutoConfirmDeposit_Warn_AlreadyConfirmed() {
        Booking alreadyConfirmedBooking = Booking.builder().bookingId(98L).status(BookingStatus.CONFIRMED).build();
        when(bookingRepository.findById(98L)).thenReturn(Optional.of(alreadyConfirmedBooking));

        paymentService.autoConfirmDeposit(98L);

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testAutoConfirmDeposit_Fail_NotPending() {
        mockBooking_Pending.setStatus(BookingStatus.RENTING);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.autoConfirmDeposit(98L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void testAutoConfirmDeposit_Fail_VehicleIsNull() {
        mockBooking_Pending.setVehicle(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.autoConfirmDeposit(98L)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Booking này bị lỗi: không tìm thấy thông tin xe"));
    }

    @Test
    void testAutoConfirmDeposit_Fail_VehicleNotAvailable() {
        mockBooking_Pending.getVehicle().setStatus(VehicleStatus.RENTED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.autoConfirmDeposit(98L)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Xe này vừa được người khác đặt"));
    }

    @Test
    void testAutoConfirmRentalDeposit_Success() {
        paymentService.autoConfirmRentalDeposit(97L);
        assertTrue(mockBooking_Confirmed.isRentalDepositPaid());
        verify(bookingRepository, times(1)).save(mockBooking_Confirmed);
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(200000.0, captor.getValue().getAmount());
        assertEquals(PaymentMethod.GATEWAY, captor.getValue().getPaymentMethod());
        assertNull(captor.getValue().getStaff());
    }

    @Test
    void testAutoConfirmRentalDeposit_Fail_BookingNotFound() {
        when(bookingRepository.findById(97L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.autoConfirmRentalDeposit(97L)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testAutoConfirmRentalDeposit_Warn_NotConfirmed() {
        mockBooking_Confirmed.setStatus(BookingStatus.PENDING);
        paymentService.autoConfirmRentalDeposit(97L);
        assertFalse(mockBooking_Confirmed.isRentalDepositPaid());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testAutoConfirmRentalDeposit_Warn_AlreadyPaid() {
        mockBooking_Confirmed.setRentalDepositPaid(true);
        paymentService.autoConfirmRentalDeposit(97L);
        verify(bookingRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void testAutoConfirmRentalDeposit_Fail_DepositAmountNull() {
        mockBooking_Confirmed.setRentalDeposit(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.autoConfirmRentalDeposit(97L)
        );
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Lỗi logic: Không tìm thấy số tiền cọc 2%"));
    }

    @Test
    void testConfirmFinalPayment_Success_CustomerOwes() throws IOException {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        mockBooking_Renting.setFinalFee(1000000.0);
        mockBooking_Renting.setRentalDeposit(200000.0);
        mockBooking_Renting.setReservationDepositPaid(true);
        when(transactionDetailRepository.findTotalDiscountByBooking(any(Booking.class))).thenReturn(0.0);
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(mockCheckInHistory);

        ArgumentCaptor<Vehicle> vehicleCaptor = ArgumentCaptor.forClass(Vehicle.class);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            Map<String, Object> result = paymentService.confirmFinalPayment(99L, req, mockStaff);

            verify(vehicleService).saveVehicle(vehicleCaptor.capture());
            Vehicle savedVehicle = vehicleCaptor.getValue();

            assertEquals(BookingStatus.COMPLETED, mockBooking_Renting.getStatus());
            assertEquals(VehicleStatus.UNAVAILABLE, savedVehicle.getStatus());
            assertEquals(50, savedVehicle.getBatteryLevel());
            assertEquals(1600.0, savedVehicle.getCurrentMileage());

            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(1)).save(transactionCaptor.capture());
            assertEquals(300000.0, transactionCaptor.getValue().getAmount());

            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            nf.setMaximumFractionDigits(0);

            verify(vehicleService, times(1)).recordVehicleAction(anyLong(), anyLong(), anyLong(), anyLong(), eq(VehicleActionType.RETURN), anyString(), eq("EXCELLENT"), eq("GOOD"), anyDouble(), anyDouble(), anyString());
            assertTrue(result.get("message").toString().contains(nf.format(300_000) + " VNĐ"));
        }
    }

    @Test
    void testConfirmFinalPayment_Success_RefundToCustomer() throws IOException {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        mockBooking_Renting.setFinalFee(50000.0);
        when(transactionDetailRepository.findTotalDiscountByBooking(any(Booking.class))).thenReturn(0.0);
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(mockCheckInHistory);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            Map<String, Object> result = paymentService.confirmFinalPayment(99L, req, mockStaff);

            assertEquals(BookingStatus.COMPLETED, mockBooking_Renting.getStatus());
            assertEquals(650000.0, mockBooking_Renting.getRefund());

            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(1)).save(transactionCaptor.capture());

            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            nf.setMaximumFractionDigits(0);

            assertEquals(-650000.0, transactionCaptor.getValue().getAmount());
            assertTrue(result.get("message").toString().contains(nf.format(650_000) + " VNĐ"));
        }
    }

    @Test
    void testConfirmFinalPayment_Success_WithDiscount() throws IOException {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        mockBooking_Renting.setFinalFee(50000.0);
        when(transactionDetailRepository.findTotalDiscountByBooking(any(Booking.class))).thenReturn(-20000.0);
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(mockCheckInHistory);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            Map<String, Object> result = paymentService.confirmFinalPayment(99L, req, mockStaff);

            ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository, times(1)).save(transactionCaptor.capture());

            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            nf.setMaximumFractionDigits(0);

            assertEquals(-670000.0, transactionCaptor.getValue().getAmount());
            assertTrue(result.get("message").toString().contains(nf.format(670_000) + " VNĐ"));
        }
    }

    @Test
    void testConfirmFinalPayment_Success_NoHistory() throws IOException {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        mockBooking_Renting.setFinalFee(50000.0);
        when(transactionDetailRepository.findTotalDiscountByBooking(any(Booking.class))).thenReturn(null);
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(null);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            paymentService.confirmFinalPayment(99L, req, mockStaff);

            verify(vehicleService, times(1)).recordVehicleAction(anyLong(), anyLong(), anyLong(), anyLong(), eq(VehicleActionType.RETURN), anyString(), eq("Không rõ (Lỗi không tìm thấy lịch sử check-in)"), eq("GOOD"), anyDouble(), anyDouble(), anyString());
        }
    }

    @Test
    void testConfirmFinalPayment_Success_NoSettlement() throws IOException {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        mockBooking_Renting.setFinalFee(700000.0); // Total due equals total deposit
        when(transactionDetailRepository.findTotalDiscountByBooking(any(Booking.class))).thenReturn(0.0);
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(mockCheckInHistory);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            Map<String, Object> result = paymentService.confirmFinalPayment(99L, req, mockStaff);

            assertEquals(BookingStatus.COMPLETED, mockBooking_Renting.getStatus());
            verify(transactionRepository, never()).save(any(Transaction.class));
            assertTrue(result.get("message").toString().contains("quyết toán thành công (không phát sinh)"));
        }
    }

    @Test
    void testConfirmFinalPayment_Fail_StaffInactive() {
        mockStaff.setStatus(AccountStatus.INACTIVE);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmFinalPayment(99L, new PaymentConfirmationRequest(), mockStaff)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmFinalPayment_Fail_WrongStation() {
        mockStaff.setStation(Station.builder().stationId(2L).build());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmFinalPayment(99L, new PaymentConfirmationRequest(), mockStaff)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testConfirmFinalPayment_Fail_NoCheckoutPhotos() {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        req.setConfirmPhotos(null);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.confirmFinalPayment(99L, req, mockStaff)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ảnh check-out (lúc trả xe) là bắt buộc"));
    }


    @Test
    void testConfirmFinalPayment_Fail_ObjectMapperError() throws Exception {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(mockCheckInHistory);
        doThrow(new JsonProcessingException("Lỗi JSON") {}).when(objectMapper).writeValueAsString(any());

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    paymentService.confirmFinalPayment(99L, req, mockStaff)
            );
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Lỗi xử lý ảnh check-out"));
        }
    }

    @Test
    void testConfirmFinalPayment_Fail_RecordActionError() throws IOException {
        PaymentConfirmationRequest req = setupMockRequestForConfirm();
        mockBooking_Renting.setFinalFee(50000.0);
        when(transactionDetailRepository.findTotalDiscountByBooking(any(Booking.class))).thenReturn(0.0);
        when(historyRepository.findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(any(), any(), eq(VehicleActionType.DELIVERY)))
                .thenReturn(mockCheckInHistory);
        doThrow(new RuntimeException("Lỗi DB khi ghi lịch sử"))
                .when(vehicleService).recordVehicleAction(anyLong(), anyLong(), anyLong(), anyLong(), any(), anyString(), anyString(), anyString(), anyDouble(), anyDouble(), anyString());

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            assertDoesNotThrow(() -> {
                paymentService.confirmFinalPayment(99L, req, mockStaff);
            });

            verify(transactionRepository, times(1)).save(any(Transaction.class));
            assertEquals(BookingStatus.COMPLETED, mockBooking_Renting.getStatus());
        }
    }

    @Test
    void testCreateTransaction_Success() {
        paymentService.createTransaction(mockBooking_Renting, 100000.0, PaymentMethod.CASH, mockStaff, "Test transaction");
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(1)).save(transactionCaptor.capture());
        Transaction savedTransaction = transactionCaptor.getValue();
        assertEquals(mockBooking_Renting, savedTransaction.getBooking());
        assertEquals(100000.0, savedTransaction.getAmount());
        assertEquals(PaymentMethod.CASH, savedTransaction.getPaymentMethod());
        assertEquals(mockStaff, savedTransaction.getStaff());
        assertEquals("Test transaction", savedTransaction.getStaffNote());
    }

    @Test
    void testGetAllPenaltyFees_Success() {
        PenaltyFee fee1 = PenaltyFee.builder().feeId(1L).isAdjustment(false).build();
        PenaltyFee fee2 = PenaltyFee.builder().feeId(2L).isAdjustment(null).build();
        PenaltyFee fee3 = PenaltyFee.builder().feeId(3L).isAdjustment(true).build();
        when(penaltyFeeRepository.findAll()).thenReturn(List.of(fee1, fee2, fee3));

        List<PenaltyFee> result = paymentService.getAllPenaltyFees();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(f -> f.getFeeId() == 1L));
        assertTrue(result.stream().anyMatch(f -> f.getFeeId() == 2L));
        assertFalse(result.stream().anyMatch(f -> f.getFeeId() == 3L));
    }

    @Test
    void testSaveHandoverPhoto_Fail_FileNull() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.saveHandoverPhoto(null, 1L, "checkout", 1)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ảnh bàn giao không được để trống"));
    }

    @Test
    void testSaveHandoverPhoto_Fail_FileEmpty() {
        when(mockPhoto.isEmpty()).thenReturn(true);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                paymentService.saveHandoverPhoto(mockPhoto, 1L, "checkout", 1)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Ảnh bàn giao không được để trống"));
    }

    @Test
    void testSaveHandoverPhoto_Fail_DirectoryCreationError() {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenThrow(new IOException("Lỗi tạo thư mục"));
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    paymentService.saveHandoverPhoto(mockPhoto, 1L, "checkout", 1)
            );
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Lỗi hệ thống khi lưu ảnh"));
        }
    }

    @Test
    void testSaveHandoverPhoto_Fail_FileTransferError() throws IOException {
        doThrow(new IOException("Lỗi ghi file")).when(mockPhoto).transferTo(any(Path.class));
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    paymentService.saveHandoverPhoto(mockPhoto, 1L, "checkout", 1)
            );
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Lỗi hệ thống khi lưu ảnh"));
        }
    }
}