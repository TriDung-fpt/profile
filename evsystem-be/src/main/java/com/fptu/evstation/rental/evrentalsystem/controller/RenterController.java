package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.dto.BookingSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.CancelBookingRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.BookingService;
import com.fptu.evstation.rental.evrentalsystem.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/renter")
@RequiredArgsConstructor
public class RenterController {
    private final AuthService authService;
    private final BookingService bookingService;

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingSummaryResponse>> getMyBookings(
            @RequestHeader("Authorization") String authHeader) {

        User renter = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));

        List<BookingSummaryResponse> myBookings = bookingService.getMyBookings(renter);

        return ResponseEntity.ok(myBookings);
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<?> cancelMyBooking(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) CancelBookingRequest req) {

        User renter = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        Booking booking = bookingService.getBookingById(bookingId);

        if (!booking.getUser().getUserId().equals(renter.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền hủy booking này.");
        }

        String message = bookingService.cancelBookingByRenter(renter, bookingId, req);

        return ResponseEntity.ok(Map.of("message", message));
    }
}