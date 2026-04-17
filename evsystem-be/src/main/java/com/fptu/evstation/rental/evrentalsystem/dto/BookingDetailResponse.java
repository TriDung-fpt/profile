package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BookingDetailResponse {
    private Long bookingId;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime startDate;
    private LocalDateTime updatedAt;
    private LocalDateTime endDate;


    private String renterName;
    private String renterPhone;
    private String renterEmail;

    private String modelName;
    private Double initialValue;

    private String vehicleLicensePlate;
    private Double pricePerHour;

    private String stationName;
    private String stationAddress;

    private Boolean reservationDepositPaid;
    private Double rentalDeposit;
    private Double finalFee;

    private List<String> checkInPhotoPaths;
    private String invoicePdfPath;
    private String contractPdfPath;
}
