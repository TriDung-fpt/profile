package com.fptu.evstation.rental.evrentalsystem.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Table(name = "Roles", uniqueConstraints = @UniqueConstraint(columnNames = "roleName"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column
    private Long roleId;
    @Column(nullable = false, unique = true, length = 50)
    private String roleName;

    @JsonBackReference
    @OneToMany(mappedBy = "role", fetch =  FetchType.LAZY)
    @ToString.Exclude
    private List<User> users = new ArrayList<>();
}