package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;

    @Column(nullable = false, length = 20)
    private String licensePlate;

    @Column(nullable = false)
    private Integer batteryLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modelId", nullable = false)
    private Model model;

    @Enumerated(EnumType.STRING)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    // Liên kết với Station
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stationId", nullable = false)
    private Station station;

    @Column(nullable = false)
    private Double currentMileage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCondition condition = VehicleCondition.GOOD;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String DamageReportPhotos;
}
