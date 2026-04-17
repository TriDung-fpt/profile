package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.RegisterRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateProfileRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UploadVerificationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.VerifyRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.RoleRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.StationRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StationRepository stationRepository;
    private final Path uploadBaseDir = Paths.get("uploads/verification");

    @Override
    @Transactional
    public User register(RegisterRequest req) {
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã tồn tại");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại đã tồn tại");
        }
        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu và xác nhận mật khẩu không khớp");
        }
        if (!req.isAgreedToTerms()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn phải đồng ý với các điều khoản dịch vụ");
        }

        Role role = roleRepository.findByRoleName("EV_RENTER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy vai trò mặc định cho người dùng"));

        User u = User.builder()
                .password(req.getPassword())
                .email(req.getEmail())
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .verificationStatus(VerificationStatus.PENDING)
                .status(AccountStatus.ACTIVE)
                .role(role)
                .cancellationCount(0)
                .build();
        return userRepository.save(u);
    }

    @Override
    @Transactional
    public User updateUserRole(Long userId, Long newRoleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        Role role = roleRepository.findById(newRoleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role không tồn tại"));

        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserStation(Long userId, Long newStationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User không tồn tại"));

        Station station = stationRepository.findById(newStationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Station không tồn tại"));

        user.setStation(station);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUserProfile(User user, UpdateProfileRequest req) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản bị khóa không thể cập nhật thông tin.");
        }

        boolean isApproved = user.getVerificationStatus() == VerificationStatus.APPROVED;

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email mới đã tồn tại");
            }
            user.setEmail(req.getEmail());
        }
        if (req.getPhone() != null && !req.getPhone().isBlank() && !req.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(req.getPhone())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại mới đã tồn tại");
            }
            user.setPhone(req.getPhone());
        }

        if (!isApproved) {
            if (req.getCccd() != null && !req.getCccd().isBlank() && !req.getCccd().equals(user.getCccd())) {
                if (userRepository.existsByCccd(req.getCccd())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số CCCD này đã được sử dụng");
                }
                user.setCccd(req.getCccd());
            }
            if (req.getGplx() != null && !req.getGplx().isBlank() && !req.getGplx().equals(user.getGplx())) {
                if (userRepository.existsByGplx(req.getGplx())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số GPLX này đã được sử dụng");
                }
                user.setGplx(req.getGplx());
            }
        } else if ((req.getCccd() != null && !req.getCccd().isBlank()) || (req.getGplx() != null && !req.getGplx().isBlank())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thông tin CCCD/GPLX đã được xác thực và không thể tự chỉnh sửa.");
        }

        return userRepository.save(user);
    }

    @Override
    @Transactional
    public String uploadVerificationDocuments(User user, UploadVerificationRequest req) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản bị khóa không thể tải lên giấy tờ.");
        }

        if (user.getVerificationStatus() == VerificationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản đã được xác thực, không cần tải lên lại");
        }

        if (!req.getCccd().equals(user.getCccd()) && userRepository.existsByCccd(req.getCccd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số CCCD này đã được sử dụng");
        }
        if (!req.getGplx().equals(user.getGplx()) && userRepository.existsByGplx(req.getGplx())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số GPLX này đã được sử dụng");
        }

        user.setCccd(req.getCccd());
        user.setGplx(req.getGplx());

        Path userDir = uploadBaseDir.resolve("user_" + user.getUserId());
        try {
            Files.createDirectories(userDir);
        } catch (IOException e) {
            log.error("Không thể tạo thư mục cho người dùng: " + userDir, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi xử lý thư mục");
        }

        if (user.getVerificationStatus() == VerificationStatus.REJECTED) {
            user.setRejectionReason(null);
        }
        user.setVerificationStatus(VerificationStatus.PENDING);

        if (req.getCccdFile1() == null || req.getCccdFile2() == null ||
                req.getGplxFile1() == null || req.getGplxFile2() == null ||
                req.getSelfieFile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng tải lên đầy đủ 5 ảnh: 2 mặt CCCD, 2 mặt GPLX và ảnh selfie");
        }

        validateFile(req.getCccdFile1(), "CCCD mặt trước");
        validateFile(req.getCccdFile2(), "CCCD mặt sau");
        validateFile(req.getGplxFile1(), "GPLX mặt trước");
        validateFile(req.getGplxFile2(), "GPLX mặt sau");
        validateFile(req.getSelfieFile(), "Selfie");

        user.setCccdPath1(saveFile(req.getCccdFile1(), userDir, "cccd_1"));
        user.setCccdPath2(saveFile(req.getCccdFile2(), userDir, "cccd_2"));
        user.setGplxPath1(saveFile(req.getGplxFile1(), userDir, "gplx_1"));
        user.setGplxPath2(saveFile(req.getGplxFile2(), userDir, "gplx_2"));
        user.setSelfiePath(saveFile(req.getSelfieFile(), userDir, "selfie"));

        userRepository.save(user);
        return "Yêu cầu đã gửi. Vui lòng chờ nhân viên xác nhận trong vòng 24 giờ.";
    }

    @Override
    public List<User> getPendingVerifications() {
        return userRepository.findByVerificationStatus(VerificationStatus.PENDING);
    }

    @Override
    @Transactional
    public String processVerification(Long userId, VerifyRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        if (user.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xử lý các yêu cầu đang ở trạng thái PENDING");
        }
        if (!req.isApproved()) {
            if (req.getReason() == null || req.getReason().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải nhập lý do khi từ chối");
            }
            user.setVerificationStatus(VerificationStatus.REJECTED);
            user.setRejectionReason(req.getReason());
            userRepository.save(user);
            return "Đã từ chối yêu cầu xác minh. Lý do: " + req.getReason();
        } else {
            user.setVerificationStatus(VerificationStatus.APPROVED);
            user.setRejectionReason(null);
            userRepository.save(user);
            return "Xác minh người dùng thành công.";
        }
    }

    @Override
    public Map<String, Object> getVerificationStatus(User user) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", user.getVerificationStatus().name());
        if (user.getVerificationStatus() == VerificationStatus.REJECTED && user.getRejectionReason() != null) {
            response.put("reason", user.getRejectionReason());
        }
        return response;
    }

    @Override
    @Transactional
    public User unlockUserAccount(Long userId) {
        User user = getUserById(userId);

        if (user.getStatus() == AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản này không bị khóa.");
        }
        user.setStatus(AccountStatus.ACTIVE);
        user.setCancellationCount(0);

        return userRepository.save(user);
    }

    private void validateFile(MultipartFile file, String fileType) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fileType + " phải là một file ảnh (jpg, png, etc.)");
        }
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kích thước file " + fileType + " không được vượt quá 5MB");
        }
    }

    private String saveFile(MultipartFile file, Path userDir, String baseFileName) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String finalFileName = baseFileName + extension;
            Path filePath = userDir.resolve(finalFileName);
            file.transferTo(filePath);
            return "/uploads/verification/" + userDir.getFileName().toString() + "/" + finalFileName;
        } catch (IOException e) {
            log.error("Lỗi khi lưu file: " + baseFileName, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi lưu file");
        }
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User không tồn tại"));
    }

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}