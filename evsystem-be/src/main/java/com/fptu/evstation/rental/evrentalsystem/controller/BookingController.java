package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.dto.BookingDetailResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.BookingRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.BookingService;
import com.fptu.evstation.rental.evrentalsystem.service.util.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    private final BookingService bookingService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody BookingRequest req) {
        User renter = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        Map<String, Object> result = bookingService.createBooking(renter, req);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBookingDetails(@PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBookingDetailsById(bookingId));
    }

}
