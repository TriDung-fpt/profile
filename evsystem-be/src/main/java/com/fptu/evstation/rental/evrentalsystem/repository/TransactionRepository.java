package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBooking(Booking booking);
    List<Transaction> findByTransactionDateBetween(LocalDateTime from, LocalDateTime to);
    List<Transaction> findByBooking_Vehicle_Station_StationIdAndTransactionDateBetween(
            Long stationId,
            LocalDateTime from,
            LocalDateTime to
    );


}
