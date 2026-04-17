package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.ContractSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.ContractRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.TransactionRepository;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PdfGenerationService pdfGenerationService;

    @InjectMocks
    private ContractServiceImpl contractService;

    // --- Biến Mock Data ---
    private User mockStaff;
    private User mockRenter;
    private Station mockStation;
    private Model mockModel;
    private Vehicle mockVehicle;
    private Booking mockBooking;

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
                .station(mockStation) // Staff được gán vào Trạm 1
                .build();

        mockBooking = Booking.builder()
                .bookingId(1L)
                .user(mockRenter)
                .vehicle(mockVehicle)
                .station(mockStation)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusHours(8))
                .build();
    }

    // --- Test 1: generateAndSaveContract ---

    @Test
    void testGenerateAndSaveContract_Success() {
        // 1. Chuẩn bị
        // Giả lập ContractRepository.save()
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
            Contract contract = invocation.getArgument(0);
            contract.setContractId(1L); // Giả lập việc DB gán ID
            return contract;
        });

        // 2. Thực thi (SỬA LỖI: Dùng mockStatic cho Files.createDirectories)
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Giả lập Files.createDirectories() không làm gì và không báo lỗi
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);

            // Giả lập PdfGenerationService.generateContractPdf() không báo lỗi
            doNothing().when(pdfGenerationService).generateContractPdf(any(Path.class), eq(mockBooking), eq(mockStaff));

            Contract result = contractService.generateAndSaveContract(mockBooking, mockStaff);

            // 3. Xác minh
            assertNotNull(result);
            assertEquals(1L, result.getContractId());
            assertNotNull(result.getContractPdfPath());
            assertEquals(mockBooking, result.getBooking());
            assertNotNull(result.getSignedDate());
            assertEquals("Điều khoản dịch vụ phiên bản 1.4 được áp dụng.", result.getTermsSnapshot());
            assertTrue(result.getContractPdfPath().contains("HopDong_Nguyen_Van_A_Booking_1.pdf"));

            // Xác minh các mock đã được gọi
            verify(contractRepository, times(1)).save(any(Contract.class));
            verify(pdfGenerationService, times(1)).generateContractPdf(any(Path.class), eq(mockBooking), eq(mockStaff));
            mockedFiles.verify(() -> Files.createDirectories(any(Path.class)), times(1));

        }
    }

    @Test
    void testGenerateAndSaveContract_Success_NormalizeVietnameseName() {
        // Test trường hợp tên có dấu
        mockRenter.setFullName("Trần Thị Hương");

        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            doNothing().when(pdfGenerationService).generateContractPdf(any(Path.class), eq(mockBooking), eq(mockStaff));

            Contract result = contractService.generateAndSaveContract(mockBooking, mockStaff);

            assertNotNull(result);
            // Xác minh tên đã được chuẩn hóa (loại bỏ dấu và thay bằng '_')
            assertTrue(result.getContractPdfPath().contains("HopDong_Tran_Thi_Huong_Booking_1.pdf"));
            verify(contractRepository, times(1)).save(any(Contract.class));
        }
    }

    @Test
    void testGenerateAndSaveContract_Fail_FilesCreateDirectoriesError() {
        // Test trường hợp lỗi (IOException) khi tạo thư mục
        // 1. Chuẩn bị
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            // Giả lập Files.createDirectories() ném ra IOException
            mockedFiles.when(() -> Files.createDirectories(any(Path.class)))
                    .thenThrow(new IOException("Không thể tạo thư mục"));

            // 2. Thực thi & 3. Xác minh
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    contractService.generateAndSaveContract(mockBooking, mockStaff)
            );

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Lỗi hệ thống khi tạo Hợp đồng PDF"));

            // Xác minh các service khác không được gọi
            verify(pdfGenerationService, never()).generateContractPdf(any(), any(), any());
            verify(contractRepository, never()).save(any());
        }
    }

    @Test
    void testGenerateAndSaveContract_Fail_PdfGenerationError() {
        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class))).thenReturn(null);
            doThrow(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể tạo file PDF hợp đồng."
            )).when(pdfGenerationService)
                    .generateContractPdf(any(Path.class), any(Booking.class), any(User.class));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    contractService.generateAndSaveContract(mockBooking, mockStaff)
            );
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
            String reason = ex.getReason();
            assertTrue(reason != null && (
                    reason.contains("Không thể tạo file PDF hợp đồng")
                            || reason.contains("Lỗi hệ thống khi tạo Hợp đồng PDF")
            ), "Reason phải chứa thông điệp lỗi PDF");
            verify(contractRepository, never()).save(any());
        }
    }

    // --- Test 2: getAllContractsByStation ---

    @Test
    void testGetAllContractsByStation_Success() {
        // 1. Chuẩn bị
        Contract contract = Contract.builder()
                .contractId(1L)
                .booking(mockBooking)
                .contractPdfPath("/path/contract.pdf")
                .signedDate(LocalDateTime.now())
                .build();

        Transaction staffTransaction = Transaction.builder()
                .staff(mockStaff) // Staff Trần Văn B
                .staffNote("Thu cọc thuê xe 2%") // Ghi chú khớp
                .build();

        when(contractRepository.findByBooking_Station(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(contract));

        when(transactionRepository.findByBooking(mockBooking))
                .thenReturn(List.of(staffTransaction));

        // 2. Thực thi
        List<ContractSummaryResponse> result = contractService.getAllContractsByStation(mockStaff);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nguyễn Văn A", result.get(0).getRenterName());
        assertEquals("51A-12345", result.get(0).getVehicleLicensePlate());
        assertEquals("Trần Văn B", result.get(0).getStaffName()); // Lấy được tên Staff

        verify(contractRepository, times(1)).findByBooking_Station(eq(mockStation), any(Sort.class));
        verify(transactionRepository, times(1)).findByBooking(mockBooking);
    }

    @Test
    void testGetAllContractsByStation_Success_NoMatchingStaffNote() {
        // Test trường hợp tìm thấy transaction, nhưng staffNote không khớp
        // 1. Chuẩn bị
        Contract contract = Contract.builder().contractId(1L).booking(mockBooking).build();
        Transaction otherTransaction = Transaction.builder()
                .staff(mockStaff)
                .staffNote("Ghi chú khác") // Ghi chú không khớp
                .build();

        when(contractRepository.findByBooking_Station(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(contract));

        when(transactionRepository.findByBooking(mockBooking))
                .thenReturn(List.of(otherTransaction)); // Trả về transaction

        // 2. Thực thi
        List<ContractSummaryResponse> result = contractService.getAllContractsByStation(mockStaff);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("N/A", result.get(0).getStaffName()); // Tên staff là "N/A" vì filter thất bại

        verify(contractRepository, times(1)).findByBooking_Station(eq(mockStation), any(Sort.class));
        verify(transactionRepository, times(1)).findByBooking(mockBooking);
    }

    @Test
    void testGetAllContractsByStation_Success_NoTransactionFound() {
        // Test trường hợp không tìm thấy transaction nào (orElse("N/A"))
        // 1. Chuẩn bị
        Contract contract = Contract.builder().contractId(1L).booking(mockBooking).build();

        when(contractRepository.findByBooking_Station(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(contract));

        when(transactionRepository.findByBooking(mockBooking))
                .thenReturn(Collections.emptyList()); // Không có transaction

        // 2. Thực thi
        List<ContractSummaryResponse> result = contractService.getAllContractsByStation(mockStaff);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("N/A", result.get(0).getStaffName()); // Tên staff là "N/A"

        verify(contractRepository, times(1)).findByBooking_Station(eq(mockStation), any(Sort.class));
        verify(transactionRepository, times(1)).findByBooking(mockBooking);
    }

    @Test
    void testGetAllContractsByStation_Fail_StaffNotAssignedToStation() {
        // Test guard clause (if (staff.getStation() == null))
        // 1. Chuẩn bị
        mockStaff.setStation(null); // Staff không thuộc trạm nào

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                contractService.getAllContractsByStation(mockStaff)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Nhân viên chưa được gán cho trạm nào"));

        // Xác minh repository không được gọi
        verify(contractRepository, never()).findByBooking_Station(any(), any());
    }

    @Test
    void testGetAllContractsByStation_Success_EmptyList() {
        // Test trường hợp không có hợp đồng nào
        // 1. Chuẩn bị
        when(contractRepository.findByBooking_Station(eq(mockStation), any(Sort.class)))
                .thenReturn(Collections.emptyList()); // Trả về danh sách rỗng

        // 2. Thực thi
        List<ContractSummaryResponse> result = contractService.getAllContractsByStation(mockStaff);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(0, result.size()); // Danh sách rỗng

        verify(contractRepository, times(1)).findByBooking_Station(eq(mockStation), any(Sort.class));
        verify(transactionRepository, never()).findByBooking(any()); // Không gọi khi list rỗng
    }
}