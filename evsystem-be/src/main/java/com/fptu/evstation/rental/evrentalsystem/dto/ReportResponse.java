package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class ReportResponse {
    private String stationName;
    private Long stationId;
    private double totalBookingRevenue;
    private double totalPenaltyRevenue;
    private double totalRevenue;
    private LocalDate fromDate;
    private LocalDate toDate;
    private int totalTransactions;
}
