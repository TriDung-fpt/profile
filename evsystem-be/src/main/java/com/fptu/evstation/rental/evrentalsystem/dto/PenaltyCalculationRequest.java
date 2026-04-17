package com.fptu.evstation.rental.evrentalsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyCalculationRequest {

    private List<SelectedFee> selectedFees;

    private CustomFee customFee;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedFee {
        private Long feeId;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomFee {
        private String feeName;
        private String description;
        private Double amount;
        private List<MultipartFile> photoFiles;
    }
}