package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.entity.Vehicle;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleActionType;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleHistoryRepository extends JpaRepository<VehicleHistory, Long>, JpaSpecificationExecutor<VehicleHistory> {
    List<VehicleHistory> findByVehicle_VehicleIdOrderByActionTimeDesc(Long vehicleId);
    List<VehicleHistory> findByRenter_UserIdOrderByActionTimeDesc(Long renterId);
    VehicleHistory findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(Vehicle vehicle, User user, VehicleActionType vehicleActionType);
    VehicleHistory findFirstByVehicleOrderByActionTimeDesc(Vehicle vehicle);
}