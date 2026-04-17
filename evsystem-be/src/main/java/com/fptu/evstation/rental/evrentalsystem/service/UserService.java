package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.RegisterRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateProfileRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UploadVerificationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.VerifyRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

import java.util.List;
import java.util.Map;

public interface UserService {
    User register(RegisterRequest req);
    User updateUserRole(Long userId, Long newRoleId);
    User unlockUserAccount(Long userId);
    User updateUserStation(Long userId, Long newStationId);
    User updateUserProfile(User user, UpdateProfileRequest req);
    String uploadVerificationDocuments(User user, UploadVerificationRequest req);
    List<User> getPendingVerifications();
    String processVerification(Long userId, VerifyRequest req);
    Map<String, Object> getVerificationStatus(User user);
    List<User> getAllUsers();
    User getUserById(Long id);
    User saveUser(User user);
}