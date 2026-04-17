package com.fptu.evstation.rental.evrentalsystem.repository;


import com.fptu.evstation.rental.evrentalsystem.entity.PasswordReset;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByUser(User user);

    // Tìm theo OTP code
    Optional<PasswordReset> findByOtpCode(String otpCode);

    // Xoá tất cả bản ghi reset theo user
    void deleteByUser(User user);
}
