package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.VehicleCondition;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAvailabilityResponse {
    private Long vehicleId;
    private String licensePlate;
    private Integer batteryLevel;
    private Double currentMileage;
    private VehicleStatus status;
    private VehicleCondition condition;
    private LocalDateTime lastMaintenanceDate;
    private String modelName;
    private Long modelId;
    private String stationName;
    private Long stationId;

    // Indicates if this vehicle is available for the requested time period
    private Boolean isAvailable;
    private String availabilityNote;
    private LocalDateTime createdAt;
}
