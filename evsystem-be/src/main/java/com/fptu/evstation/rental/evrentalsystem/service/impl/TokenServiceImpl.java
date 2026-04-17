package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.entity.AuthToken;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.AuthTokenRepository;
import com.fptu.evstation.rental.evrentalsystem.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final AuthTokenRepository authTokenRepository;


    @Override
    public AuthToken createToken(User user) {
        List<AuthToken> existingTokens = authTokenRepository.findByUser(user);

        if (!existingTokens.isEmpty()) {
            authTokenRepository.deleteAll(existingTokens);
        }

        AuthToken t = AuthToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plus(2, ChronoUnit.HOURS))
                .build();
        return authTokenRepository.save(t);
    }

    @Override
    public void deleteToken(String token) {
        AuthToken existingToken = authTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token không tồn tại hoặc đã hết hạn"));
        authTokenRepository.delete(existingToken);
    }

    @Override
    public User validateTokenAndGetUser(String token) {
        var tokenOpt = authTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
        }
        var t = tokenOpt.get();
        if (t.getExpiresAt() == null || t.getExpiresAt().isBefore(LocalDateTime.now())) {
            authTokenRepository.delete(t);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn");
        }

        return t.getUser();
    }
}
