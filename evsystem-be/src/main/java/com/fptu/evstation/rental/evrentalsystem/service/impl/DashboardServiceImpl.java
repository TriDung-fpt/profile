package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.DashboardSummaryDto;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import com.fptu.evstation.rental.evrentalsystem.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummaryForStaff(User staff) {

        Station staffStation = staff.getStation();
        if (staffStation == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên này chưa được gán cho trạm nào.");
        }

        long totalVehicles = vehicleRepository.countByStation(staffStation);

        Map<VehicleStatus, Long> statusSummary = vehicleRepository.countVehiclesByStatus(staffStation).stream()
                .collect(Collectors.toMap(
                        result -> (VehicleStatus) result[0],
                        result -> (Long) result[1]
                ));

        return DashboardSummaryDto.builder()
                .stationName(staffStation.getName())
                .totalVehicles(totalVehicles)
                .statusSummary(statusSummary)
                .build();
    }
}
