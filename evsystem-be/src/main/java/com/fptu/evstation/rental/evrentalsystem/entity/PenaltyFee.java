package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PenaltyFees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PenaltyFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feeId;

    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String feeName;

    @Column(nullable = false)
    private Double fixedAmount;

    @Column(columnDefinition = "nvarchar(255)")
    private String description;

    @Column(nullable = false)
    private Boolean isAdjustment = false;
}
