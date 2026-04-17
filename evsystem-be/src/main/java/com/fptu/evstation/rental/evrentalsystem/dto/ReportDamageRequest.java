package com.fptu.evstation.rental.evrentalsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDamageRequest {
    @NotBlank(message = "Mô tả hư hỏng không được để trống")
    private String description;

    private List<MultipartFile> photos;
}
