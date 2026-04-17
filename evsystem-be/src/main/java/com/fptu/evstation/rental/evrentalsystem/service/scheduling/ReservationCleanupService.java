package com.fptu.evstation.rental.evrentalsystem.service.scheduling;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.service.VehicleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupService {
    private final BookingRepository bookingRepo;

    /**
     * Job chạy định kỳ để hủy các Booking đã quá hạn nhận xe (24 giờ sau CONFIRMED)
     */
    @Scheduled(cron = "0 0 * * * ?") // Chạy vào đầu mỗi giờ
    @Transactional
    public void cleanupExpiredReservations() {
        log.info("Bắt đầu Job kiểm tra Booking quá hạn...");

        LocalDateTime now = LocalDateTime.now();

        List<Booking> noShowBookings = bookingRepo.findByStatusAndStartDateBefore(BookingStatus.CONFIRMED, now);

        for (Booking booking : noShowBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepo.save(booking);
            log.warn("Booking ID {} bị hủy tự động do khách không đến nhận xe (No-show).", booking.getBookingId());
        }
    }
}