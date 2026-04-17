package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    public Optional<User> findByEmail(String email);
    public Optional<User> findByPhone(String phone);
    public boolean existsByEmail(String email);
    public boolean existsByPhone(String phone);
    List<User> findByVerificationStatus(VerificationStatus status);
    public boolean existsByCccd(String cccd);
    public boolean existsByGplx(String gplx);
    List<User> findByStation_StationId(Long stationId);
}
