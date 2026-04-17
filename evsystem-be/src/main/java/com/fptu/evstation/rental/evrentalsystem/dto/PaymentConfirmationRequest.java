package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmationRequest {

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    private Double amountReceived;
    private String staffNote;
    private String conditionBefore;

    @NotNull(message = "Tình trạng sau khi trả xe không được để trống")
    private String conditionAfter;

    @NotNull(message = "Mức pin khi trả xe không được để trống")
    private Double battery;

    @NotNull(message = "Số km khi trả xe không được để trống")
    private Double mileage;
    private List<MultipartFile> confirmPhotos;
}