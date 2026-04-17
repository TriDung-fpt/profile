package com.fptu.evstation.rental.evrentalsystem.service.scheduling;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
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
class ReservationCleanupServiceTest {

    @Mock
    private BookingRepository bookingRepo;

    @InjectMocks
    private ReservationCleanupService reservationCleanupService;

    @Test
    void testCleanupExpiredReservations_NoExpiredBookings() {
        // Given
        when(bookingRepo.findByStatusAndStartDateBefore(eq(BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        reservationCleanupService.cleanupExpiredReservations();

        // Then
        verify(bookingRepo, never()).save(any(Booking.class));
    }

    @Test
    void testCleanupExpiredReservations_WithExpiredBookings() {
        // Given
        Booking expiredBooking = new Booking();
        expiredBooking.setBookingId(1L);
        expiredBooking.setStatus(BookingStatus.CONFIRMED);
        expiredBooking.setStartDate(LocalDateTime.now().minusHours(1));

        List<Booking> expiredBookings = Collections.singletonList(expiredBooking);

        when(bookingRepo.findByStatusAndStartDateBefore(eq(BookingStatus.CONFIRMED), any(LocalDateTime.class)))
                .thenReturn(expiredBookings);

        // When
        reservationCleanupService.cleanupExpiredReservations();

        // Then
        verify(bookingRepo, times(1)).save(expiredBooking);
        assert expiredBooking.getStatus() == BookingStatus.CANCELLED;
    }
}
