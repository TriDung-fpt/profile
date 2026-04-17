package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.ReportResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.Transaction;
import com.fptu.evstation.rental.evrentalsystem.entity.TransactionDetail;
import com.fptu.evstation.rental.evrentalsystem.repository.TransactionDetailRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.TransactionRepository;
import com.fptu.evstation.rental.evrentalsystem.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransactionRepository transactionRepository;
    private final TransactionDetailRepository transactionDetailRepository;

    @Override
    public ReportResponse getRevenueByStation(Long stationId, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(stationId, fromDateTime, toDateTime);

        List<TransactionDetail> penaltyDetails = transactionDetailRepository
                .findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(stationId, fromDateTime, toDateTime);

        double totalTransactionAmount = transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalPenaltyAmount = penaltyDetails.stream()
                .mapToDouble(TransactionDetail::getAppliedAmount)
                .sum();

        double totalRevenue = totalTransactionAmount + totalPenaltyAmount;

        String stationName = transactions.isEmpty()
                ? "Không có giao dịch"
                : transactions.get(0).getBooking().getVehicle().getStation().getName();

        return ReportResponse.builder()
                .stationName(stationName)
                .stationId(stationId)
                .fromDate(from)
                .toDate(to)
                .totalBookingRevenue(totalTransactionAmount)
                .totalPenaltyRevenue(totalPenaltyAmount)
                .totalRevenue(totalRevenue)
                .totalTransactions(transactions.size())
                .build();
    }

    @Override
    public ReportResponse getTotalRevenue(LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.atTime(LocalTime.MAX);

        List<Transaction> transactions = transactionRepository
                .findByTransactionDateBetween(fromDateTime, toDateTime);

        List<TransactionDetail> penaltyDetails = transactionDetailRepository
                .findByBooking_CreatedAtBetween(fromDateTime, toDateTime);

        double totalTransactionAmount = transactions.stream()
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalPenaltyAmount = penaltyDetails.stream()
                .mapToDouble(TransactionDetail::getAppliedAmount)
                .sum();

        double totalRevenue = totalTransactionAmount + totalPenaltyAmount;

        return ReportResponse.builder()
                .stationName("Tất cả trạm")
                .fromDate(from)
                .toDate(to)
                .totalBookingRevenue(totalTransactionAmount)
                .totalPenaltyRevenue(totalPenaltyAmount)
                .totalRevenue(totalRevenue)
                .totalTransactions(transactions.size())
                .build();
    }
}
