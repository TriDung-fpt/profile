package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.AuthResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.LoginRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

public interface AuthService {
    AuthResponse login(LoginRequest req);
    void logout(String token);
    AuthResponse loginWithGoogle(String idToken);
    User validateTokenAndGetUser(String token);
    String getTokenFromHeader(String authHeader);
}
