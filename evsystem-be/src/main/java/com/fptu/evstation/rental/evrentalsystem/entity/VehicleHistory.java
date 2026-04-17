package com.fptu.evstation.rental.evrentalsystem.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Vehicle_History")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private User staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renter_id")
    private User renter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Station station;

    @Enumerated(EnumType.STRING)
    private VehicleActionType actionType;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @Column(columnDefinition = "NVARCHAR(200)")
    private String conditionBefore;

    @Column(columnDefinition = "NVARCHAR(200)")
    private String conditionAfter;

    private Double batteryLevel;

    private Double mileage;

    @Column(name = "photo_paths", columnDefinition = "VARCHAR(MAX)")
    private String photoPaths;

    @CreationTimestamp
    private LocalDateTime actionTime;
}
