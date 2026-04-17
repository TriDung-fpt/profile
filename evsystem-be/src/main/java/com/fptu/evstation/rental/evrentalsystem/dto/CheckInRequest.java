package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckInRequest {

    @NotNull(message = "Phương thức thanh toán cọc không được để trống")
    private PaymentMethod depositPaymentMethod;

    @NotNull(message = "Tình trạng xe (trước khi giao) không được để trống")
    private String conditionBefore;

    @NotNull(message = "Mức pin (trước khi giao) không được để trống")
    private Double battery;

    @NotNull(message = "Số km (trước khi giao) không được để trống")
    private Double mileage;

    private List<MultipartFile> checkInPhotos;
}
