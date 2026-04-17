package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.DashboardSummaryDto;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus; // Thêm import
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map; // Thêm import

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private User mockStaff;
    private Station mockStation;

    @BeforeEach
    void setUp() {
        mockStation = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .build();

        mockStaff = User.builder()
                .userId(10L)
                .fullName("Trần Văn B")
                .station(mockStation)
                .build();
    }

    @Test
    void testGetSummaryForStaff_Success_WithEmptyStatusSummary() {
        when(vehicleRepository.countByStation(eq(mockStation))).thenReturn(10L);
        when(vehicleRepository.countVehiclesByStatus(eq(mockStation))).thenReturn(List.of());

        DashboardSummaryDto result = dashboardService.getSummaryForStaff(mockStaff);

        assertNotNull(result);
        assertEquals("Trạm Evolve - Quận 1", result.getStationName());
        assertEquals(10L, result.getTotalVehicles());
        assertNotNull(result.getStatusSummary());
        assertTrue(result.getStatusSummary().isEmpty());

        verify(vehicleRepository, times(1)).countByStation(eq(mockStation));
        verify(vehicleRepository, times(1)).countVehiclesByStatus(eq(mockStation));
    }

    @Test
    void testGetSummaryForStaff_Success_WithFullStatusSummary() {
        when(vehicleRepository.countByStation(eq(mockStation))).thenReturn(10L);

        Object[] availableRow = {VehicleStatus.AVAILABLE, 5L};
        Object[] rentedRow = {VehicleStatus.RENTED, 3L};
        Object[] unavailableRow = {VehicleStatus.UNAVAILABLE, 2L};
        List<Object[]> statusList = List.of(availableRow, rentedRow, unavailableRow);

        when(vehicleRepository.countVehiclesByStatus(eq(mockStation))).thenReturn(statusList);

        DashboardSummaryDto result = dashboardService.getSummaryForStaff(mockStaff);

        assertNotNull(result);
        assertEquals(10L, result.getTotalVehicles());
        assertNotNull(result.getStatusSummary());
        assertEquals(3, result.getStatusSummary().size());
        assertEquals(5L, result.getStatusSummary().get(VehicleStatus.AVAILABLE));
        assertEquals(3L, result.getStatusSummary().get(VehicleStatus.RENTED));
        assertEquals(2L, result.getStatusSummary().get(VehicleStatus.UNAVAILABLE));
        assertNull(result.getStatusSummary().get(VehicleStatus.RESERVED));
    }

    @Test
    void testGetSummaryForStaff_Success_NoVehicles() {
        when(vehicleRepository.countByStation(eq(mockStation))).thenReturn(0L);
        when(vehicleRepository.countVehiclesByStatus(eq(mockStation))).thenReturn(List.of());

        DashboardSummaryDto result = dashboardService.getSummaryForStaff(mockStaff);

        assertNotNull(result);
        assertEquals("Trạm Evolve - Quận 1", result.getStationName());
        assertEquals(0L, result.getTotalVehicles());
        assertNotNull(result.getStatusSummary());
        assertEquals(0, result.getStatusSummary().size());

        verify(vehicleRepository, times(1)).countByStation(eq(mockStation));
        verify(vehicleRepository, times(1)).countVehiclesByStatus(eq(mockStation));
    }

    @Test
    void testGetSummaryForStaff_Fail_StaffNotAssignedToStation() {
        mockStaff.setStation(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                dashboardService.getSummaryForStaff(mockStaff)
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Tài khoản nhân viên này chưa được gán cho trạm nào"));

        verify(vehicleRepository, never()).countByStation(any());
        verify(vehicleRepository, never()).countVehiclesByStatus(any());
    }

}