package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {
    private Long vehicleId;
    private String licensePlate;
    private Integer batteryLevel;
    private String modelName;
    private String stationName;
    private Long stationId;
    private Double currentMileage;
    private String status;
    private String condition;
    private boolean isReservedByMe;
    private boolean isRentedByMe;
    private VehicleType vehicleType;
    private Double pricePerHour;
    private Integer seatCount;
    private Double rangeKm;
    private String features;
    private String description;
    private List<String> imagePaths;
    private LocalDateTime createdAt;
}
