package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class InvoiceSummaryResponse {
    private Long bookingId;
    private String renterName;
    private Double finalAmount;
    private LocalDateTime createdDate;
    private String invoicePdfPath;
}
