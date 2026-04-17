package com.fptu.evstation.rental.evrentalsystem.service.scheduling;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.Vehicle;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingBookingCleanupServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private PendingBookingCleanupService pendingBookingCleanupService;

    @Test
    void testCleanupPendingBookings_NoExpiredBookings() {
        // Given
        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        pendingBookingCleanupService.cleanupPendingBookings();

        // Then
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testCleanupPendingBookings_WithExpiredBookingAndVehicle() {
        // Given
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(1L);
        vehicle.setStatus(VehicleStatus.RESERVED);

        Booking expiredBooking = new Booking();
        expiredBooking.setBookingId(1L);
        expiredBooking.setStatus(BookingStatus.PENDING);
        expiredBooking.setCreatedAt(LocalDateTime.now().minusMinutes(31));
        expiredBooking.setVehicle(vehicle);

        List<Booking> expiredBookings = Collections.singletonList(expiredBooking);

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        pendingBookingCleanupService.cleanupPendingBookings();

        // Then
        verify(bookingRepository, times(1)).save(expiredBooking);
        assert expiredBooking.getStatus() == BookingStatus.CANCELLED;

        verify(vehicleRepository, times(1)).save(vehicle);
        assert vehicle.getStatus() == VehicleStatus.AVAILABLE;
    }

    @Test
    void testCleanupPendingBookings_WithExpiredBookingAndNoVehicle() {
        // Given
        Booking expiredBooking = new Booking();
        expiredBooking.setBookingId(1L);
        expiredBooking.setStatus(BookingStatus.PENDING);
        expiredBooking.setCreatedAt(LocalDateTime.now().minusMinutes(31));
        expiredBooking.setVehicle(null);

        List<Booking> expiredBookings = Collections.singletonList(expiredBooking);

        when(bookingRepository.findByStatusAndCreatedAtBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        pendingBookingCleanupService.cleanupPendingBookings();

        // Then
        verify(bookingRepository, times(1)).save(expiredBooking);
        assert expiredBooking.getStatus() == BookingStatus.CANCELLED;

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }
}
