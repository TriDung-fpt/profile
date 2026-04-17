package com.fptu.evstation.rental.evrentalsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String fullName;

    @Email(message = "Email không hợp lệ")
    private String email;

    @Pattern(
            regexp = "^(84|0[35789])[0-9]{8}$",
            message = "Phone phải là 10 chữ số và bắt đầu bằng 84 hoặc 03/05/07/08/09"
    )
    private String phone;

    private String cccd;
    private String gplx;
}
