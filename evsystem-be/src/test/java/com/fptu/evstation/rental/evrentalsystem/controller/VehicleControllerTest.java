package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService; // <-- THÊM IMPORT
import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import com.fptu.evstation.rental.evrentalsystem.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Thêm (excludeAutoConfiguration = SecurityAutoConfiguration.class) để vô hiệu hóa security trong test này
@WebMvcTest(value = VehicleController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    @MockitoBean
    private StationService stationService;

    @MockitoBean
    private AuthService authService; // <-- SỬA LỖI: Thêm mock bean này

    @Autowired
    private ObjectMapper objectMapper;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.of(2025, 11, 10, 10, 0, 0);
        endTime = LocalDateTime.of(2025, 11, 10, 12, 0, 0);
    }

    // --- Tests cho GET /api/vehicles/station/{stationId}/stats ---

    @Test
    void testGetVehicleStatsByStation_KhiGoiThanhCong() throws Exception {
        // Dữ liệu giả trả về từ service
        Map<String, Object> mockStats = Map.of(
                "stationId", 1L,
                "stationName", "Trạm A",
                "totalVehicles", 10L
        );

        // Giả lập stationService trả về dữ liệu
        when(stationService.getVehicleStatsByStation(1L)).thenReturn(mockStats);

        // Thực hiện gọi API
        // SỬA LỖI: Thêm /stats vào cuối URL
        mockMvc.perform(get("/api/vehicles/station/1/stats"))
                .andExpect(status().isOk()) // Mong đợi 200 OK
                .andExpect(jsonPath("$.stationId").value(1L))
                .andExpect(jsonPath("$.stationName").value("Trạm A"))
                .andExpect(jsonPath("$.totalVehicles").value(10));
    }


    @Test
    void testGetVehicleStatsByStation_KhiTramKhongTimThay() throws Exception {
        // Giả lập service ném lỗi 404
        when(stationService.getVehicleStatsByStation(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy trạm"));

        // Thực hiện gọi API
        mockMvc.perform(get("/api/vehicles/station/999"))
                .andExpect(status().isNotFound()); // Mong đợi 404 Not Found
    }

    // --- Tests cho GET /api/vehicles/{vehicleId}/availability ---

    @Test
    void testCheckVehicleScheduleAvailability_KhiXeKhaDung() throws Exception {
        // Dữ liệu giả trả về
        Map<String, Object> mockAvailability = Map.of(
                "isAvailable", true,
                "message", "Xe khả dụng trong khung giờ này."
        );

        // Giả lập service
        when(vehicleService.checkVehicleSchedule(1L, startTime, endTime)).thenReturn(mockAvailability);

        mockMvc.perform(get("/api/vehicles/1/availability")
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void testCheckVehicleScheduleAvailability_KhiXeKhongKhaDung() throws Exception {
        Map<String, Object> mockAvailability = Map.of(
                "isAvailable", false,
                "message", "Xe đã có lịch đặt trong khung giờ này."
        );

        when(vehicleService.checkVehicleSchedule(2L, startTime, endTime)).thenReturn(mockAvailability);

        mockMvc.perform(get("/api/vehicles/2/availability")
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isAvailable").value(false))
                .andExpect(jsonPath("$.message").value("Xe đã có lịch đặt trong khung giờ này."));
    }

    @Test
    void testCheckVehicleScheduleAvailability_KhiXeKhongTimThay() throws Exception {
        when(vehicleService.checkVehicleSchedule(999L, startTime, endTime))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe"));

        mockMvc.perform(get("/api/vehicles/999/availability")
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCheckVehicleScheduleAvailability_KhiThieuParamStartTime() throws Exception {
        mockMvc.perform(get("/api/vehicles/1/availability")
                        .param("endTime", endTime.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckVehicleScheduleAvailability_KhiThieuParamEndTime() throws Exception {
        mockMvc.perform(get("/api/vehicles/1/availability")
                        .param("startTime", startTime.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCheckVehicleScheduleAvailability_KhiSaiDinhDangNgay() throws Exception {
        // Không cần mock, Spring @DateTimeFormat sẽ ném lỗi
        mockMvc.perform(get("/api/vehicles/1/availability")
                        .param("startTime", "10-11-2025 10:00")
                        .param("endTime", endTime.toString()))
                .andExpect(status().isBadRequest());
    }
}