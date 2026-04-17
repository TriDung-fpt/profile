package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByStatusAndStartDateBefore(BookingStatus status, LocalDateTime expiryTime);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoffTime);

    List<Booking> findByUserAndStatusIn(User user, List<BookingStatus> statuses);

    List<Booking> findAllByInvoicePdfPathIsNotNullAndStation(Station station, Sort sort);

    List<Booking> findAllByStartDateBetween(LocalDateTime from, LocalDateTime to);

    List<Booking> findAllByStationAndStartDateBetween(Station station, LocalDateTime from, LocalDateTime to);

    long countByUserAndStatusIn(User user, List<BookingStatus> activeStatuses);

    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.vehicle = :vehicle " +
            "AND b.status NOT IN :excludedStatuses " +
            "AND (b.startDate < :endTime AND b.endDate > :startTime)")
    long countOverlappingBookingsForVehicle(
            @Param("vehicle") Vehicle vehicle,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludedStatuses") List<BookingStatus> excludedStatuses);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.vehicle v " +
            "LEFT JOIN FETCH v.model " +
            "WHERE b.user = :renter")
    List<Booking> findByUserWithDetails(@Param("renter") User renter, Sort sort);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.vehicle v " +
            "LEFT JOIN FETCH v.model " +
            "WHERE b.station = :station")
    List<Booking> findAllByStationWithDetails(@Param("station") Station station, Sort sort);
}
