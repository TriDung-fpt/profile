package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.entity.AuthToken;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

public interface TokenService {
    AuthToken createToken(User user);
    void deleteToken(String token);
    User validateTokenAndGetUser(String token);
}
