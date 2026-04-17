package com.fptu.evstation.rental.evrentalsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelBookingRequest {

    @Size(max = 100, message = "Tên ngân hàng quá dài")
    private String bankName;

    @Size(max = 20, message = "Số tài khoản quá dài")
    private String accountNumber;

    @Size(max = 100, message = "Tên chủ tài khoản quá dài")
    private String accountName;
}
