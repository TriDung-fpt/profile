package com.fptu.evstation.rental.evrentalsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Họ và tên không đc để trống")
    private String fullName;

    @NotBlank(message = "Mật khẩu không đc để trống")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$",
            message = "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt"
    )
    private String password;

    @NotBlank(message = "Vui lòng lặp lại mật khẩu")
    private String confirmPassword;

    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Không để trống email")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^(84|0[35789])[0-9]{8}$",
            message = "Phone phải là 10 chữ số và bắt đầu bằng 84 hoặc 03/05/07/08/09"
    )
    private String phone;

    private boolean agreedToTerms = false;
}
