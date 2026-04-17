package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequest {
    private String licensePlate;
    private Integer batteryLevel;
    private Long modelId;
    private Long stationId;
    private Double currentMileage;
    private String status;
    private String condition;
}
