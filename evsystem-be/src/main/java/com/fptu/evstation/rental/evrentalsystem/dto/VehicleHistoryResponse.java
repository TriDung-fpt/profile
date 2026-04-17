package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleHistoryResponse {
    private Long historyId;
    private VehicleType vehicleType;
    private String licensePlate;
    private String staffName;
    private String renterName;
    private String stationName;
    private String actionType;
    private String note;
    private Double batteryLevel;
    private Double mileage;
    private LocalDateTime actionTime;
    private String conditionBefore;
    private String conditionAfter;
    private String photoPath;
}
