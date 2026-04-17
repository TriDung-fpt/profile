package com.fptu.evstation.rental.evrentalsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "Users",
        indexes = {
                @Index(columnList = "email"),
                @Index(columnList = "phone")
        })
@Getter // Add these annotations
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"station"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long userId;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(unique = true, length = 100)
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String fullName;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(unique = true, length = 50)
    private String cccd;

    @Column(unique = true, length = 50)
    private String gplx;

    @Column(length = 500)
    private String cccdPath1;
    @Column(length = 500)
    private String cccdPath2;
    @Column(length = 500)
    private String gplxPath1;
    @Column(length = 500)
    private String gplxPath2;
    @Column(length = 500)
    private String selfiePath;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    @Column(columnDefinition = "nvarchar(500)")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer cancellationCount = 0;

    @JsonManagedReference
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleId",nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stationId")
    private Station station;
}