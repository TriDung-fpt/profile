package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DashboardSummaryDto {
    private String stationName;
    private long totalVehicles;
    private Map<VehicleStatus, Long> statusSummary;
}
