package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.PenaltyFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PenaltyFeeRepository extends JpaRepository<PenaltyFee, Long> {
    Optional<PenaltyFee> findByIsAdjustmentTrue();
    boolean existsByFeeName(String feeName);
    boolean existsByIsAdjustmentTrue();
    Optional<PenaltyFee> findById(Long feeId);
    List<PenaltyFee> findByIsAdjustmentIsFalse();
}
