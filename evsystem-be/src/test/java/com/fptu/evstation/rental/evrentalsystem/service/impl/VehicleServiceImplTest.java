package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.CreateVehicleRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.ReportDamageRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateVehicleDetailsRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.VehicleResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.VehicleHistoryResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleHistoryRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.service.ModelService;
import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleHistoryRepository historyRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StationService stationService;
    @Mock
    private ModelService modelService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    @InjectMocks
    private VehicleServiceImpl vehicleService;

    // --- Mock Data ---
    private Vehicle mockVehicle_Available;
    private Vehicle mockVehicle_Rented;
    private User mockStaff;
    private User mockRenter;
    private Station mockStation;
    private Model mockModel;
    private Booking mockBooking_Confirmed;
    private Booking mockBooking_Renting;

    @BeforeEach
    void setUp() {
        mockStation = Station.builder()
                .stationId(1L)
                .name("Station A")
                .build();

        mockModel = Model.builder()
                .modelId(1L)
                .modelName("VinFast VF e34")
                .vehicleType(VehicleType.CAR)
                .pricePerHour(100.0)
                .seatCount(5)
                .rangeKm(300.0)
                .features("AI")
                .description("Desc")
                .imagePaths("path/img1.jpg,path/img2.jpg")
                .build();

        mockStaff = User.builder()
                .userId(10L)
                .fullName("Test Staff")
                .status(AccountStatus.ACTIVE)
                .station(mockStation)
                .build();

        mockRenter = User.builder()
                .userId(1L)
                .fullName("Test Renter")
                .build();

        mockVehicle_Available = Vehicle.builder()
                .vehicleId(101L)
                .licensePlate("51A-11111")
                .status(VehicleStatus.AVAILABLE)
                .station(mockStation)
                .model(mockModel)
                .batteryLevel(90)
                .condition(VehicleCondition.GOOD)
                .currentMileage(1000.0)
                .createdAt(LocalDateTime.now())
                .build();

        mockVehicle_Rented = Vehicle.builder()
                .vehicleId(102L)
                .licensePlate("51A-22222")
                .status(VehicleStatus.RENTED)
                .station(mockStation)
                .model(mockModel)
                .build();

        mockBooking_Confirmed = Booking.builder()
                .bookingId(1L)
                .vehicle(mockVehicle_Available)
                .user(mockRenter)
                .status(BookingStatus.CONFIRMED)
                .build();

        mockBooking_Renting = Booking.builder()
                .bookingId(2L)
                .vehicle(mockVehicle_Rented)
                .user(mockRenter)
                .status(BookingStatus.RENTING)
                .build();
        ReflectionTestUtils.setField(vehicleService, "objectMapper", objectMapper);
    }

    // --- createVehicle ---

    @Test
    void testCreateVehicle_Success() {
        CreateVehicleRequest request = CreateVehicleRequest.builder()
                .licensePlate("51H-12345")
                .modelId(1L)
                .stationId(1L)
                .status("AVAILABLE")
                .condition("GOOD")
                .batteryLevel(100)
                .currentMileage(0.0)
                .build();

        when(vehicleRepository.existsByLicensePlate("51H-12345")).thenReturn(false);
        when(modelService.getModelById(1L)).thenReturn(mockModel);
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle v = invocation.getArgument(0);
            v.setVehicleId(103L);
            v.setCreatedAt(LocalDateTime.now());
            return v;
        });

        VehicleResponse createdVehicle = vehicleService.createVehicle(request);

        assertNotNull(createdVehicle);
        assertEquals("51H-12345", createdVehicle.getLicensePlate());
        assertEquals("VinFast VF e34", createdVehicle.getModelName());
        assertEquals("Station A", createdVehicle.getStationName());
        assertEquals(2, createdVehicle.getImagePaths().size());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void testCreateVehicle_Fail_LicensePlateExists() {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setLicensePlate("51H-12345");

        when(vehicleRepository.existsByLicensePlate("51H-12345")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            vehicleService.createVehicle(request);
        });
        assertEquals("Biển số xe đã tồn tại!", ex.getMessage());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // --- updateVehicle ---

    @Test
    void testUpdateVehicle_Success_AllFields() {
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest();
        request.setLicensePlate("51H-UPDATED");
        request.setCurrentMileage(2000.0);
        request.setBatteryLevel(80);
        request.setNewCondition(VehicleCondition.MINOR_DAMAGE);
        request.setStatus(VehicleStatus.UNAVAILABLE);
        request.setModelId(2L);
        request.setStationId(2L);

        Model newModel = Model.builder().modelId(2L).build();
        Station newStation = Station.builder().stationId(2L).build();

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(modelService.getModelById(2L)).thenReturn(newModel);
        when(stationService.getStationById(2L)).thenReturn(newStation);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(mockVehicle_Available);

        Vehicle updatedVehicle = vehicleService.updateVehicle(101L, request);

        assertNotNull(updatedVehicle);
        assertEquals("51H-UPDATED", updatedVehicle.getLicensePlate());
        assertEquals(80, updatedVehicle.getBatteryLevel());
        assertEquals(VehicleStatus.UNAVAILABLE, updatedVehicle.getStatus());
        assertEquals(newModel, updatedVehicle.getModel());
        assertEquals(newStation, updatedVehicle.getStation());
        verify(vehicleRepository, times(1)).save(mockVehicle_Available);
    }

    @Test
    void testUpdateVehicle_Success_PartialFieldsNull() {
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest();
        request.setLicensePlate("51H-PARTIAL"); // Only update license plate

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(mockVehicle_Available);

        Vehicle updatedVehicle = vehicleService.updateVehicle(101L, request);

        assertEquals("51H-PARTIAL", updatedVehicle.getLicensePlate());
        // Other fields remain unchanged
        assertEquals(90, updatedVehicle.getBatteryLevel());
        verify(vehicleRepository, times(1)).save(mockVehicle_Available);
    }

    @Test
    void testUpdateVehicle_Fail_NotFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.updateVehicle(999L, new UpdateVehicleDetailsRequest());
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- deleteVehicle ---

    @Test
    void testDeleteVehicle_Success_Available() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        doNothing().when(vehicleRepository).delete(mockVehicle_Available);

        assertDoesNotThrow(() -> vehicleService.deleteVehicle(101L));

        verify(vehicleRepository, times(1)).delete(mockVehicle_Available);
    }

    @Test
    void testDeleteVehicle_Fail_Rented() {
        when(vehicleRepository.findById(102L)).thenReturn(Optional.of(mockVehicle_Rented));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.deleteVehicle(102L);
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể xóa xe đang trong trạng thái 'RENTED'"));

        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    @Test
    void testDeleteVehicle_Fail_NotFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.deleteVehicle(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- getAllVehiclesByStation ---

    @Test
    void testGetAllVehiclesByStation_Success() {
        when(vehicleRepository.findByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(List.of(mockVehicle_Available));

        List<VehicleResponse> result = vehicleService.getAllVehiclesByStation(mockStation);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getVehicleId());
        assertEquals("Station A", result.get(0).getStationName());
        assertEquals(2, result.get(0).getImagePaths().size());
    }

    @Test
    void testGetAllVehiclesByStation_Success_Empty() {
        when(vehicleRepository.findByStationWithDetails(eq(mockStation), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<VehicleResponse> result = vehicleService.getAllVehiclesByStation(mockStation);

        assertTrue(result.isEmpty());
    }

    // --- getVehiclesByModelAndStation ---

    @Test
    void testGetVehiclesByModelAndStation_Success_NoUser() {
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(modelService.getModelById(1L)).thenReturn(mockModel);
        when(vehicleRepository.findByStationAndModelAndStatusNotIn(eq(mockStation), eq(mockModel), anyList()))
                .thenReturn(List.of(mockVehicle_Available));

        List<VehicleResponse> result = vehicleService.getVehiclesByModelAndStation(1L, 1L, null);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isReservedByMe());
        assertFalse(result.get(0).isRentedByMe());
    }

    @Test
    void testGetVehiclesByModelAndStation_Success_WithUser_RentedByMe() {
        mockVehicle_Available.setCondition(VehicleCondition.GOOD);
        mockVehicle_Rented.setCondition(VehicleCondition.GOOD);
        mockVehicle_Rented.setStatus(VehicleStatus.RENTED);
        mockBooking_Renting.setVehicle(mockVehicle_Rented);

        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(modelService.getModelById(1L)).thenReturn(mockModel);
        when(vehicleRepository.findByStationAndModelAndStatusNotIn(eq(mockStation), eq(mockModel), anyList()))
                .thenReturn(List.of(mockVehicle_Available, mockVehicle_Rented));
        when(bookingRepository.findByUserAndStatusIn(eq(mockRenter), anyList()))
                .thenReturn(List.of(mockBooking_Renting));

        List<VehicleResponse> result = vehicleService.getVehiclesByModelAndStation(1L, 1L, mockRenter);

        assertEquals(2, result.size());
        assertFalse(result.stream().anyMatch(v -> v.getVehicleId() == 101L && v.isRentedByMe()));
        assertTrue(result.stream().anyMatch(v -> v.getVehicleId() == 102L && v.isRentedByMe()));
        assertFalse(result.stream().anyMatch(v -> v.getVehicleId() == 102L && v.isReservedByMe()));
    }


    @Test
    void testGetVehiclesByModelAndStation_Success_ReservedByMe() {
        mockVehicle_Available.setStatus(VehicleStatus.RESERVED);
        mockBooking_Confirmed.setVehicle(mockVehicle_Available);

        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(modelService.getModelById(1L)).thenReturn(mockModel);
        when(vehicleRepository.findByStationAndModelAndStatusNotIn(eq(mockStation), eq(mockModel), anyList()))
                .thenReturn(List.of(mockVehicle_Available));
        when(bookingRepository.findByUserAndStatusIn(eq(mockRenter), anyList()))
                .thenReturn(List.of(mockBooking_Confirmed));

        List<VehicleResponse> result = vehicleService.getVehiclesByModelAndStation(1L, 1L, mockRenter);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isReservedByMe());
        assertFalse(result.get(0).isRentedByMe());
    }

    @Test
    void testGetVehiclesByModelAndStation_UserHasBookingButNotForThisVehicle() {
        // User is renting another vehicle (not in the list)
        Vehicle otherVehicle = Vehicle.builder().vehicleId(999L).status(VehicleStatus.RENTED).model(mockModel).station(mockStation).build();
        Booking otherBooking = Booking.builder().vehicle(otherVehicle).status(BookingStatus.RENTING).build();

        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(modelService.getModelById(1L)).thenReturn(mockModel);
        when(vehicleRepository.findByStationAndModelAndStatusNotIn(eq(mockStation), eq(mockModel), anyList()))
                .thenReturn(List.of(mockVehicle_Available));
        when(bookingRepository.findByUserAndStatusIn(eq(mockRenter), anyList()))
                .thenReturn(List.of(otherBooking));

        List<VehicleResponse> result = vehicleService.getVehiclesByModelAndStation(1L, 1L, mockRenter);

        assertEquals(1, result.size());
        // The available vehicle should not be marked as rented or reserved by the user
        assertFalse(result.get(0).isRentedByMe());
        assertFalse(result.get(0).isReservedByMe());
    }

    @Test
    void testGetVehiclesByModelAndStation_Success_BookingWithNoVehicle() {
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(modelService.getModelById(1L)).thenReturn(mockModel);
        when(vehicleRepository.findByStationAndModelAndStatusNotIn(eq(mockStation), eq(mockModel), anyList()))
                .thenReturn(List.of(mockVehicle_Available));
        Booking bookingNoVehicle = Booking.builder().status(BookingStatus.CONFIRMED).vehicle(null).build();
        when(bookingRepository.findByUserAndStatusIn(eq(mockRenter), anyList()))
                .thenReturn(List.of(bookingNoVehicle));

        List<VehicleResponse> result = vehicleService.getVehiclesByModelAndStation(1L, 1L, mockRenter);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isReservedByMe());
    }

    // --- checkVehicleSchedule ---

    @Test
    void testCheckVehicleSchedule_Success_Available() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(3);

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(bookingRepository.countOverlappingBookingsForVehicle(eq(mockVehicle_Available), eq(start), eq(end), anyList()))
                .thenReturn(0L);

        Map<String, Object> result = vehicleService.checkVehicleSchedule(101L, start, end);

        assertTrue((Boolean) result.get("isAvailable"));
        assertEquals("Xe khả dụng trong khung giờ này.", result.get("message"));
    }

    @Test
    void testCheckVehicleSchedule_Fail_NotAvailable() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(3);

        mockVehicle_Available.setStatus(VehicleStatus.UNAVAILABLE);
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        Map<String, Object> result = vehicleService.checkVehicleSchedule(101L, start, end);

        assertFalse((Boolean) result.get("isAvailable"));
        assertEquals("Xe này hiện không còn khả dụng.", result.get("message"));
        verify(bookingRepository, never()).countOverlappingBookingsForVehicle(any(), any(), any(), anyList());
    }

    @Test
    void testCheckVehicleSchedule_Fail_Conflicts() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(3);

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(bookingRepository.countOverlappingBookingsForVehicle(eq(mockVehicle_Available), eq(start), eq(end), anyList()))
                .thenReturn(1L);

        Map<String, Object> result = vehicleService.checkVehicleSchedule(101L, start, end);

        assertFalse((Boolean) result.get("isAvailable"));
        assertEquals("Xe đã có lịch đặt trong khung giờ này.", result.get("message"));
    }

    // --- getAllVehicles ---

    @Test
    void testGetAllVehicles_NoFilters_DefaultSort() {
        when(vehicleRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(mockVehicle_Available));

        List<VehicleResponse> result = vehicleService.getAllVehicles(null, null, null, null, null);

        assertEquals(1, result.size());
        verify(vehicleRepository, times(1))
                .findAll(any(Specification.class), eq(Sort.by(Sort.Direction.DESC, "model.pricePerHour")));

    }

    @Test
    void testGetAllVehicles_AllFilters_SortByPrice_ASC() {
        when(vehicleRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(mockVehicle_Available));

        List<VehicleResponse> result = vehicleService.getAllVehicles(1L, 1L, VehicleType.CAR, "model.pricePerHour", "ASC");

        assertEquals(1, result.size());
        verify(vehicleRepository, times(1)).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.ASC, "model.pricePerHour")));
    }

    @Test
    void testGetAllVehicles_HandlesNullStatusCondition() {
        mockVehicle_Available.setStatus(null);
        mockVehicle_Available.setCondition(null);
        when(vehicleRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(mockVehicle_Available));

        List<VehicleResponse> result = vehicleService.getAllVehicles(null, null, null, null, null);

        assertEquals(1, result.size());
        assertNull(result.get(0).getStatus());
        assertNull(result.get(0).getCondition());
    }

    // --- getVehicleById ---

    @Test
    void testGetVehicleById_Success() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        Vehicle result = vehicleService.getVehicleById(101L);
        assertEquals(101L, result.getVehicleId());
    }

    @Test
    void testGetVehicleById_Fail_NotFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.getVehicleById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- getVehicleDetailsById ---

    @Test
    void testGetVehicleDetailsById_Success() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        VehicleResponse result = vehicleService.getVehicleDetailsById(101L);

        assertEquals(101L, result.getVehicleId());
        assertEquals("51A-11111", result.getLicensePlate());
        assertEquals("AVAILABLE", result.getStatus());
        assertEquals("GOOD", result.getCondition());
        assertEquals(90, result.getBatteryLevel());
        assertEquals(1000.0, result.getCurrentMileage());
        assertEquals("VinFast VF e34", result.getModelName());
        assertEquals(1L, result.getStationId());
        assertEquals("Station A", result.getStationName());
        assertEquals(VehicleType.CAR, result.getVehicleType());
        assertEquals(100.0, result.getPricePerHour());
        assertEquals(5, result.getSeatCount());
        assertEquals(300.0, result.getRangeKm());
        assertEquals("AI", result.getFeatures());
        assertEquals("Desc", result.getDescription());
        assertEquals(2, result.getImagePaths().size());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void testGetVehicleDetailsById_Fail_NotFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.getVehicleDetailsById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testGetVehicleDetailsById_HandlesNullImagePaths() {
        mockModel.setImagePaths(null);
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        VehicleResponse result = vehicleService.getVehicleDetailsById(101L);

        assertNotNull(result.getImagePaths());
        assertTrue(result.getImagePaths().isEmpty());
    }

    @Test
    void testGetVehicleDetailsById_HandlesBlankImagePaths() {
        mockModel.setImagePaths(" ");
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        VehicleResponse result = vehicleService.getVehicleDetailsById(101L);

        assertNotNull(result.getImagePaths());
        assertTrue(result.getImagePaths().isEmpty());
    }

    // --- saveVehicle ---

    @Test
    void testSaveVehicle() {
        when(vehicleRepository.save(mockVehicle_Available)).thenReturn(mockVehicle_Available);
        vehicleService.saveVehicle(mockVehicle_Available);
        verify(vehicleRepository, times(1)).save(mockVehicle_Available);
    }

    // --- updateVehicleDetails ---

    @Test
    void testUpdateVehicleDetails_Success() {
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest();
        request.setBatteryLevel(80);
        request.setNewCondition(VehicleCondition.MINOR_DAMAGE);

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(mockVehicle_Available);

        Vehicle updatedVehicle = vehicleService.updateVehicleDetails(mockStaff, 101L, request);

        assertNotNull(updatedVehicle);
        assertEquals(80, updatedVehicle.getBatteryLevel());
        assertEquals(VehicleCondition.MINOR_DAMAGE, updatedVehicle.getCondition());
        verify(vehicleRepository, times(1)).save(mockVehicle_Available);
    }

    @Test
    void testUpdateVehicleDetails_Success_EmptyRequest() {
        UpdateVehicleDetailsRequest request = new UpdateVehicleDetailsRequest(); // Empty

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(mockVehicle_Available);

        Vehicle updatedVehicle = vehicleService.updateVehicleDetails(mockStaff, 101L, request);

        // Values remain unchanged
        assertEquals(90, updatedVehicle.getBatteryLevel());
        assertEquals(VehicleCondition.GOOD, updatedVehicle.getCondition());
        verify(vehicleRepository, times(1)).save(mockVehicle_Available);
    }

    @Test
    void testUpdateVehicleDetails_Fail_StaffNoStation() {
        mockStaff.setStation(null);
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.updateVehicleDetails(mockStaff, 101L, new UpdateVehicleDetailsRequest());
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn không có quyền cập nhật"));
    }

    @Test
    void testUpdateVehicleDetails_Fail_WrongStation() {
        mockVehicle_Available.setStation(Station.builder().stationId(2L).build());
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.updateVehicleDetails(mockStaff, 101L, new UpdateVehicleDetailsRequest());
        });

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn không có quyền cập nhật"));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // --- reportMajorDamage ---

    @Test
    void testReportMajorDamage_Success_NoPhotos_NoHistory() throws Exception {
        ReportDamageRequest req = new ReportDamageRequest();
        req.setDescription("Xe bị vỡ đèn");
        req.setPhotos(null);

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(historyRepository.findFirstByVehicleOrderByActionTimeDesc(mockVehicle_Available)).thenReturn(null);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle damagedVehicle = vehicleService.reportMajorDamage(mockStaff, 101L, req);

        assertNotNull(damagedVehicle);
        assertEquals(VehicleCondition.MAINTENANCE_REQUIRED, damagedVehicle.getCondition());
        assertEquals(VehicleStatus.UNAVAILABLE, damagedVehicle.getStatus());
        assertEquals("[]", damagedVehicle.getDamageReportPhotos());

        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
        verify(historyRepository, times(1)).save(
                argThat(history ->
                        history.getActionType() == VehicleActionType.MAINTENANCE &&
                                "Xe bị vỡ đèn".equals(history.getNote()) &&
                                "Không rõ (Xe mới)".equals(history.getConditionBefore()) &&
                                "[]".equals(history.getPhotoPaths())
                )
        );
    }


    @Test
    void testReportMajorDamage_Success_WithPhotos_WithHistory() throws Exception {
        MultipartFile mockFile = new MockMultipartFile("photos", "damage.jpg", "image/jpeg", "content".getBytes());
        ReportDamageRequest req = new ReportDamageRequest();
        req.setDescription("Xe bị vỡ đèn");
        req.setPhotos(List.of(mockFile));

        VehicleHistory lastHistory = VehicleHistory.builder().conditionAfter("GOOD").build();

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(historyRepository.findFirstByVehicleOrderByActionTimeDesc(mockVehicle_Available)).thenReturn(lastHistory);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock the internal method to avoid file system interaction
        doReturn("/uploads/damage_reports/vehicle_101/damage-1.jpg")
                .when(vehicleService).saveDamageReportPhoto(any(MultipartFile.class), eq(101L), eq(1));

        Vehicle damagedVehicle = vehicleService.reportMajorDamage(mockStaff, 101L, req);

        assertNotNull(damagedVehicle);
        assertEquals(VehicleCondition.MAINTENANCE_REQUIRED, damagedVehicle.getCondition());
        assertTrue(damagedVehicle.getDamageReportPhotos().contains("damage-1.jpg"));

        verify(historyRepository, times(1)).save(
                argThat(history -> "GOOD".equals(history.getConditionBefore()))
        );
        verify(vehicleService, times(1)).saveDamageReportPhoto(mockFile, 101L, 1);
    }


    @Test
    void testReportMajorDamage_Success_HistoryHasConditionBeforeOnly() throws Exception {
        ReportDamageRequest req = new ReportDamageRequest();
        req.setDescription("Xe bị vỡ đèn");
        req.setPhotos(null);

        VehicleHistory lastHistory = VehicleHistory.builder().conditionBefore("EXCELLENT").conditionAfter(null).build();

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(historyRepository.findFirstByVehicleOrderByActionTimeDesc(mockVehicle_Available)).thenReturn(lastHistory);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(mockVehicle_Available);

        vehicleService.reportMajorDamage(mockStaff, 101L, req);

        verify(historyRepository).save(argThat(h -> "EXCELLENT".equals(h.getConditionBefore())));
    }

    @Test
    void testReportMajorDamage_Fail_StaffInactive() {
        mockStaff.setStatus(AccountStatus.INACTIVE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.reportMajorDamage(mockStaff, 101L, new ReportDamageRequest());
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Tài khoản nhân viên của bạn đã bị khóa"));
    }

    @Test
    void testReportMajorDamage_Fail_WrongStation() {
        mockVehicle_Available.setStation(Station.builder().stationId(2L).build());
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.reportMajorDamage(mockStaff, 101L, new ReportDamageRequest());
        });
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Bạn không có quyền báo cáo hư hỏng"));
    }

    @Test
    void testReportMajorDamage_Fail_SavePhotoError() throws Exception {
        MultipartFile mockFile = new MockMultipartFile("photos", "damage.jpg", "image/jpeg", "content".getBytes());
        ReportDamageRequest req = new ReportDamageRequest();
        req.setDescription("Test photo save error");
        req.setPhotos(List.of(mockFile));

        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        // Mock the internal method to throw an exception
        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi lưu ảnh báo cáo."))
                .when(vehicleService).saveDamageReportPhoto(any(MultipartFile.class), anyLong(), anyInt());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.reportMajorDamage(mockStaff, 101L, req);
        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Lỗi hệ thống khi lưu ảnh báo cáo."));
        // Verify that the main entity is not saved if photo saving fails
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }


    @Test
    void testReportMajorDamage_Fail_InternalErrorInTryBlock() throws Exception {
        ObjectMapper mockObjectMapper = Mockito.mock(ObjectMapper.class);
        ReflectionTestUtils.setField(vehicleService, "objectMapper", mockObjectMapper);

        ReportDamageRequest req = new ReportDamageRequest();
        req.setDescription("Xe bị lỗi test");
        req.setPhotos(null);

        lenient().when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        lenient().when(historyRepository.findFirstByVehicleOrderByActionTimeDesc(mockVehicle_Available)).thenReturn(null);
        lenient().when(mockObjectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Fake JSON Error") {});
        lenient().when(vehicleRepository.save(any(Vehicle.class))).thenReturn(mockVehicle_Available);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                vehicleService.reportMajorDamage(mockStaff, 101L, req)
        );

        assertTrue(ex.getMessage().contains("Lỗi khi lưu ảnh"), "Phải bắt lỗi RuntimeException với thông báo phù hợp");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    // --- recordVehicleAction ---

    @Test
    void testRecordVehicleAction_Success_AllIds() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(userRepository.findById(10L)).thenReturn(Optional.of(mockStaff));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockRenter));
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(historyRepository.save(any(VehicleHistory.class))).thenAnswer(i -> i.getArgument(0));

        VehicleHistory history = vehicleService.recordVehicleAction(101L, 10L, 1L, 1L, VehicleActionType.DELIVERY, "Note", "Before", "After", 90.0, 1000.0, "[]");

        assertEquals(mockVehicle_Available, history.getVehicle());
        assertEquals(mockStaff, history.getStaff());
        assertEquals(mockRenter, history.getRenter());
        assertEquals(mockStation, history.getStation());
        assertEquals(VehicleActionType.DELIVERY, history.getActionType());
        assertEquals("Note", history.getNote());
        assertEquals("Before", history.getConditionBefore());
        assertEquals("After", history.getConditionAfter());
        assertEquals(90.0, history.getBatteryLevel());
        assertEquals(1000.0, history.getMileage());
        assertEquals("[]", history.getPhotoPaths());
    }

    @Test
    void testRecordVehicleAction_Success_NullIds() {
        when(vehicleRepository.findById(101L)).thenReturn(Optional.of(mockVehicle_Available));
        when(historyRepository.save(any(VehicleHistory.class))).thenAnswer(i -> i.getArgument(0));

        VehicleHistory history = vehicleService.recordVehicleAction(101L, null, null, null, VehicleActionType.MAINTENANCE, "Note", null, null, null, null, null);

        assertEquals(mockVehicle_Available, history.getVehicle());
        assertNull(history.getStaff());
        assertNull(history.getRenter());
        assertNull(history.getStation());
        assertEquals(VehicleActionType.MAINTENANCE, history.getActionType());
    }

    @Test
    void testRecordVehicleAction_Fail_VehicleNotFound() {
        when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            vehicleService.recordVehicleAction(999L, null, null, null, VehicleActionType.MAINTENANCE, "Note", null, null, null, null, null);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- getVehicleHistory ---

    @Test
    void testGetVehicleHistory_Success_AllFilters() {
        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L).vehicle(mockVehicle_Available).staff(mockStaff).renter(mockRenter).station(mockStation)
                .actionType(VehicleActionType.DELIVERY).actionTime(LocalDateTime.now())
                .build();
        when(historyRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(history));

        List<VehicleHistoryResponse> result = vehicleService.getVehicleHistory(1L, LocalDate.now(), LocalDate.now(), VehicleType.CAR, "51A");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getHistoryId());
        assertEquals("Test Staff", result.get(0).getStaffName());
        assertEquals("Test Renter", result.get(0).getRenterName());
        assertEquals("Station A", result.get(0).getStationName());
        assertEquals("51A-11111", result.get(0).getLicensePlate());

        verify(historyRepository, times(1)).findAll(any(Specification.class), eq(Sort.by(Sort.Direction.DESC, "actionTime")));
    }

    @Test
    void testGetVehicleHistory_WithBlankLicensePlate() {
        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L).vehicle(mockVehicle_Available).staff(mockStaff).renter(mockRenter).station(mockStation)
                .actionType(VehicleActionType.DELIVERY).actionTime(LocalDateTime.now())
                .build();
        when(historyRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(history));

        // Call with a blank license plate
        List<VehicleHistoryResponse> result = vehicleService.getVehicleHistory(1L, LocalDate.now(), LocalDate.now(), VehicleType.CAR, " ");

        assertEquals(1, result.size());
        // We can't easily verify the predicate, but we can ensure the method runs and returns data.
        // The key is that the blank license plate should not cause an error and should be ignored in the filter.
        verify(historyRepository, times(1)).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void testGetVehicleHistory_Success_NoFilters() {
        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L).vehicle(mockVehicle_Available).staff(mockStaff).renter(mockRenter).station(mockStation)
                .actionType(VehicleActionType.DELIVERY).actionTime(LocalDateTime.now())
                .build();
        when(historyRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(history));

        List<VehicleHistoryResponse> result = vehicleService.getVehicleHistory(null, null, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void testGetVehicleHistory_Success_NullRelations() {
        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L)
                .vehicle(mockVehicle_Available)
                .staff(null).renter(null).station(null)
                .actionType(VehicleActionType.MAINTENANCE).actionTime(LocalDateTime.now())
                .build();
        when(historyRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(history));

        List<VehicleHistoryResponse> result = vehicleService.getVehicleHistory(null, null, null, null, null);

        assertEquals(1, result.size());
        assertNull(result.get(0).getStaffName());
        assertNull(result.get(0).getRenterName());
        assertNull(result.get(0).getStationName());
    }

    // --- getHistoryByVehicle ---

    @Test
    void testGetHistoryByVehicle_Success() {
        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L).vehicle(mockVehicle_Available).staff(mockStaff).renter(mockRenter).station(mockStation)
                .actionType(VehicleActionType.DELIVERY).actionTime(LocalDateTime.now())
                .build();
        when(historyRepository.findByVehicle_VehicleIdOrderByActionTimeDesc(101L)).thenReturn(List.of(history));

        List<VehicleHistoryResponse> result = vehicleService.getHistoryByVehicle(101L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getHistoryId());
        assertEquals("Test Staff", result.get(0).getStaffName());
    }

    // --- getHistoryByRenter ---

    @Test
    void testGetHistoryByRenter_Success() {
        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L).vehicle(mockVehicle_Available).staff(mockStaff).renter(mockRenter).station(mockStation)
                .actionType(VehicleActionType.DELIVERY).actionTime(LocalDateTime.now())
                .build();
        when(historyRepository.findByRenter_UserIdOrderByActionTimeDesc(1L)).thenReturn(List.of(history));

        List<VehicleHistoryResponse> result = vehicleService.getHistoryByRenter(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getHistoryId());
        assertEquals("Test Renter", result.get(0).getRenterName());
    }

    @AfterEach
    void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

}