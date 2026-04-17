package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelWithAvailabilityResponse {
    private Long modelId;
    private String modelName;
    private VehicleType vehicleType;
    private Integer seatCount;
    private Double batteryCapacity;
    private Double rangeKm;
    private Double pricePerHour;
    private Double initialValue;
    private String features;
    private String description;
    private List<String> imagePaths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Availability information
    private Integer availableVehicleCount;
    private Integer totalVehicleCount;
}

