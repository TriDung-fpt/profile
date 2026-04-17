package com.fptu.evstation.rental.evrentalsystem.service.util;

import com.fptu.evstation.rental.evrentalsystem.entity.PasswordReset;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.PasswordResetRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final PasswordResetRepository tokenRepo;
    private final UserRepository userRepo;

    // Tạo OTP dạng số 6 chữ số
    private String generateOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    // Tạo OTP và gửi email
    @Transactional
    public String createPasswordResetToken(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        PasswordReset existingToken = tokenRepo.findByUser(user).orElse(null);
        if (existingToken != null) {
            if (existingToken.getExpiryDate().isAfter(LocalDateTime.now())) {
                long secondsRemaining = java.time.Duration.between(LocalDateTime.now(), existingToken.getExpiryDate()).getSeconds();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "OTP đã được gửi, vui lòng thử lại sau " + secondsRemaining + " giây");
            } else {
                tokenRepo.delete(existingToken);
            }
        }

        String otp = generateOtp();
        PasswordReset resetToken = new PasswordReset();
        resetToken.setUser(user);
        resetToken.setOtpCode(otp);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(1));
        tokenRepo.save(resetToken);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Yêu cầu đặt lại mật khẩu");
        message.setText(
                "Xin chào " + user.getFullName() + ",\n\n" +
                        "Bạn vừa yêu cầu đặt lại mật khẩu tài khoản EVolve.\n\n" +
                        "Mã xác thực của bạn là: " + otp + "\n\n" +
                        "Mã có hiệu lực trong 1 phút. Không chia sẻ mã này với bất kỳ ai.\n\n" +
                        "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ EVolve"
        );

        mailSender.send(message);

        return otp;
    }

    @Transactional
    public void resetPasswordWithOtp(String otpCode, String newPassword, String confirmPassword) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP không được để trống");
        }
        if (newPassword == null || newPassword.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu mới và nhập lại mật khẩu không được để trống");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu nhập lại không khớp");
        }

        PasswordReset resetToken = tokenRepo.findByOtpCode(otpCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP không hợp lệ"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            // token hết hạn
            tokenRepo.delete(resetToken); // dọn dẹp luôn
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP đã hết hạn");
        }

        // policy mật khẩu (giữ giống bạn trước đó)
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$";
        if (!Pattern.matches(passwordPattern, newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
        }

        User user = resetToken.getUser();
        user.setPassword(newPassword);
        userRepo.save(user);

        tokenRepo.deleteByUser(user);
    }

    public void sendInvoiceWithAttachment(String toEmail, String customerName, String invoiceCode, File pdfFile) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Thông báo phát hành Hóa đơn điện tử của EVolve");

            String htmlContent =
                    "<p>Kính gửi Quý khách <strong>" + customerName + "</strong>,</p>" +
                            "<p>Hệ thống cho thuê xe điện <strong>EVolve</strong> chân thành cảm ơn Quý khách đã tin tưởng sử dụng dịch vụ của chúng tôi.</p>" +
                            "<p>EVolve trân trọng thông báo: Chúng tôi đã phát hành <strong>Hóa đơn điện tử</strong> mang mã số: <strong>" + invoiceCode + "</strong>.</p>" +
                            "<p>(Chi tiết hóa đơn và tệp PDF được đính kèm trong email này).</p>" +
                            "<p>Trân trọng,<br>Đội ngũ EVolve.</p>";

            helper.setText(htmlContent, true);
            helper.addAttachment(pdfFile.getName(), pdfFile);

            mailSender.send(message);
            log.info("Đã gửi hóa đơn cho khách hàng {}", customerName);

        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email hóa đơn: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi gửi email hóa đơn: " + e.getMessage(), e);
        }
    }



}

