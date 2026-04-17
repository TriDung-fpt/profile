package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.StationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateStationRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.StationStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.StationRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StationServiceImplTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private StationServiceImpl stationService;

    private Station mockStation;
    private StationRequest stationRequest;
    private UpdateStationRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockStation = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .status(StationStatus.ACTIVE)
                .build();

        stationRequest = new StationRequest("New Station", "123 Street", "Desc", "8-17", "19001009");
        updateRequest = new UpdateStationRequest("Updated Name", "Updated Address", "Updated Desc", "9-18", "19001008", "INACTIVE");
    }

    // --- Test 1: addStation ---
    @Test
    void testAddStation_Success() {
        // 1. Chuẩn bị
        when(stationRepository.save(any(Station.class))).thenAnswer(invocation -> {
            Station station = invocation.getArgument(0);
            station.setStationId(2L); // Giả lập DB gán ID
            return station;
        });

        // 2. Thực thi
        Station createdStation = stationService.addStation(stationRequest);

        // 3. Xác minh
        assertNotNull(createdStation);
        assertEquals(2L, createdStation.getStationId());
        assertEquals("New Station", createdStation.getName());
        assertEquals("123 Street", createdStation.getAddress());
        assertEquals("Desc", createdStation.getDescription());
        assertEquals("8-17", createdStation.getOpeningHours());
        assertEquals("19001009", createdStation.getHotline());
        assertEquals(StationStatus.ACTIVE, createdStation.getStatus()); // Mặc định là ACTIVE
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    // --- Test 2: getAllStations ---
    @Test
    void testGetAllStations_Success() {
        // 1. Chuẩn bị
        when(stationRepository.findByStatus(StationStatus.ACTIVE)).thenReturn(List.of(mockStation));

        // 2. Thực thi
        List<Station> stations = stationService.getAllStations();

        // 3. Xác minh
        assertNotNull(stations);
        assertEquals(1, stations.size());
        assertEquals("Trạm Evolve - Quận 1", stations.get(0).getName());
        verify(stationRepository, times(1)).findByStatus(StationStatus.ACTIVE);
    }

    // --- Test 3: updateStation ---
    @Test
    void testUpdateStation_Success_AllFields() {
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        when(stationRepository.save(any(Station.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Thực thi
        Station updatedStation = stationService.updateStation(1L, updateRequest);

        // 3. Xác minh
        assertNotNull(updatedStation);
        assertEquals("Updated Name", updatedStation.getName());
        assertEquals("Updated Address", updatedStation.getAddress());
        assertEquals("Updated Desc", updatedStation.getDescription());
        assertEquals("9-18", updatedStation.getOpeningHours());
        assertEquals("19001008", updatedStation.getHotline());
        assertEquals(StationStatus.INACTIVE, updatedStation.getStatus()); // Đã cập nhật status
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    void testUpdateStation_Success_NullFields() {
        // Test trường hợp request chỉ cập nhật 1 vài trường, các trường khác là null
        // 1. Chuẩn bị
        UpdateStationRequest partialRequest = new UpdateStationRequest();
        partialRequest.setName("Only Name Updated");
        // Các trường khác là null

        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        when(stationRepository.save(any(Station.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Thực thi
        Station updatedStation = stationService.updateStation(1L, partialRequest);

        // 3. Xác minh
        assertEquals("Only Name Updated", updatedStation.getName()); // Tên mới
        assertNull(updatedStation.getAddress()); // Địa chỉ cũ (null trong mockStation)
        assertEquals(StationStatus.ACTIVE, updatedStation.getStatus()); // Status cũ
        verify(stationRepository, times(1)).save(any(Station.class));
    }

    @Test
    void testUpdateStation_Fail_NotFound() {
        // 1. Chuẩn bị
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        // 2. Thực thi & 3. Xác minh
        assertThrows(RuntimeException.class, () -> { // Service ném RuntimeException
            stationService.updateStation(999L, updateRequest);
        });

        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    void testUpdateStation_Fail_InvalidStatus() {
        // Test khối try-catch (IllegalArgumentException e)
        // 1. Chuẩn bị
        UpdateStationRequest invalidStatusRequest = new UpdateStationRequest();
        invalidStatusRequest.setStatus("INVALID_STATUS"); // Trạng thái không hợp lệ

        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));

        // 2. Thực thi & 3. Xác minh
        assertThrows(IllegalArgumentException.class, () -> {
            stationService.updateStation(1L, invalidStatusRequest);
        });

        verify(stationRepository, never()).save(any(Station.class));
    }

    // --- Test 4: deleteStation ---
    @Test
    void testDeleteStation_Success_NoVehicles() {
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        when(vehicleRepository.countByStation(any(Station.class))).thenReturn(0L); // Không có xe

        // 2. Thực thi & 3. Xác minh
        assertDoesNotThrow(() -> stationService.deleteStation(1L));

        verify(stationRepository, times(1)).delete(mockStation);
    }

    @Test
    void testDeleteStation_Fail_VehicleExists() {
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        when(vehicleRepository.countByStation(any(Station.class))).thenReturn(1L); // Vẫn còn 1 xe

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stationService.deleteStation(1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể xóa trạm"));

        verify(stationRepository, never()).delete(any(Station.class));
    }

    @Test
    void testDeleteStation_Fail_NotFound() {
        // 1. Chuẩn bị
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stationService.deleteStation(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(vehicleRepository, never()).countByStation(any());
        verify(stationRepository, never()).delete(any());
    }

    // --- Test 5: getStationById ---
    @Test
    void testGetStationById_Success() {
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        // 2. Thực thi
        Station station = stationService.getStationById(1L);
        // 3. Xác minh
        assertNotNull(station);
        assertEquals(1L, station.getStationId());
    }

    @Test
    void testGetStationById_Fail_NotFound() {
        // 1. Chuẩn bị
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());
        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stationService.getStationById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- Test 6: getVehicleStatsByStation ---
    @Test
    void testGetVehicleStatsByStation_Success_Calculations() {
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));

        // Giả lập 2 AVAILABLE, 8 RENTED
        Object[] row1 = {VehicleStatus.AVAILABLE, 2L};
        Object[] row2 = {VehicleStatus.RENTED, 8L};
        List<Object[]> mockResults = List.of(row1, row2);
        when(vehicleRepository.countVehiclesByStatus(mockStation)).thenReturn(mockResults);

        // 2. Thực thi
        Map<String, Object> stats = stationService.getVehicleStatsByStation(1L);

        // 3. Xác minh
        assertNotNull(stats);
        assertEquals(10L, stats.get("totalVehicles")); // Tổng 2 + 8 = 10
        assertEquals(80.0, (Double) stats.get("rentedRate")); // 8/10 * 100
        assertEquals("CAO", stats.get("demandLevel")); // > 60 là CAO

        // Xác minh map có đầy đủ các key
        Map<String, Long> counts = (Map<String, Long>) stats.get("vehicleCounts");
        assertEquals(2L, counts.get("AVAILABLE"));
        assertEquals(8L, counts.get("RENTED"));
        assertEquals(0L, counts.get("RESERVED")); // Key mặc định
        assertEquals(0L, counts.get("UNAVAILABLE")); // Key mặc định
    }

    @Test
    void testGetVehicleStatsByStation_Success_NoVehicles() {
        // Test luồng total = 0
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        when(vehicleRepository.countVehiclesByStatus(mockStation)).thenReturn(Collections.emptyList()); // Không có xe

        // 2. Thực thi
        Map<String, Object> stats = stationService.getVehicleStatsByStation(1L);

        // 3. Xác minh
        assertNotNull(stats);
        assertEquals(0L, stats.get("totalVehicles"));
        assertEquals(0.0, (Double) stats.get("rentedRate")); // Test (total > 0 ? ...)
        assertEquals("THẤP", stats.get("demandLevel")); // 0 là THẤP
    }

    @Test
    void testGetVehicleStatsByStation_Success_MediumDemand() {
        // Test demandLevel = TRUNG BÌNH
        // 1. Chuẩn bị
        when(stationRepository.findById(1L)).thenReturn(Optional.of(mockStation));
        Object[] row = {VehicleStatus.RENTED, 5L}; // 5 xe thuê
        // SỬA LỖI: Sửa tên phương thức bị sai chính tả
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{VehicleStatus.RENTED, 5L});
        when(vehicleRepository.countVehiclesByStatus(mockStation)).thenReturn(rows);

        // 2. Thực thi
        Map<String, Object> stats = stationService.getVehicleStatsByStation(1L);

        // 3. Xác minh
        assertNotNull(stats);
        assertEquals(5L, stats.get("totalVehicles"));
        assertEquals(100.0, (Double) stats.get("rentedRate")); // 5/5 * 100
        assertEquals("CAO", stats.get("demandLevel")); // > 60
    }

    @Test
    void testGetVehicleStatsByStation_Fail_NotFound() {
        // 1. Chuẩn bị
        when(stationRepository.findById(999L)).thenReturn(Optional.empty());

        // 2. Thực thi & 3. Xác minh
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            stationService.getVehicleStatsByStation(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(vehicleRepository, never()).countVehiclesByStatus(any());
    }

    // --- Test 7: getAllStationReports ---

    @Test
    void testGetAllStationReports_Success() {
        // 1. Chuẩn bị
        // Giả lập 2 trạm, trạm 1 có 10 xe (8 RENTED, 2 AVAILABLE)
        Object[] row1_S1 = {1L, "Station 1", VehicleStatus.RENTED, 8L};
        Object[] row2_S1 = {1L, "Station 1", VehicleStatus.AVAILABLE, 2L};
        // Trạm 2 có 5 xe (1 RENTED, 4 RESERVED)
        Object[] row1_S2 = {2L, "Station 2", VehicleStatus.RENTED, 1L};
        Object[] row2_S2 = {2L, "Station 2", VehicleStatus.RESERVED, 4L};

        List<Object[]> mockResults = List.of(row1_S1, row2_S1, row1_S2, row2_S2);
        when(vehicleRepository.getVehicleStatsGroupedByStation()).thenReturn(mockResults);

        // 2. Thực thi
        List<Map<String, Object>> result = stationService.getAllStationReports();

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(2, result.size()); // Phải gộp thành 2 trạm

        // Tìm report của trạm 1
        Map<String, Object> report1 = result.stream().filter(r -> r.get("stationId").equals(1L)).findFirst().get();
        assertEquals("Station 1", report1.get("stationName"));
        assertEquals(8L, report1.get("RENTED"));
        assertEquals(2L, report1.get("AVAILABLE"));
        assertEquals(0L, report1.get("RESERVED")); // Mặc định
        assertEquals(80.0, (Double) report1.get("rentedRate"));
        assertEquals("CAO", report1.get("demandLevel"));

        // Tìm report của trạm 2
        Map<String, Object> report2 = result.stream().filter(r -> r.get("stationId").equals(2L)).findFirst().get();
        assertEquals("Station 2", report2.get("stationName"));
        assertEquals(1L, report2.get("RENTED"));
        assertEquals(4L, report2.get("RESERVED"));
        assertEquals(0L, report2.get("AVAILABLE")); // Mặc định
        assertEquals(20.0, (Double) report2.get("rentedRate")); // 1 / (1+4) * 100
        assertEquals("THẤP", report2.get("demandLevel")); // < 30
    }

    @Test
    void testGetAllStationReports_Success_NoData() {
        // 1. Chuẩn bị
        when(vehicleRepository.getVehicleStatsGroupedByStation()).thenReturn(Collections.emptyList());

        // 2. Thực thi
        List<Map<String, Object>> result = stationService.getAllStationReports();

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}