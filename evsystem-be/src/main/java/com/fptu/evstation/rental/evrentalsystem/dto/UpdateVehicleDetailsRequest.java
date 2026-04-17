package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.VehicleCondition;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVehicleDetailsRequest {
    private String licensePlate;
    private Long modelId;
    private Long stationId;
    private Double currentMileage;
    @Min(value = 0, message = "Mức pin không thể âm")
    @Max(value = 100, message = "Mức pin không thể lớn hơn 100")
    private Integer batteryLevel;
    private VehicleCondition newCondition;
    private VehicleStatus status;

}
