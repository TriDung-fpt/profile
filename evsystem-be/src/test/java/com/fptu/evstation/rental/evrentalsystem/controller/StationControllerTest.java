package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.VehicleResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StationController.class) // Chỉ định controller cần test
class StationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // SỬA LỖI: Dùng @MockBean thay vì @Mock
    @MockitoBean
    private StationService stationService;
    @MockitoBean
    private ModelService modelService;
    @MockitoBean
    private RatingService ratingService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private VehicleService vehicleService;

    // --- Biến dùng chung ---
    private Station mockStationActive;
    private Station mockStationInactive;
    private User mockUserActive;
    private User mockUserInactive;
    private Rating mockRating;
    private Role mockUserRole;
    private final String MOCK_TOKEN_HEADER = "Bearer valid.token";
    private final String RAW_TOKEN = "valid.token";

    @BeforeEach
    void setUp() {
        mockUserRole = Role.builder().roleId(1L).roleName("USER").build();
        mockStationActive = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .status(StationStatus.ACTIVE) // Trạng thái ACTIVE
                .build();

        mockStationInactive = Station.builder()
                .stationId(2L)
                .name("Trạm Evolve - Quận 2 (Bảo trì)")
                .status(StationStatus.INACTIVE) // Trạng thái INACTIVE
                .build();
        mockUserActive = User.builder()
                .userId(1L)
                .status(AccountStatus.ACTIVE)
                .role(mockUserRole) // <-- Gán role vào đây
                .build();

        mockUserInactive = User.builder()
                .userId(2L)
                .status(AccountStatus.INACTIVE)
                .role(mockUserRole) // <-- Gán role vào đây
                .build();

        mockRating = Rating.builder().id(1L).stars(5).build();


        // Giả lập AuthService luôn thành công (happy path)
        when(authService.getTokenFromHeader(MOCK_TOKEN_HEADER)).thenReturn(RAW_TOKEN);
        when(authService.validateTokenAndGetUser(RAW_TOKEN)).thenReturn(mockUserActive);
    }

    // --- Test 1: GET /api/stations ---
    @Test
    void testGetAllPublicStations_Success() throws Exception {
        when(stationService.getAllStations()).thenReturn(Collections.singletonList(mockStationActive));

        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(stationService, times(1)).getAllStations();
    }

    // --- Test 2: GET /api/stations/{stationId} ---
    @Test
    void testGetPublicStationDetail_Success_Active() throws Exception {
        // Test trường hợp tìm thấy và trạm ACTIVE
        when(stationService.getStationById(1L)).thenReturn(mockStationActive);

        mockMvc.perform(get("/api/stations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value(1L));

        verify(stationService, times(1)).getStationById(1L);
    }

    @Test
    void testGetPublicStationDetail_Fail_Inactive() throws Exception {
        // Test trường hợp tìm thấy nhưng trạm INACTIVE (logic if trong controller)
        when(stationService.getStationById(2L)).thenReturn(mockStationInactive);

        mockMvc.perform(get("/api/stations/2"))
                .andExpect(status().isNotFound()); // Controller sẽ ném 404

        verify(stationService, times(1)).getStationById(2L);
    }

    @Test
    void testGetPublicStationDetail_Fail_NotFoundInService() throws Exception {
        // Test trường hợp service ném lỗi 404
        when(stationService.getStationById(999L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Station not found"));

        mockMvc.perform(get("/api/stations/999"))
                .andExpect(status().isNotFound());

        verify(stationService, times(1)).getStationById(999L);
    }

    // --- Test 3: GET /api/stations/{stationId}/models ---
    @Test
    void testGetModelsByStation_Success() throws Exception {
        List<ModelResponse> models = Collections.singletonList(ModelResponse.builder().modelId(1L).build());
        when(modelService.getModelsByStation(1L, "vinf", VehicleType.CAR)).thenReturn(models);

        mockMvc.perform(get("/api/stations/1/models")
                        .param("keyword", "vinf")
                        .param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(modelService, times(1)).getModelsByStation(1L, "vinf", VehicleType.CAR);
    }

    // --- Test 4: GET /api/stations/{stationId}/models/{modelId}/vehicles ---
    @Test
    void testGetVehicles_Success_UserLoggedIn() throws Exception {
        // Test trường hợp user đã đăng nhập (Path 1a)
        List<VehicleResponse> vehicles = Collections.singletonList(VehicleResponse.builder().vehicleId(1L).build());
        when(vehicleService.getVehiclesByModelAndStation(2L, 1L, mockUserActive)).thenReturn(vehicles);

        mockMvc.perform(get("/api/stations/1/models/2/vehicles")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Xác minh service được gọi với user
        verify(vehicleService, times(1)).getVehiclesByModelAndStation(2L, 1L, mockUserActive);
    }

    @Test
    void testGetVehicles_Success_Anonymous() throws Exception {
        // Test trường hợp user không đăng nhập (Path 2)
        List<VehicleResponse> vehicles = Collections.singletonList(VehicleResponse.builder().vehicleId(1L).build());
        when(vehicleService.getVehiclesByModelAndStation(2L, 1L, null)).thenReturn(vehicles);

        mockMvc.perform(get("/api/stations/1/models/2/vehicles")) // Không có header Authorization
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Xác minh service được gọi với user=null
        verify(vehicleService, times(1)).getVehiclesByModelAndStation(2L, 1L, null);
    }

    @Test
    void testGetVehicles_Fails_InvalidToken() throws Exception { // ĐÃ ĐỔI TÊN
        // Test trường hợp user có header nhưng token sai
        // Dùng ResponseStatusException là cách mock chính xác nhất cho việc auth thất bại
        // Filter sẽ bắt được lỗi này và trả về 401
        when(authService.validateTokenAndGetUser(RAW_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Token"));

        // Không cần mock vehicleService vì controller sẽ không được gọi

        mockMvc.perform(get("/api/stations/1/models/2/vehicles")
                        .header("Authorization", MOCK_TOKEN_HEADER))
                .andExpect(status().isUnauthorized()); // Mong đợi 401

        // Xác minh service KHÔNG được gọi (vì filter đã chặn request)
        verify(vehicleService, times(0)).getVehiclesByModelAndStation(anyLong(), anyLong(), any());
    }

    // --- Test 5: POST /api/stations/rating ---
    @Test
    void testAddRating_Success() throws Exception {
        // Test trường hợp thành công (Path 1)
        when(ratingService.saveRating(1L, 5, "Great", mockUserActive)).thenReturn(mockRating);
        Map<String, Object> requestBody = Map.of("stationId", 1, "stars", 5, "comment", "Great");

        mockMvc.perform(post("/api/stations/rating")
                        .header("Authorization", MOCK_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                // --- SỬA LỖI: Thay "ratingId" bằng "id" để khớp với JSON trả về ---
                .andExpect(jsonPath("$.id").value(1L));

        verify(ratingService, times(1)).saveRating(1L, 5, "Great", mockUserActive);
    }



    @Test
    void testAddRating_Fail_UserLocked() throws Exception {
        // Test trường hợp user bị khóa (Path 3)
        when(authService.validateTokenAndGetUser(RAW_TOKEN)).thenReturn(mockUserInactive); // Override setup
        Map<String, Object> requestBody = Map.of("stationId", 1, "stars", 5, "comment", "Great");

        mockMvc.perform(post("/api/stations/rating")
                        .header("Authorization", MOCK_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isForbidden()); // Mong đợi 403

        // Xác minh service KHÔNG được gọi
        verify(ratingService, never()).saveRating(anyLong(), anyInt(), anyString(), any(User.class));
    }


    @Test
    void testAddRating_Fail_AuthError() throws Exception {
        // Test trường hợp token sai (Path 2)
        when(authService.validateTokenAndGetUser(RAW_TOKEN))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED)); // Override setup
        Map<String, Object> requestBody = Map.of("stationId", 1, "stars", 5, "comment", "Great");

        mockMvc.perform(post("/api/stations/rating")
                        .header("Authorization", MOCK_TOKEN_HEADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isUnauthorized()); // Mong đợi 401

        // Xác minh service KHÔNG được gọi
        verify(ratingService, never()).saveRating(anyLong(), anyInt(), anyString(), any(User.class));
    }

    // --- Test 6: GET /api/stations/rating/{stationId} ---
    @Test
    void testGetRatingsByStation_Success() throws Exception {
        when(ratingService.getRatingsByStation(1L)).thenReturn(Collections.singletonList(mockRating));

        mockMvc.perform(get("/api/stations/rating/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                // --- SỬA LỖI (CHÍNH XÁC): Thay "ratingId" bằng "id" ---
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(ratingService, times(1)).getRatingsByStation(1L);
    }


    // --- Test 7: GET /api/stations/rating/{stationId}/average ---
    @Test
    void testGetAverageRating_Success() throws Exception {
        when(ratingService.getAverageRating(1L)).thenReturn(4.5);

        mockMvc.perform(get("/api/stations/rating/1/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4.5));

        verify(ratingService, times(1)).getAverageRating(1L);
    }
}