package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.ReportResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.TransactionDetailRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionDetailRepository transactionDetailRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    // --- Biến Mock Data ---
    private Station mockStation;
    private Model mockModel;
    private Vehicle mockVehicle;
    private User mockRenter;
    private Booking mockBooking;
    private Transaction mockTransaction;
    private TransactionDetail mockTransactionDetail;

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

        mockBooking = Booking.builder()
                .bookingId(1L)
                .user(mockRenter)
                .vehicle(mockVehicle)
                .station(mockStation)
                .createdAt(LocalDateTime.now()) // Đảm bảo booking có vehicle và station
                .build();

        mockTransaction = Transaction.builder()
                .transactionId(1L)
                .booking(mockBooking) // Gán booking vào transaction
                .amount(200000.0)
                .transactionDate(LocalDateTime.now())
                .build();

        mockTransactionDetail = TransactionDetail.builder()
                .detailId(1L)
                .booking(mockBooking) // Gán booking vào detail
                .appliedAmount(50000.0)
                .build();
    }

    @Test
    void testGetRevenueByStation_Success() {
        // 1. Chuẩn bị
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        when(transactionRepository.findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(eq(1L), eq(from), eq(to)))
                .thenReturn(List.of(mockTransaction));

        when(transactionDetailRepository.findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(eq(1L), eq(from), eq(to)))
                .thenReturn(List.of(mockTransactionDetail));

        // 2. Thực thi
        ReportResponse result = reportService.getRevenueByStation(1L, fromDate, toDate);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(1L, result.getStationId());
        assertEquals("Trạm Evolve - Quận 1", result.getStationName()); // Test nhánh (else)
        assertEquals(200000.0, result.getTotalBookingRevenue());
        assertEquals(50000.0, result.getTotalPenaltyRevenue());
        assertEquals(250000.0, result.getTotalRevenue());
        assertEquals(1, result.getTotalTransactions());

        verify(transactionRepository, times(1)).findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(transactionDetailRepository, times(1)).findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetRevenueByStation_Success_NoTransactions() {
        // 1. Chuẩn bị
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        // Trả về danh sách rỗng
        when(transactionRepository.findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(eq(1L), eq(from), eq(to)))
                .thenReturn(List.of());

        when(transactionDetailRepository.findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(eq(1L), eq(from), eq(to)))
                .thenReturn(List.of());

        // 2. Thực thi
        ReportResponse result = reportService.getRevenueByStation(1L, fromDate, toDate);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals("Không có giao dịch", result.getStationName()); // Test nhánh (if)
        assertEquals(0.0, result.getTotalBookingRevenue());
        assertEquals(0.0, result.getTotalPenaltyRevenue());
        assertEquals(0.0, result.getTotalRevenue());
        assertEquals(0, result.getTotalTransactions());
    }

    @Test
    void testGetRevenueByStation_Success_MultipleTransactions() {
        // 1. Chuẩn bị
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();

        Transaction transaction2 = Transaction.builder()
                .transactionId(2L)
                .booking(mockBooking)
                .amount(150000.0) // Thêm 150k
                .transactionDate(LocalDateTime.now().minusDays(1))
                .build();

        TransactionDetail detail2 = TransactionDetail.builder()
                .detailId(2L)
                .booking(mockBooking)
                .appliedAmount(30000.0) // Thêm 30k
                .build();

        when(transactionRepository.findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockTransaction, transaction2));

        when(transactionDetailRepository.findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockTransactionDetail, detail2));

        // 2. Thực thi
        ReportResponse result = reportService.getRevenueByStation(1L, fromDate, toDate);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(350000.0, result.getTotalBookingRevenue()); // 200k + 150k
        assertEquals(80000.0, result.getTotalPenaltyRevenue()); // 50k + 30k
        assertEquals(430000.0, result.getTotalRevenue()); // 350k + 80k
        assertEquals(2, result.getTotalTransactions());
    }

    @Test
    void testGetTotalRevenue_Success() {
        // 1. Chuẩn bị
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        when(transactionRepository.findByTransactionDateBetween(eq(from), eq(to)))
                .thenReturn(List.of(mockTransaction));

        when(transactionDetailRepository.findByBooking_CreatedAtBetween(eq(from), eq(to)))
                .thenReturn(List.of(mockTransactionDetail));

        // 2. Thực thi
        ReportResponse result = reportService.getTotalRevenue(fromDate, toDate);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals("Tất cả trạm", result.getStationName());
        assertEquals(200000.0, result.getTotalBookingRevenue());
        assertEquals(50000.0, result.getTotalPenaltyRevenue());
        assertEquals(250000.0, result.getTotalRevenue());
        assertEquals(1, result.getTotalTransactions());

        verify(transactionRepository, times(1)).findByTransactionDateBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(transactionDetailRepository, times(1)).findByBooking_CreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void testGetTotalRevenue_Success_NoTransactions() {
        // 1. Chuẩn bị
        LocalDate fromDate = LocalDate.now().minusDays(7);
        LocalDate toDate = LocalDate.now();
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);

        when(transactionRepository.findByTransactionDateBetween(eq(from), eq(to)))
                .thenReturn(List.of());

        when(transactionDetailRepository.findByBooking_CreatedAtBetween(eq(from), eq(to)))
                .thenReturn(List.of());

        // 2. Thực thi
        ReportResponse result = reportService.getTotalRevenue(fromDate, toDate);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals("Tất cả trạm", result.getStationName());
        assertEquals(0.0, result.getTotalBookingRevenue());
        assertEquals(0.0, result.getTotalPenaltyRevenue());
        assertEquals(0.0, result.getTotalRevenue());
        assertEquals(0, result.getTotalTransactions());
    }

    @Test
    void testGetTotalRevenue_Success_MultipleTransactions() {
        // 1. Chuẩn bị
        LocalDate fromDate = LocalDate.now().minusDays(30);
        LocalDate toDate = LocalDate.now();

        Transaction transaction2 = Transaction.builder()
                .transactionId(2L)
                .booking(mockBooking)
                .amount(300000.0) // 300k
                .transactionDate(LocalDateTime.now().minusDays(5))
                .build();

        Transaction transaction3 = Transaction.builder()
                .transactionId(3L)
                .booking(mockBooking)
                .amount(250000.0) // 250k
                .transactionDate(LocalDateTime.now().minusDays(10))
                .build();

        when(transactionRepository.findByTransactionDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockTransaction, transaction2, transaction3)); // 200k + 300k + 250k = 750k

        when(transactionDetailRepository.findByBooking_CreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(mockTransactionDetail)); // 50k

        // 2. Thực thi
        ReportResponse result = reportService.getTotalRevenue(fromDate, toDate);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(750000.0, result.getTotalBookingRevenue());
        assertEquals(50000.0, result.getTotalPenaltyRevenue());
        assertEquals(800000.0, result.getTotalRevenue());
        assertEquals(3, result.getTotalTransactions());
    }

    @Test
    void testGetRevenueByStation_DateRangeCorrectness() {
        // Test này chủ yếu để đảm bảo DTO được điền đúng
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);

        when(transactionRepository.findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        when(transactionDetailRepository.findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        ReportResponse result = reportService.getRevenueByStation(1L, fromDate, toDate);

        assertNotNull(result);
        assertEquals(fromDate, result.getFromDate());
        assertEquals(toDate, result.getToDate());
    }

    @Test
    void testGetTotalRevenue_DateRangeCorrectness() {
        // Test này chủ yếu để đảm bảo DTO được điền đúng
        LocalDate fromDate = LocalDate.of(2024, 1, 1);
        LocalDate toDate = LocalDate.of(2024, 1, 31);

        when(transactionRepository.findByTransactionDateBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        when(transactionDetailRepository.findByBooking_CreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        ReportResponse result = reportService.getTotalRevenue(fromDate, toDate);

        assertNotNull(result);
        assertEquals(fromDate, result.getFromDate());
        assertEquals(toDate, result.getToDate());
    }
}