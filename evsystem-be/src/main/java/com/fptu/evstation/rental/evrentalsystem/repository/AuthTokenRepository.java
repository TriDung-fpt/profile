package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.AuthToken;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AuthTokenRepository extends JpaRepository<AuthToken,Integer> {
    public Optional<AuthToken> findByToken(String token);

    List<AuthToken> findByUser(User user);

    @Transactional
    public void deleteByToken(AuthToken token);

    @Transactional
    @Modifying
    @Query("DELETE FROM AuthToken t WHERE t.expiresAt < :now")
    void deleteExpiredBefore(@Param("now") LocalDateTime now);
}
