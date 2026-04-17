package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Contracts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contractId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingId", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, length = 500)
    private String contractPdfPath;

    @Column(nullable = false)
    private LocalDateTime signedDate;

    // Khác
    @Column(columnDefinition = "nvarchar(255)")
    private String termsSnapshot;

}
