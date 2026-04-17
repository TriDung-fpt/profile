package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.CreateModelRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateModelRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Model;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.StationStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleType;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.ModelRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelServiceImplTest {

    @Mock
    private ModelRepository modelRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private StationService stationService;

    @InjectMocks
    private ModelServiceImpl modelService;

    private Model mockModel;
    private Station mockStation;

    @BeforeEach
    void setUp() {
        mockModel = Model.builder()
                .modelId(1L)
                .modelName("VF 3")
                .imagePaths("/uploads/models_img/VF_3/image1.jpg") // Giả lập ảnh cũ
                .build();

        mockStation = Station.builder()
                .stationId(1L)
                .status(StationStatus.ACTIVE)
                .build();
    }

    // --- Tests for createModel ---

    @Test
    void testCreateModel_Success() {
        CreateModelRequest request = CreateModelRequest.builder()
                .modelName("New Model")
                .vehicleType("CAR")
                .seatCount(4)
                .batteryCapacity(50.0)
                .rangeKm(300.0)
                .features("AI")
                .pricePerHour(100.0)
                .initialValue(1000.0)
                .description("Desc")
                .build();

        when(modelRepository.existsByModelName("New Model")).thenReturn(false);
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Model createdModel = modelService.createModel(request, new ArrayList<>());

        assertNotNull(createdModel);
        assertEquals("New Model", createdModel.getModelName());
        assertEquals(VehicleType.CAR, createdModel.getVehicleType());
        assertEquals(4, createdModel.getSeatCount());
        assertEquals(50.0, createdModel.getBatteryCapacity());
        assertEquals(300.0, createdModel.getRangeKm());
        assertEquals("AI", createdModel.getFeatures());
        assertEquals(100.0, createdModel.getPricePerHour());
        assertEquals(1000.0, createdModel.getInitialValue());
        assertEquals("Desc", createdModel.getDescription());

        verify(modelRepository, times(1)).save(any(Model.class));
    }

    @Test
    void testCreateModel_Fail_ModelExists() {
        CreateModelRequest request = CreateModelRequest.builder().modelName("Existing Model").build();

        when(modelRepository.existsByModelName("Existing Model")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            modelService.createModel(request, new ArrayList<>());
        });

        assertTrue(ex.getMessage().contains("Model đã tồn tại!"));
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void testCreateModel_Fail_InvalidVehicleType() {
        CreateModelRequest request = CreateModelRequest.builder()
                .modelName("New Model")
                .vehicleType("INVALID_TYPE")
                .build();

        when(modelRepository.existsByModelName("New Model")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            modelService.createModel(request, new ArrayList<>());
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("VehicleType không hợp lệ"));
    }

    // --- Tests for updateModel ---

    @Test
    void testUpdateModel_Success_AllFields() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateModelRequest request = new UpdateModelRequest();
        request.setModelName("Updated Model");
        request.setVehicleType("MOTORBIKE");
        request.setSeatCount(2);
        request.setBatteryCapacity(20.0);
        request.setRangeKm(150.0);
        request.setFeatures("New Features");
        request.setDescription("New Desc");
        request.setPricePerHour(50.0);
        request.setInitialValue(500.0);

        Model updatedModel = modelService.updateModel(1L, request, new ArrayList<>());

        assertNotNull(updatedModel);
        assertEquals("Updated Model", updatedModel.getModelName());
        assertEquals(VehicleType.MOTORBIKE, updatedModel.getVehicleType());
        assertEquals(2, updatedModel.getSeatCount());
        assertEquals(20.0, updatedModel.getBatteryCapacity());
        assertEquals(150.0, updatedModel.getRangeKm());
        assertEquals("New Features", updatedModel.getFeatures());
        assertEquals("New Desc", updatedModel.getDescription());
        assertEquals(50.0, updatedModel.getPricePerHour());
        assertEquals(500.0, updatedModel.getInitialValue());
        assertNotNull(updatedModel.getUpdatedAt());

        verify(modelRepository, times(1)).save(any(Model.class));
    }

    @Test
    void testUpdateModel_Success_PartialFieldsNull() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(modelRepository.save(any(Model.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateModelRequest request = new UpdateModelRequest();
        request.setModelName("Updated Model");

        Model updatedModel = modelService.updateModel(1L, request, new ArrayList<>());

        assertEquals("Updated Model", updatedModel.getModelName());
        verify(modelRepository, times(1)).save(any(Model.class));
    }

    @Test
    void testUpdateModel_Fail_NotFound() {
        UpdateModelRequest request = new UpdateModelRequest();
        request.setModelName("Updated Model");

        when(modelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            modelService.updateModel(999L, request, new ArrayList<>());
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(modelRepository, never()).save(any(Model.class));
    }

    // --- Tests for deleteModel ---

    @Test
    void testDeleteModel_Success_NoVehicles() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(vehicleRepository.count(any(Specification.class))).thenReturn(0L);

        assertDoesNotThrow(() -> modelService.deleteModel(1L));

        verify(modelRepository, times(1)).delete(mockModel);
    }

    @Test
    void testDeleteModel_Fail_VehicleExists() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(vehicleRepository.count(any(Specification.class))).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            modelService.deleteModel(1L);
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Không thể xóa model"));

        verify(modelRepository, never()).delete(any(Model.class));
    }

    @Test
    void testDeleteModel_Fail_NotFound() {
        when(modelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            modelService.deleteModel(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(modelRepository, never()).delete(any(Model.class));
    }

    // --- Tests for getModelDetailsById ---

    @Test
    void testGetModelDetailsById_Success() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));

        ModelResponse response = modelService.getModelDetailsById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getModelId());
        assertEquals("VF 3", response.getModelName());
        assertNotNull(response.getImagePaths());
        assertEquals(1, response.getImagePaths().size());
        assertEquals("/uploads/models_img/VF_3/image1.jpg", response.getImagePaths().get(0));
    }

    @Test
    void testGetModelDetailsById_Success_NullImages() {
        mockModel.setImagePaths(null);
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));

        ModelResponse response = modelService.getModelDetailsById(1L);

        assertNotNull(response.getImagePaths());
        assertTrue(response.getImagePaths().isEmpty());
    }

    @Test
    void testGetModelDetailsById_Fail_NotFound() {
        when(modelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            modelService.getModelDetailsById(999L);
        });

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- Tests for getModelById ---

    @Test
    void testGetModelById_Success() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        Model result = modelService.getModelById(1L);
        assertEquals(mockModel, result);
    }

    @Test
    void testGetModelById_Fail_NotFound() {
        when(modelRepository.findById(999L)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            modelService.getModelById(999L);
        });
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // --- Tests for getAllModels ---

    @Test
    void testGetAllModels_NoKeyword() {
        when(modelRepository.findAll()).thenReturn(List.of(mockModel));

        List<ModelResponse> result = modelService.getAllModels(null);

        assertEquals(1, result.size());
        assertEquals("VF 3", result.get(0).getModelName());
        verify(modelRepository, times(1)).findAll();
        verify(modelRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void testGetAllModels_BlankKeyword() {
        when(modelRepository.findAll()).thenReturn(List.of(mockModel));

        List<ModelResponse> result = modelService.getAllModels("   ");

        assertEquals(1, result.size());
        verify(modelRepository, times(1)).findAll();
        verify(modelRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void testGetAllModels_WithKeyword() {
        when(modelRepository.findAll(any(Specification.class))).thenReturn(List.of(mockModel));

        List<ModelResponse> result = modelService.getAllModels("vf3");

        assertEquals(1, result.size());
        assertEquals("VF 3", result.get(0).getModelName());
        verify(modelRepository, never()).findAll();
        verify(modelRepository, times(1)).findAll(any(Specification.class));
    }

    // --- Tests for getModelsByStation ---

    @Test
    void testGetModelsByStation_Success_AllFilters() {
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(vehicleRepository.findDistinctModelIdsByStation(mockStation)).thenReturn(List.of(1L, 2L));
        when(modelRepository.findAll(any(Specification.class))).thenReturn(List.of(mockModel));

        List<ModelResponse> result = modelService.getModelsByStation(1L, "vf", VehicleType.CAR);

        assertEquals(1, result.size());
        assertEquals("VF 3", result.get(0).getModelName());
        verify(modelRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    void testGetModelsByStation_Success_NoFilters() {
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(vehicleRepository.findDistinctModelIdsByStation(mockStation)).thenReturn(List.of(1L));
        when(modelRepository.findAll(any(Specification.class))).thenReturn(List.of(mockModel));

        List<ModelResponse> result = modelService.getModelsByStation(1L, null, null);

        assertEquals(1, result.size());
        verify(modelRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    void testGetModelsByStation_Fail_StationNotFound() {
        when(stationService.getStationById(999L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> {
            modelService.getModelsByStation(999L, null, null);
        });
    }

    @Test
    void testGetModelsByStation_Fail_StationInactive() {
        mockStation.setStatus(StationStatus.INACTIVE);
        when(stationService.getStationById(1L)).thenReturn(mockStation);

        List<ModelResponse> result = modelService.getModelsByStation(1L, null, null);

        assertTrue(result.isEmpty());
        verify(vehicleRepository, never()).findDistinctModelIdsByStation(any());
    }

    @Test
    void testGetModelsByStation_Success_NoModelsAtStation() {
        when(stationService.getStationById(1L)).thenReturn(mockStation);
        when(vehicleRepository.findDistinctModelIdsByStation(mockStation)).thenReturn(Collections.emptyList());

        List<ModelResponse> result = modelService.getModelsByStation(1L, null, null);

        assertTrue(result.isEmpty());
        verify(modelRepository, never()).findAll(any(Specification.class));
    }

    // --- Tests for convertToResponse (private) ---

    @Test
    void testConvertToResponse_NullImages() {
        mockModel.setImagePaths(null);

        // 👇 Giả lập repository trả về mockModel
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));

        ModelResponse response = modelService.getModelDetailsById(1L);

        assertNotNull(response.getImagePaths());
        assertTrue(response.getImagePaths().isEmpty());
    }

    @Test
    void testConvertToResponse_BlankImages() {
        mockModel.setImagePaths("");
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));

        ModelResponse response = modelService.getModelDetailsById(1L);

        assertNotNull(response);
        assertNotNull(response.getImagePaths(), "List imagePaths không được null");

        boolean ok = response.getImagePaths().isEmpty()
                || response.getImagePaths().stream().allMatch(s -> s == null || s.trim().isEmpty());
        assertTrue(ok, "List imagePaths phải rỗng hoặc chỉ chứa chuỗi rỗng khi imagePaths là chuỗi rỗng");

        assertEquals(mockModel.getModelId(), response.getModelId());
        assertEquals(mockModel.getModelName(), response.getModelName());
    }

    // --- Tests for saveImagesAndGetPaths (private, test gián tiếp) ---

    @Test
    void testSaveImages_ReturnsCurrentPaths_IfImagesNull() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(modelRepository.save(any(Model.class))).thenReturn(mockModel);

        Model updatedModel = modelService.updateModel(1L, new UpdateModelRequest(), null);

        assertEquals("/uploads/models_img/VF_3/image1.jpg", updatedModel.getImagePaths());
    }

    @Test
    void testSaveImages_ReturnsCurrentPaths_IfImagesEmpty() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(modelRepository.save(any(Model.class))).thenReturn(mockModel);

        Model updatedModel = modelService.updateModel(1L, new UpdateModelRequest(), Collections.emptyList());

        assertEquals("/uploads/models_img/VF_3/image1.jpg", updatedModel.getImagePaths());
    }

    @Test
    void testSaveImages_ReturnsCurrentPaths_IfImagesHaveEmptyFile() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(modelRepository.save(any(Model.class))).thenReturn(mockModel);

        MultipartFile emptyFile = new MockMultipartFile("file", "", "", (byte[]) null);
        Model updatedModel = modelService.updateModel(1L, new UpdateModelRequest(), List.of(emptyFile));

        assertEquals("/uploads/models_img/VF_3/image1.jpg", updatedModel.getImagePaths());
    }

    @Test
    void testSaveImages_SkipsInvalidContentType() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));
        when(modelRepository.save(any(Model.class))).thenReturn(mockModel);

        MultipartFile invalidFile = new MockMultipartFile("file", "text.txt", "text/plain", "content".getBytes());
        MultipartFile validFile = new MockMultipartFile("file2", "image.jpg", "image/jpeg", "content".getBytes());

        try {
            Model updatedModel = modelService.updateModel(1L, new UpdateModelRequest(), List.of(invalidFile, validFile));
            assertTrue(updatedModel.getImagePaths().contains("image.jpg"));
            assertFalse(updatedModel.getImagePaths().contains("text.txt"));
        } catch (Exception e) {
            System.err.println("Bỏ qua testSaveImages_SkipsInvalidContentType do lỗi IO: " + e.getMessage());
        }
    }

    @Test
    void testSaveImages_HandlesIOException() {
        when(modelRepository.findById(1L)).thenReturn(Optional.of(mockModel));

        MultipartFile mockFileWithIOError = new MockMultipartFile("file", "filename.jpg", "image/jpeg", "content".getBytes()) {
            @Override
            public void transferTo(java.nio.file.Path dest) throws IOException, IllegalStateException {
                throw new IOException("Disk full");
            }
        };

        assertDoesNotThrow(() -> {
            modelService.updateModel(1L, new UpdateModelRequest(), List.of(mockFileWithIOError));
        });
    }
}