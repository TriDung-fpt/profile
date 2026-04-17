package com.fptu.evstation.rental.evrentalsystem.service.scheduling;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.Vehicle;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
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
public class PendingBookingCleanupService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private static final int PAYMENT_TIMEOUT_MINUTES = 30;

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void cleanupPendingBookings() {
        log.info("Bắt đầu Job dọn dẹp booking PENDING quá hạn...");

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoffTime);

        if (expiredBookings.isEmpty()) {
            log.info("Không tìm thấy booking PENDING nào quá hạn.");
            return;
        }

        log.warn("Tìm thấy {} booking PENDING quá hạn. Bắt đầu hủy...", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            Vehicle vehicle = booking.getVehicle();
            if (vehicle != null && vehicle.getStatus() == VehicleStatus.RESERVED) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
                log.info("Booking ID {} đã bị hủy (hết hạn thanh toán). Xe ID {} đã quay lại AVAILABLE.", booking.getBookingId(), vehicle.getVehicleId());
            } else {
                log.warn("Booking ID {} đã bị hủy (hết hạn thanh toán). Không tìm thấy xe hoặc xe không ở trạng thái RESERVED.", booking.getBookingId());
            }
        }
        log.info("Job dọn dẹp booking PENDING đã hoàn tất.");
    }
}
