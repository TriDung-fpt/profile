package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ContractSummaryResponse {
    private Long contractId;
    private Long bookingId;
    private String renterName;
    private String staffName;
    private String vehicleLicensePlate;
    private LocalDateTime signedDate;
    private String contractPdfPath;
}
