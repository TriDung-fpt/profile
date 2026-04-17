package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Chỉ định rõ ràng controller cần test
@WebMvcTest(AdminStationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminStationControllerTest {

    @Autowired
    private MockMvc mockMvc; // Dùng để giả lập các request HTTP

    @Autowired
    private ObjectMapper objectMapper; // Dùng để chuyển đổi object sang JSON

    // Sử dụng @MockBean để mock các service dependencies trong Spring Context
    // Đây là cách đúng khi dùng @WebMvcTest
    @MockitoBean
    private StationService stationService;
    @MockitoBean
    private ModelService modelService;
    @MockitoBean
    private VehicleService vehicleService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private UserRepository userRepository; // Controller này có inject UserRepository
    @MockitoBean
    private ReportService reportService;
    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private AuthService authService;
    // Chuẩn bị các đối tượng mock để trả về
    private Station mockStation;
    private Model mockModel;
    private VehicleResponse mockVehicleResponse;
    private User mockUser;
    private ReportResponse mockReportResponse;
    private VehicleHistoryResponse mockVehicleHistoryResponse;

    // Chuẩn bị các đối tượng DTO cho request body
    private StationRequest stationRequest;
    private UpdateStationRequest updateStationRequest;
    private CreateVehicleRequest createVehicleRequest;
    private UpdateVehicleDetailsRequest updateVehicleRequest;
    private RoleUpdateRequest roleUpdateRequest;

    @BeforeEach
    void setUp() {
        // Khởi tạo mock data
        mockStation = Station.builder().stationId(1L).name("Test Station").build();
        mockModel = Model.builder().modelId(1L).modelName("VinFast VF 3").build();
        mockVehicleResponse = VehicleResponse.builder().vehicleId(1L).licensePlate("12345").build();
        mockUser = User.builder().userId(1L).fullName("Test User").cancellationCount(0).build();
        mockReportResponse = ReportResponse.builder().totalRevenue(1000.0).build();
        mockVehicleHistoryResponse = VehicleHistoryResponse.builder().historyId(1L).build();

        // Khởi tạo request DTOs
        stationRequest = new StationRequest("Test Station", "123 Street", "Desc", "8-5", "19001009");
        updateStationRequest = new UpdateStationRequest();
        updateStationRequest.setName("Updated Station");
        createVehicleRequest = new CreateVehicleRequest("51H-12345", 80, 1L, 1L, 1000.0, "AVAILABLE", "GOOD");
        updateVehicleRequest = new UpdateVehicleDetailsRequest();
        updateVehicleRequest.setLicensePlate("51H-67890");
        roleUpdateRequest = new RoleUpdateRequest(2L);
    }

    // --- 1. Quản lý Trạm (Stations) ---

    @Test
    void testCreateStation_Success() throws Exception {
        when(stationService.addStation(any(StationRequest.class))).thenReturn(mockStation);

        mockMvc.perform(post("/api/admin/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value(1L));
    }

        @Test
        void testGetAllStations_Success() throws Exception {
            when(stationService.getAllStations()).thenReturn(Collections.singletonList(mockStation));

            mockMvc.perform(get("/api/admin/stations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].stationId").value(1L));
        }

    @Test
    void testUpdateStation_Success() throws Exception {
        when(stationService.updateStation(eq(1L), any(UpdateStationRequest.class))).thenReturn(mockStation);

        mockMvc.perform(put("/api/admin/stations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value(1L));
    }

    @Test
    void testDeleteStation_Success() throws Exception {
        doNothing().when(stationService).deleteStation(1L);

        mockMvc.perform(delete("/api/admin/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cửa hàng với ID 1 đã được xóa thành công!"));
    }

    @Test
    void testDeleteStation_Fail_ResponseStatusException() throws Exception {
        // Test block catch (ResponseStatusException e)
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạm vẫn còn xe");
        doThrow(ex).when(stationService).deleteStation(1L);

        mockMvc.perform(delete("/api/admin/stations/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Trạm vẫn còn xe"));
    }

    @Test
    void testDeleteStation_Fail_InternalServerError() throws Exception {
        // Test block catch (Exception e)
        doThrow(new RuntimeException("Database error")).when(stationService).deleteStation(1L);

        mockMvc.perform(delete("/api/admin/stations/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Lỗi hệ thống: Database error"));
    }

    // --- 2. Quản lý Xe (Vehicles) ---

    @Test
    void testCreateVehicle_Success() throws Exception {
        when(vehicleService.createVehicle(any(CreateVehicleRequest.class))).thenReturn(mockVehicleResponse);

        mockMvc.perform(post("/api/admin/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createVehicleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(1L));
    }

    @Test
    void testGetAllVehicles_Success() throws Exception {
        when(vehicleService.getAllVehicles(any(), any(), any(), any(), any())).thenReturn(Collections.singletonList(mockVehicleResponse));

        mockMvc.perform(get("/api/admin/vehicles")
                        .param("sortBy", "createdAt")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(1L));
    }

    @Test
    void testGetVehicleDetailsById_Success() throws Exception {
        when(vehicleService.getVehicleDetailsById(1L)).thenReturn(mockVehicleResponse);
        mockMvc.perform(get("/api/admin/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(1L));
    }

    @Test
    void testGetVehicleDetailsById_Fail_NotFound() throws Exception {
        // Test block catch (ResponseStatusException e)
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe");
        when(vehicleService.getVehicleDetailsById(1L)).thenThrow(ex);
        mockMvc.perform(get("/api/admin/vehicles/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy xe"));
    }

    @Test
    void testGetVehicleDetailsById_Fail_InternalError() throws Exception {
        // Test block catch (Exception e)
        when(vehicleService.getVehicleDetailsById(1L)).thenThrow(new RuntimeException("DB error"));
        mockMvc.perform(get("/api/admin/vehicles/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Lỗi hệ thống: DB error"));
    }

    @Test
    void testUpdateVehicle_Success() throws Exception {
        Vehicle updatedVehicle = Vehicle.builder().vehicleId(1L).build();
        when(vehicleService.updateVehicle(eq(1L), any(UpdateVehicleDetailsRequest.class))).thenReturn(updatedVehicle);

        mockMvc.perform(put("/api/admin/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVehicleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xe đã được cập nhật thành công!"));
    }

    @Test
    void testUpdateVehicle_Fail_ResponseStatusException() throws Exception {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe");
        when(vehicleService.updateVehicle(eq(1L), any(UpdateVehicleDetailsRequest.class))).thenThrow(ex);

        mockMvc.perform(put("/api/admin/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVehicleRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy xe"));
    }

    @Test
    void testUpdateVehicle_Fail_InternalError() throws Exception {
        when(vehicleService.updateVehicle(eq(1L), any(UpdateVehicleDetailsRequest.class))).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(put("/api/admin/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVehicleRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Lỗi hệ thống: DB error"));
    }

    @Test
    void testDeleteVehicle_Success() throws Exception {
        doNothing().when(vehicleService).deleteVehicle(1L);
        mockMvc.perform(delete("/api/admin/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xe với ID 1 đã được xóa thành công!"));
    }

    @Test
    void testDeleteVehicle_Fail_ResponseStatusException() throws Exception {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xe đang được thuê");
        doThrow(ex).when(vehicleService).deleteVehicle(1L);
        mockMvc.perform(delete("/api/admin/vehicles/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Xe đang được thuê"));
    }

    @Test
    void testDeleteVehicle_Fail_InternalError() throws Exception {
        doThrow(new RuntimeException("DB error")).when(vehicleService).deleteVehicle(1L);
        mockMvc.perform(delete("/api/admin/vehicles/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Lỗi hệ thống: DB error"));
    }

    // --- 3. Quản lý Mẫu xe (Models) ---

    @Test
    void testGetAllModels_Success() throws Exception {
        ModelResponse mockResponse = ModelResponse.builder().modelId(1L).build();
        when(modelService.getAllModels(any())).thenReturn(Collections.singletonList(mockResponse));

        mockMvc.perform(get("/api/admin/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelId").value(1L));
    }

    @Test
    void testCreateModel_Success() throws Exception {
        when(modelService.createModel(any(CreateModelRequest.class), any())).thenReturn(mockModel);

        MockMultipartFile image = new MockMultipartFile("images", "test.png", MediaType.IMAGE_PNG_VALUE, "test".getBytes());

        // Test request dạng multipart
        mockMvc.perform(multipart("/api/admin/models")
                        .file(image)
                        .param("modelName", "Test Model")
                        .param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(1L));
    }

    @Test
    void testUpdateModel_Success() throws Exception {
        when(modelService.updateModel(eq(1L), any(UpdateModelRequest.class), any())).thenReturn(mockModel);

        MockMultipartFile newImage = new MockMultipartFile(
                "newImages", "new.png", MediaType.IMAGE_PNG_VALUE, "new".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/models/1")
                        .file(newImage)
                        .param("modelName", "Updated Model")
                        .with(request -> {
                            request.setMethod("PUT"); // ✅ Ép multipart thành PUT
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(" Cập nhật model thành công!"));
    }


    @Test
    void testUpdateModel_Fail_ResponseStatusException() throws Exception {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy model");
        when(modelService.updateModel(eq(1L), any(UpdateModelRequest.class), any())).thenThrow(ex);

        mockMvc.perform(multipart("/api/admin/models/1")
                        .param("modelName", "Updated Model")
                        .with(request -> { request.setMethod("PUT"); return request; })) // ✅ Đặt ở đây
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy model"));
    }

    @Test
    void testUpdateModel_Fail_InternalError() throws Exception {
        // Giả lập service ném RuntimeException
        when(modelService.updateModel(eq(1L), any(UpdateModelRequest.class), any()))
                .thenThrow(new RuntimeException("DB error"));

        // Gửi request multipart PUT
        mockMvc.perform(multipart("/api/admin/models/1")
                        .param("modelName", "Updated Model")
                        .with(request -> { request.setMethod("PUT"); return request; })) // Ép thành PUT
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("DB error"));
    }


    @Test
    void testGetModelDetailsById_Success() throws Exception {
        ModelResponse mockResponse = ModelResponse.builder().modelId(1L).build();
        when(modelService.getModelDetailsById(1L)).thenReturn(mockResponse);
        mockMvc.perform(get("/api/admin/models/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelId").value(1L));
    }

    @Test
    void testGetModelDetailsById_Fail_NotFound() throws Exception {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy model");
        when(modelService.getModelDetailsById(1L)).thenThrow(ex);
        mockMvc.perform(get("/api/admin/models/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy model"));
    }

    @Test
    void testGetModelDetailsById_Fail_InternalError() throws Exception {
        when(modelService.getModelDetailsById(1L)).thenThrow(new RuntimeException("DB error"));
        mockMvc.perform(get("/api/admin/models/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Lỗi hệ thống: DB error"));
    }

    @Test
    void testDeleteModel_Success() throws Exception {
        doNothing().when(modelService).deleteModel(1L);
        mockMvc.perform(delete("/api/admin/models/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(" Model với ID 1 đã được xóa thành công!"));
    }

    @Test
    void testDeleteModel_Fail_ResponseStatusException() throws Exception {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model đang được sử dụng");
        doThrow(ex).when(modelService).deleteModel(1L);
        mockMvc.perform(delete("/api/admin/models/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Model đang được sử dụng"));
    }

    @Test
    void testDeleteModel_Fail_InternalError() throws Exception {
        doThrow(new RuntimeException("DB error")).when(modelService).deleteModel(1L);
        mockMvc.perform(delete("/api/admin/models/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("DB error"));
    }

    // --- 4. Quản lý Người dùng (Users) ---

    @Test
    void testGetAllUsers_Success() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(mockUser));
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1L));
    }

    @Test
    void testGetUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(mockUser);
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void testUpdateUserStatus_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(mockUser);
        when(userService.saveUser(any(User.class))).thenReturn(mockUser);

        // Test trường hợp khóa tài khoản
        mockMvc.perform(patch("/api/admin/users/1/status")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("INACTIVE"));

        // Test trường hợp mở khóa (phải reset cancellationCount)
        mockUser.setCancellationCount(5); // Giả lập user bị khóa
        mockUser.setStatus(AccountStatus.INACTIVE);

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("ACTIVE"));

        // Xác minh
        verify(userService, times(2)).getUserById(1L);
        verify(userService, times(2)).saveUser(any(User.class));
        // Đảm bảo logic reset count được gọi
        org.junit.jupiter.api.Assertions.assertEquals(0, mockUser.getCancellationCount());
    }

    @Test
    void testUpdateUserRole_Success() throws Exception {
        when(userService.updateUserRole(1L, 2L)).thenReturn(mockUser);
        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleUpdateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void testGetStaffByStation_Success() throws Exception {
        // Controller này gọi thẳng UserRepository, nên ta mock UserRepository
        when(userRepository.findByStation_StationId(1L)).thenReturn(Collections.singletonList(mockUser));
        mockMvc.perform(get("/api/admin/staff/station/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1L));
    }

    @Test
    void testUpdateUserStation_Success() throws Exception {
        when(userService.updateUserStation(1L, 2L)).thenReturn(mockUser);
        // Lưu ý: Endpoint này không có /users/
        mockMvc.perform(put("/api/admin/1/station/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    // --- 5. Báo cáo & Thống kê (Reports & Stats) ---

    @Test
    void testGetRevenueReport_ByStation() throws Exception {
        // Test trường hợp có stationId
        when(reportService.getRevenueByStation(eq(1L), any(LocalDate.class), any(LocalDate.class))).thenReturn(mockReportResponse);

        mockMvc.perform(get("/api/admin/revenue")
                        .param("stationId", "1")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(1000.0));
    }

    @Test
    void testGetRevenueReport_Total() throws Exception {
        // Test trường hợp không có stationId
        when(reportService.getTotalRevenue(any(LocalDate.class), any(LocalDate.class))).thenReturn(mockReportResponse);

        mockMvc.perform(get("/api/admin/revenue")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(1000.0));
    }

    @Test
    void testGetAllStationReports_Success() throws Exception {
        List<Map<String, Object>> reports = Collections.singletonList(Map.of("stationName", "Station A", "totalVehicles", 10L));
        when(stationService.getAllStationReports()).thenReturn(reports);

        mockMvc.perform(get("/api/admin/stations/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stationName").value("Station A"));
    }

    @Test
    void testGetAllVehicleHistory_Success() throws Exception {
        when(vehicleService.getVehicleHistory(any(), any(), any(), any(), any())).thenReturn(Collections.singletonList(mockVehicleHistoryResponse));

        mockMvc.perform(get("/api/admin/vehicle-history")
                        .param("stationId", "1")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31")
                        .param("vehicleType", "CAR")
                        .param("licensePlate", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].historyId").value(1L));
    }

    @Test
    void testGetHistoryByVehicle_Success() throws Exception {
        when(vehicleService.getHistoryByVehicle(1L)).thenReturn(Collections.singletonList(mockVehicleHistoryResponse));

        mockMvc.perform(get("/api/admin/vehicle-history/vehicle/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].historyId").value(1L));
    }

    @Test
    void testGetHistoryByRenter_Success() throws Exception {
        when(vehicleService.getHistoryByRenter(1L)).thenReturn(Collections.singletonList(mockVehicleHistoryResponse));

        mockMvc.perform(get("/api/admin/vehicle-history/renter/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].historyId").value(1L));
    }

    @Test
    void testGetPeakHourStats_Success() throws Exception {
        Map<String, Object> stats = Map.of("peakHour", "10:00 - 11:00", "totalRentals", 50L);
        when(bookingService.getPeakHourStatistics(any(), any(), any())).thenReturn(stats);

        mockMvc.perform(get("/api/admin/statistics/peak-hour")
                        .param("stationId", "1")
                        .param("fromDate", "2025-01-01")
                        .param("toDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peakHour").value("10:00 - 11:00"));
    }
}