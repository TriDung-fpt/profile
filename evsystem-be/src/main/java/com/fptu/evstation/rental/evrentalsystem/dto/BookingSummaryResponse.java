package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class BookingSummaryResponse {
    private Long bookingId;
    private String renterName;
    private String vehicleLicensePlate;
    private String modelName;
    private VehicleStatus vehicleStatus;
    private Integer batteryLevel;
    private Double currentMileage;
    private BookingStatus bookingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;

    private String renterPhone;
    private Double refundAmount;
    private String refundInfo;
}
