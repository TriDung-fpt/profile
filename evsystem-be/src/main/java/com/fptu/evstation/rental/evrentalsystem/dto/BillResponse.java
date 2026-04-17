package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillResponse {
    private Long bookingId;
    private LocalDateTime dateTime;
    private String userName;

    private Double baseRentalFee;
    private Double totalPenaltyFee;
    private Double totalDiscount;
    private Double downpayPaid;

    private Double paymentDue;

    private Double refundToCustomer;

    private List<FeeItem> feeItems;
    private String qrCodeUrl;
    private String invoicePdfPath;

    @Data
    @Builder
    public static class FeeItem {
        private String feeName;
        private Double amount;
        private String staffNote;
        private String adjustmentNote;
    }
}
