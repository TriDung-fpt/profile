package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.AuthResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.LoginRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.RoleRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.service.AuthService;
import com.fptu.evstation.rental.evrentalsystem.service.TokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final TokenService tokenService;

    @Value("${google.clientId}")
    private String googleClientId;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getIdentifier())
                .or(() -> userRepository.findByPhone(req.getIdentifier()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!req.getPassword().equals(user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mật khẩu không chính xác");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa hoặc chưa được kích hoạt.");
        }

        AuthToken t = tokenService.createToken(user);
        return new AuthResponse(t.getToken(), t.getExpiresAt(), user.getFullName());
    }

    @Transactional
    public void logout(String token) {
        tokenService.deleteToken(token);
    }

    @Override
    public AuthResponse loginWithGoogle(String idToken) {
        String verifyUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        ResponseEntity<Map> googleResp;
        try {
            googleResp = restTemplate.getForEntity(verifyUrl, Map.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google id_token không hợp lệ");
        }

        if (!googleResp.getStatusCode().is2xxSuccessful() || googleResp.getBody() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google id_token không hợp lệ");
        }

        Map<String, Object> payload = googleResp.getBody();

        Object audObj = payload.get("aud");
        if (audObj == null || !googleClientId.equals(String.valueOf(audObj))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid audience in id_token");
        }

        String email = (String) payload.get("email");
        String fullName = (String) payload.getOrDefault("name", "Unknown");

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản Google không có email");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role role = roleRepository.findByRoleName("EV_RENTER")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy role EV_RENTER"));
            String randomPassword = String.format("%06d", new Random().nextInt(1_000_000));
            User newUser = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .password(randomPassword)
                    .role(role)
                    .verificationStatus(VerificationStatus.PENDING)
                    .status(AccountStatus.ACTIVE)
                    .cancellationCount(0)
                    .build();
            return userRepository.save(newUser);
        });

        AuthToken authToken = tokenService.createToken(user);
        return new AuthResponse(authToken.getToken(), authToken.getExpiresAt(), user.getFullName());
    }

    @Override
    public User validateTokenAndGetUser(String token) {
        return tokenService.validateTokenAndGetUser(token);
    }

    @Override
    public String getTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Yêu cầu thiếu token xác thực");
        }
        return authHeader.substring(7);
    }
}
