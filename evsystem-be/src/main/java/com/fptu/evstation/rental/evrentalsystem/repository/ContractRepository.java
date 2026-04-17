package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.Contract;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer> {
    List<Contract> findByBooking_Station(Station station, Sort sort);
    Optional<Contract> findByBooking(Booking booking);
}
