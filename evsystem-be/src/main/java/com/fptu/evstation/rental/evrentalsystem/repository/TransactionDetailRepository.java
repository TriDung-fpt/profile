package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.TransactionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Integer> {
    void deleteByBooking(Booking booking);
    List<TransactionDetail> findByBooking(Booking booking);

    List<TransactionDetail> findByBooking_Vehicle_Station_StationIdAndBooking_CreatedAtBetween(
            Long stationId,
            LocalDateTime from,
            LocalDateTime to
    );

    List<TransactionDetail> findByBooking_CreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("SELECT SUM(td.appliedAmount) FROM TransactionDetail td WHERE td.booking = :booking AND td.appliedAmount < 0")
    Double findTotalDiscountByBooking(@Param("booking") Booking booking);
}
