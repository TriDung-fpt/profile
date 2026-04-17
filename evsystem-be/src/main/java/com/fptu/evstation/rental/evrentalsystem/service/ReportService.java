package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.ReportResponse;

import java.time.LocalDate;

public interface ReportService {
    ReportResponse getRevenueByStation(Long stationId, LocalDate from, LocalDate to);
    ReportResponse getTotalRevenue(LocalDate from, LocalDate to);
}
