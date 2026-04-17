package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.Contract;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface BookingService {
    Map<String, Object> createBooking(User renter, BookingRequest req);
    Booking getBookingById(Long bookingId);
    BookingDetailResponse getBookingDetailsById(Long bookingId);
    List<BookingSummaryResponse> getMyBookings(User renter);
    List<BookingSummaryResponse> getAllBookingsByStation(User staff, String keyword, String status, String date);
    Map<String, Object> initiateCheckIn(Long bookingId, User staff);
    Contract processCheckIn(Long bookingId, CheckInRequest req, User staff);
    Map<String, Object> getPeakHourStatistics(Long stationId, LocalDate fromDate, LocalDate toDate);
    String cancelBookingByRenter(User renter, Long bookingId, CancelBookingRequest req);
    List<BookingSummaryResponse> getPendingRefundsByStation(User staff);
    void confirmRefund(User staff, Long bookingId);
    void cancelBookingByStaff(Long bookingId, User staff);
}

