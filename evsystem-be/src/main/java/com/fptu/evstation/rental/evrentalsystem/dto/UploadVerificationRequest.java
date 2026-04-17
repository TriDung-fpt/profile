package com.fptu.evstation.rental.evrentalsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadVerificationRequest {
    @NotBlank(message = "Số CCCD không được để trống")
    private String cccd;

    @NotBlank(message = "Số GPLX không được để trống")
    private String gplx;

    private MultipartFile cccdFile1;
    private MultipartFile cccdFile2;
    private MultipartFile gplxFile1;
    private MultipartFile gplxFile2;
    private MultipartFile selfieFile;
}
