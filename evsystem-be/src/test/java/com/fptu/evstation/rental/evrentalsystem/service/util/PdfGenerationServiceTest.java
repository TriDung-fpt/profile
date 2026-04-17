package com.fptu.evstation.rental.evrentalsystem.service.util;

import com.fptu.evstation.rental.evrentalsystem.dto.BillResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.PenaltyFeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfGenerationServiceTest {

    @Mock
    private PenaltyFeeRepository penaltyFeeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PdfGenerationService pdfGenerationService;

    private User mockStaff;
    private User mockRenter;
    private Station mockStation;
    private Model mockModel;
    private Vehicle mockVehicle;
    private Booking mockBooking;
    private Path testContractPath;
    private Path testInvoicePath;

    private final String VALID_QR_CODE_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";


    @BeforeEach
    void setUp() throws IOException {
        mockStation = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .address("123 Nguyễn Huệ, Quận 1, TP.HCM")
                .build();

        mockModel = Model.builder()
                .modelId(1L)
                .modelName("VinFast VF 3")
                .vehicleType(VehicleType.CAR)
                .pricePerHour(25000.0)
                .initialValue(10000000.0)
                .build();

        mockVehicle = Vehicle.builder()
                .vehicleId(1L)
                .licensePlate("51A-12345")
                .station(mockStation)
                .model(mockModel)
                .build();

        mockRenter = User.builder()
                .userId(1L)
                .email("renter@test.com")
                .fullName("Nguyễn Văn A")
                .phone("0901234567")
                .cccd("123456789")
                .gplx("987654321")
                .build();

        mockStaff = User.builder()
                .userId(10L)
                .fullName("Trần Văn B")
                .station(mockStation)
                .build();

        mockBooking = Booking.builder()
                .bookingId(1L)
                .user(mockRenter)
                .vehicle(mockVehicle)
                .station(mockStation)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(8))
                .rentalDeposit(200000.0)
                .finalFee(200000.0)
                .build();

        testContractPath = Paths.get(System.getProperty("java.io.tmpdir"), "test_contract.pdf");
        testInvoicePath = Paths.get(System.getProperty("java.io.tmpdir"), "test_invoice.pdf");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Xóa file sau mỗi test
        Files.deleteIfExists(testContractPath);
        Files.deleteIfExists(testInvoicePath);
    }

    // --- generateContractPdf Tests ---

    @Test
    void testGenerateContractPdf_Success_NoPenaltyFees() throws IOException {
        when(penaltyFeeRepository.findByIsAdjustmentIsFalse()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> pdfGenerationService.generateContractPdf(testContractPath, mockBooking, mockStaff));

        assertTrue(Files.exists(testContractPath));
        assertTrue(Files.size(testContractPath) > 0);
    }

    @Test
    void testGenerateContractPdf_Success_WithPenaltyFees() throws IOException {
        PenaltyFee fee1 = PenaltyFee.builder().feeName("Phí vệ sinh").fixedAmount(100000.0).isAdjustment(false).build();
        PenaltyFee fee2 = PenaltyFee.builder().feeName("Phí hư hỏng nhẹ").fixedAmount(200000.0).isAdjustment(false).build();

        when(penaltyFeeRepository.findByIsAdjustmentIsFalse()).thenReturn(List.of(fee1, fee2));

        assertDoesNotThrow(() -> pdfGenerationService.generateContractPdf(testContractPath, mockBooking, mockStaff));

        assertTrue(Files.exists(testContractPath));
        assertTrue(Files.size(testContractPath) > 0);
    }

    @Test
    void testGenerateContractPdf_Fail_IOException() {
        Path invalidPath = Paths.get("/invalid/directory/that/does/not/exist/contract.pdf");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pdfGenerationService.generateContractPdf(invalidPath, mockBooking, mockStaff)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể tạo file PDF hợp đồng"));
    }

    // --- generateInvoicePdf Tests ---

    // Bên trong file: PdfGenerationServiceTest.java

    @Test
    void testGenerateInvoicePdf_Success_PaymentDue_NoFees_NoQr() throws IOException {
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(200000.0)
                .downpayPaid(0.0)
                .refundToCustomer(0.0)
                .totalPenaltyFee(0.0)
                .totalDiscount(0.0)
                .feeItems(Collections.emptyList()) // <-- SỬA DÒNG NÀY
                .qrCodeUrl(null)
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));

        assertTrue(Files.exists(testInvoicePath));
        assertTrue(Files.size(testInvoicePath) > 0);
    }

    @Test
    void testGenerateInvoicePdf_Success_WithPositiveFees() throws IOException {
        BillResponse.FeeItem feeItem = BillResponse.FeeItem.builder().feeName("Phí vệ sinh").amount(100000.0).build();

        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(100000.0)
                .downpayPaid(200000.0)
                .totalPenaltyFee(100000.0)
                .refundToCustomer(0.0)
                .totalDiscount(0.0)
                .feeItems(List.of(feeItem))
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));
        assertTrue(Files.exists(testInvoicePath));
    }

    @Test
    void testGenerateInvoicePdf_Success_WithNegativeFees_Discount() throws IOException {
        BillResponse.FeeItem feeItem = BillResponse.FeeItem.builder().feeName("Khuyến mãi").amount(-50000.0).build();

        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(150000.0)
                .downpayPaid(0.0)
                .totalPenaltyFee(0.0)
                .totalDiscount(50000.0)
                .refundToCustomer(0.0)
                .feeItems(List.of(feeItem))
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));
        assertTrue(Files.exists(testInvoicePath));
    }

    @Test
    void testGenerateInvoicePdf_Success_WithRefund() throws IOException {
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(100000.0)
                .paymentDue(0.0)
                .downpayPaid(200000.0)
                .refundToCustomer(100000.0)
                .totalPenaltyFee(0.0)
                .totalDiscount(0.0)
                .feeItems(Collections.emptyList())
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));
        assertTrue(Files.exists(testInvoicePath));
    }

    @Test
    void testGenerateInvoicePdf_Success_ZeroBalance() throws IOException {
        // Test kịch bản Hoàn tất, không phát sinh (else)
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(0.0)
                .downpayPaid(200000.0)
                .refundToCustomer(0.0)
                .totalPenaltyFee(0.0)
                .totalDiscount(0.0)
                .feeItems(Collections.emptyList())
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));
        assertTrue(Files.exists(testInvoicePath));
    }

    // Bên trong file: PdfGenerationServiceTest.java

    @Test
    void testGenerateInvoicePdf_Success_WithQrCode() throws IOException {
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(200000.0)
                .downpayPaid(0.0)
                .refundToCustomer(0.0)
                .totalPenaltyFee(0.0)
                .totalDiscount(0.0)
                .feeItems(Collections.emptyList()) // <-- SỬA DÒNG NÀY
                .qrCodeUrl(VALID_QR_CODE_BASE64)
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));
        assertTrue(Files.exists(testInvoicePath));
    }
    // Bên trong file: PdfGenerationServiceTest.java

    @Test
    void testGenerateInvoicePdf_Success_NullDateTime() throws IOException {
        // Test kịch bản dateTime == null
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(200000.0)
                .downpayPaid(0.0)
                .refundToCustomer(0.0)
                .dateTime(null)
                .totalPenaltyFee(0.0)
                .feeItems(Collections.emptyList())
                .totalDiscount(0.0) // <-- THÊM DÒNG NÀY
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        assertDoesNotThrow(() -> pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse));
        assertTrue(Files.exists(testInvoicePath));
    }

    @Test
    void testGenerateInvoicePdf_Fail_BookingNotFound() {
        BillResponse billResponse = BillResponse.builder().bookingId(999L).build();

        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể tạo file PDF hóa đơn"));
    }

    @Test
    void testGenerateInvoicePdf_Fail_InvalidQrCode() {
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .paymentDue(100.0)
                .qrCodeUrl("data:image/png;base64,invalid-base64-string!")
                .dateTime(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pdfGenerationService.generateInvoicePdf(testInvoicePath, billResponse)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể tạo file PDF hóa đơn"));
    }

    @Test
    void testGenerateInvoicePdf_Fail_IOException() {
        // 1. Khởi tạo BillResponse
        // (LƯU Ý: Vẫn phải .build() đầy đủ để tránh lỗi NPE nếu sau này bạn đổi logic)
        BillResponse billResponse = BillResponse.builder()
                .bookingId(1L)
                .dateTime(LocalDateTime.now())
                .totalPenaltyFee(0.0) // Vẫn nên giữ
                .feeItems(Collections.emptyList()) // Vẫn nên giữ
                .totalDiscount(0.0) // Vẫn nên giữ
                .build();

        // 2. XÓA DÒNG NÀY ĐI
        // when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        // 3. Phần còn lại giữ nguyên
        Path invalidPath = Paths.get("/invalid/directory/that/does/not/exist/invoice.pdf");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                pdfGenerationService.generateInvoicePdf(invalidPath, billResponse)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể tạo file PDF hóa đơn"));
    }
}