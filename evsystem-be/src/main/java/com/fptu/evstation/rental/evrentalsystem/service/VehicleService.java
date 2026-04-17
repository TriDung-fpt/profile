package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface VehicleService {
    VehicleResponse createVehicle(CreateVehicleRequest request);

    Vehicle updateVehicle(Long id, UpdateVehicleDetailsRequest request);

    void deleteVehicle(Long id);

    List<VehicleResponse> getAllVehiclesByStation(Station station);

    List<VehicleResponse> getAllVehicles(Long modelId, Long stationId, VehicleType vehicleType, String sortBy, String order);

    Vehicle getVehicleById(Long vehicleId);

    VehicleResponse getVehicleDetailsById(Long id);

    Vehicle saveVehicle(Vehicle vehicle);

    Vehicle updateVehicleDetails(User staff, Long vehicleId, UpdateVehicleDetailsRequest request);

    Vehicle reportMajorDamage(User staff, Long vehicleId, ReportDamageRequest request);

    VehicleHistory recordVehicleAction(Long vehicleId, Long staffId, Long renterId, Long stationId, VehicleActionType type, String note, String conditionBefore, String conditionAfter, Double battery, Double mileage, String photoPathsJson);

    List<VehicleHistoryResponse> getVehicleHistory(Long stationId, LocalDate from, LocalDate to, VehicleType vehicleType, String licensePlate);

    List<VehicleHistoryResponse> getHistoryByVehicle(Long vehicleId);

    List<VehicleHistoryResponse> getHistoryByRenter(Long renterId);

    List<VehicleResponse> getVehiclesByModelAndStation(Long modelId, Long stationId, User requestingUser);

    Map<String, Object> checkVehicleSchedule(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime);

    List<VehicleAvailabilityResponse> getAvailableVehiclesByModel(Long modelId, Long stationId, LocalDateTime startTime, LocalDateTime endTime);
}
