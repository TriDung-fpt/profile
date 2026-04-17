package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.BillResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.InvoiceSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.service.util.EmailService;
import com.fptu.evstation.rental.evrentalsystem.service.util.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // --- Mock Data ---
    private User mockStaff;
    private User mockRenter;
    private Station mockStation;
    private Model mockModel;
    private Vehicle mockVehicle;
    private Booking mockBooking;
    private BillResponse mockBillResponse;

    @BeforeEach
    void setUp() {
        mockStation = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .build();

        mockModel = Model.builder()
                .modelId(1L)
                .modelName("VinFast VF 3")
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
                .build();

        mockStaff = User.builder()
                .userId(10L)
                .fullName("Trần Văn B")
                .station(mockStation) // Staff assigned to Station 1
                .build();

        mockBooking = Booking.builder()
                .bookingId(1L)
                .user(mockRenter)
                .vehicle(mockVehicle)
                .station(mockStation)
                .finalFee(200000.0)
                .createdAt(LocalDateTime.now())
                .build();

        mockBillResponse = BillResponse.builder()
                .bookingId(1L)
                .userName("Nguyễn Văn A")
                .baseRentalFee(200000.0)
                .paymentDue(200000.0)
                .downpayPaid(200000.0)
                .refundToCustomer(0.0)
                .build();
    }

    // --- Test 1: generateAndSaveInvoicePdf ---

    @Test
    void testGenerateAndSaveInvoicePdf_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            doNothing().when(pdfGenerationService).generateInvoicePdf(any(Path.class), eq(mockBillResponse));
            doNothing().when(emailService).sendInvoiceWithAttachment(anyString(), anyString(), anyString(), any(File.class));

            String result = invoiceService.generateAndSaveInvoicePdf(mockBillResponse);

            assertNotNull(result);
            assertTrue(result.contains("/uploads/invoices/"));
            assertTrue(result.contains("HoaDon_Nguyen_Van_A_Booking_1.pdf"));

            verify(bookingRepository, times(1)).findById(1L);
            mockedFiles.verify(() -> Files.createDirectories(any(Path.class)), times(1));
            verify(pdfGenerationService, times(1)).generateInvoicePdf(any(Path.class), eq(mockBillResponse));
            verify(bookingRepository, times(1)).save(any(Booking.class));
            verify(emailService, times(1)).sendInvoiceWithAttachment(
                    eq("renter@test.com"),
                    eq("Nguyễn Văn A"),
                    eq("HD1"),
                    any(File.class)
            );
        }
    }

    @Test
    void testGenerateAndSaveInvoicePdf_Success_NormalizeVietnameseName() {
        mockRenter.setFullName("Trần Thị Hương");
        mockBooking.setUser(mockRenter);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            doNothing().when(pdfGenerationService).generateInvoicePdf(any(), any());
            doNothing().when(emailService).sendInvoiceWithAttachment(any(), any(), any(), any());

            String result = invoiceService.generateAndSaveInvoicePdf(mockBillResponse);

            assertNotNull(result);
            assertTrue(result.contains("HoaDon_Tran_Thi_Huong_Booking_1.pdf"));
        }
    }

    @Test
    void testGenerateAndSaveInvoicePdf_Fail_BookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        // Assert that a NoSuchElementException is thrown, as per the orElseThrow() in the service
        assertThrows(NoSuchElementException.class, () -> {
            invoiceService.generateAndSaveInvoicePdf(mockBillResponse);
        });

        // Verify that no further actions were taken
        verify(pdfGenerationService, never()).generateInvoicePdf(any(), any());
        verify(emailService, never()).sendInvoiceWithAttachment(any(), any(), any(), any());
    }

    @Test
    void testGenerateAndSaveInvoicePdf_Fail_DirectoryCreationError() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class)))
                    .thenThrow(new IOException("Không thể tạo thư mục"));

            // The service is expected to catch the IOException, log it, and return null.
            // The error log in the console is an expected outcome of this test.
            String result = invoiceService.generateAndSaveInvoicePdf(mockBillResponse);

            assertNull(result);
            verify(pdfGenerationService, never()).generateInvoicePdf(any(), any());
            verify(emailService, never()).sendInvoiceWithAttachment(any(), any(), any(), any());
        }
    }

    @Test
    void testGenerateAndSaveInvoicePdf_Fail_PdfGenerationError() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            // Stubbing with a RuntimeException because the mocked method does not throw IOException
            doThrow(new RuntimeException("PDF Generation Failed"))
                    .when(pdfGenerationService).generateInvoicePdf(any(), eq(mockBillResponse));

            // The service should re-throw the RuntimeException
            assertThrows(RuntimeException.class, () -> {
                invoiceService.generateAndSaveInvoicePdf(mockBillResponse);
            });

            verify(emailService, never()).sendInvoiceWithAttachment(any(), any(), any(), any());
        }
    }

    @Test
    void testGenerateAndSaveInvoicePdf_Fail_EmailError_RuntimeException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(mockBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(mockBooking);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            doNothing().when(pdfGenerationService).generateInvoicePdf(any(), any());
            doThrow(new RuntimeException("Lỗi máy chủ mail"))
                    .when(emailService).sendInvoiceWithAttachment(anyString(), anyString(), anyString(), any(File.class));

            assertThrows(RuntimeException.class, () -> {
                invoiceService.generateAndSaveInvoicePdf(mockBillResponse);
            }, "Lỗi máy chủ mail");

            verify(bookingRepository, times(1)).findById(1L);
            verify(pdfGenerationService, times(1)).generateInvoicePdf(any(), any());
            verify(bookingRepository, times(1)).save(any(Booking.class));
            verify(emailService, times(1)).sendInvoiceWithAttachment(any(), any(), any(), any());
        }
    }

    // --- Test 2: getAllInvoicesByStation ---

    @Test
    void testGetAllInvoicesByStation_Success() {
        Booking booking1 = Booking.builder()
                .bookingId(1L)
                .user(mockRenter)
                .station(mockStation)
                .finalFee(200000.0)
                .createdAt(LocalDateTime.now())
                .invoicePdfPath("/path/invoice1.pdf")
                .build();
        when(bookingRepository.findAllByInvoicePdfPathIsNotNullAndStation(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(booking1));

        List<InvoiceSummaryResponse> result = invoiceService.getAllInvoicesByStation(mockStaff);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getBookingId());
        assertEquals(200000.0, result.get(0).getFinalAmount());
        assertEquals("Nguyễn Văn A", result.get(0).getRenterName());
        assertEquals("/path/invoice1.pdf", result.get(0).getInvoicePdfPath());

        verify(bookingRepository, times(1)).findAllByInvoicePdfPathIsNotNullAndStation(eq(mockStation), any(Sort.class));
    }

    @Test
    void testGetAllInvoicesByStation_Success_EmptyList() {
        when(bookingRepository.findAllByInvoicePdfPathIsNotNullAndStation(eq(mockStation), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<InvoiceSummaryResponse> result = invoiceService.getAllInvoicesByStation(mockStaff);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(bookingRepository, times(1)).findAllByInvoicePdfPathIsNotNullAndStation(eq(mockStation), any(Sort.class));
    }

    @Test
    void testGetAllInvoicesByStation_Fail_StaffNotAssignedToStation() {
        mockStaff.setStation(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.getAllInvoicesByStation(mockStaff)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Nhân viên chưa được gán cho trạm nào"));

        verify(bookingRepository, never()).findAllByInvoicePdfPathIsNotNullAndStation(any(), any());
    }
}