package com.fptu.evstation.rental.evrentalsystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Models")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Model {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long modelId;

    @Column(nullable = false, unique = true, columnDefinition = "nvarchar(100)")
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VehicleType vehicleType;

    private Integer seatCount;
    private Double batteryCapacity;
    private Double rangeKm;
    @Column(columnDefinition = "nvarchar(500)")
    private String features;


    @Column(nullable = false)
    private Double pricePerHour;

    @Column(nullable = false)
    private Double initialValue;

    @Column(columnDefinition = "nvarchar(255)")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String imagePaths;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}