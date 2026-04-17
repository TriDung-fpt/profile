package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TransactionDetails")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    // Liên kết Booking mà phí này áp dụng
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", nullable = false)
    private Booking booking;

    // Liên kết loại phí (từ bảng PenaltyFee)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feeId", nullable = false)
    private PenaltyFee penaltyFee;

    @Column(nullable = false)
    private Double appliedAmount;

    @Column(columnDefinition = "nvarchar(500)")
    private String staffNote;

    @Column(columnDefinition = "nvarchar(500)")
    private String adjustmentNote;

    @Column(columnDefinition = "TEXT")
    private String photoPaths;
}