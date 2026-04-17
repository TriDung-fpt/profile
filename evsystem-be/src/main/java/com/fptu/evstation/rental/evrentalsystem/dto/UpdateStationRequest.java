package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.StationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStationRequest {
    private String name;
    private String address;
    private String description;
    private String openingHours;
    private String hotline;
    private String status;
}
