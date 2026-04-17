package com.fptu.evstation.rental.evrentalsystem.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {
    @NotNull(message = "Vehicle ID không được để trống")
    private Long vehicleId;

    @NotNull(message = "Ngày và giờ nhận xe không được để trống")
    @Future(message = "Thời gian nhận xe phải ở tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "Ngày và giờ trả xe không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    private boolean agreedToTerms = false;
}