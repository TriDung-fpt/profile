package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.CreateModelRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelSearchRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelWithAvailabilityResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateModelRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Model;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleType;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ModelService {
    Model createModel(CreateModelRequest requestList, List<MultipartFile> images);
    Model updateModel(Long id, UpdateModelRequest request, List<MultipartFile> images);
    void deleteModel(Long id);
    ModelResponse getModelDetailsById(Long id);
    Model getModelById(Long id);
    List<ModelResponse> getModelsByStation(Long stationId, String keyword, VehicleType vehicleType);
    List<ModelResponse> getAllModels(String keyword);

    // New methods for availability-based search
    List<ModelWithAvailabilityResponse> getAvailableModelsByStation(ModelSearchRequest searchRequest);
}
