package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.StationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateStationRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.StationStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.repository.StationRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StationServiceImpl implements StationService {
    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public Station addStation(StationRequest request) {
        Station station = Station.builder()
                .name(request.getName())
                .address(request.getAddress())
                .openingHours(request.getOpeningHours())
                .description(request.getDescription())
                .hotline(request.getHotline())
                .status(StationStatus.ACTIVE)
                .latitude(0.0)
                .longitude(0.0)
                .build();
        return stationRepository.save(station);
    }

    @Override
    public List<Station> getAllStations() {
        return stationRepository.findByStatus(StationStatus.ACTIVE);
    }

    @Override
    public Station updateStation(Long id, UpdateStationRequest request) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Station not found"));

        if (request.getName() != null) station.setName(request.getName());
        if (request.getAddress() != null) station.setAddress(request.getAddress());
        if (request.getOpeningHours() != null) station.setOpeningHours(request.getOpeningHours());
        if (request.getDescription() != null) station.setDescription(request.getDescription());
        if (request.getHotline() != null) station.setHotline(request.getHotline());

        if (request.getStatus() != null) {
            try {
                StationStatus newStatus = StationStatus.valueOf(request.getStatus().toUpperCase());
                station.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ. Chỉ có ACTIVE, INACTIVE, MAINTENANCE được cho phép.");
            }
        }

        return stationRepository.save(station);
    }

    @Override
    @Transactional
    public void deleteStation(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy trạm với ID " + id));
        long vehicleCount = vehicleRepository.countByStation(station);
        if (vehicleCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể xóa trạm '" + station.getName() + "' vì vẫn còn " + vehicleCount + " xe đang thuộc trạm này."
            );
        }
        stationRepository.delete(station);
    }

    @Override
    public Station getStationById(Long stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy trạm với ID: " + stationId));
    }

    @Override
    @Transactional
    public Map<String, Object> getVehicleStatsByStation(Long stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy trạm với ID: " + stationId));
        List<Object[]> results = vehicleRepository.countVehiclesByStatus(station);

        Map<String, Long> counts = new HashMap<>(Map.of(
                "AVAILABLE", 0L,
                "RENTED", 0L,
                "RESERVED", 0L,
                "UNAVAILABLE", 0L
        ));
        for (Object[] row : results) {
            VehicleStatus status = (VehicleStatus) row[0];
            Long count = (Long) row[1];
            counts.put(status.name(), count);
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        double rentedRate = total > 0 ? (double) counts.get("RENTED") / total * 100 : 0;
        String demandLevel = rentedRate > 60 ? "CAO" : rentedRate > 30 ? "TRUNG BÌNH" : "THẤP";

        Map<String, Object> response = new HashMap<>();
        response.put("stationId", station.getStationId());
        response.put("stationName", station.getName());
        response.put("vehicleCounts", counts);
        response.put("totalVehicles", total);
        response.put("rentedRate", rentedRate);
        response.put("demandLevel", demandLevel);

        return response;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getAllStationReports() {
        List<Object[]> results = vehicleRepository.getVehicleStatsGroupedByStation();

        Map<Long, Map<String, Object>> stationReportMap = new HashMap<>();

        for (Object[] row : results) {
            Long stationId = (Long) row[0];
            String stationName = (String) row[1];
            VehicleStatus status = (VehicleStatus) row[2];
            Long count = (Long) row[3];

            stationReportMap.putIfAbsent(stationId, new HashMap<>(Map.of(
                    "stationId", stationId,
                    "stationName", stationName,
                    "AVAILABLE", 0L,
                    "RENTED", 0L,
                    "RESERVED", 0L,
                    "UNAVAILABLE", 0L
            )));

            stationReportMap.get(stationId).put(status.name(), count);
        }

        for (Map<String, Object> report : stationReportMap.values()) {
            long available = (long) report.get("AVAILABLE");
            long rented = (long) report.get("RENTED");
            long reserved = (long) report.get("RESERVED");
            long unavailable = (long) report.get("UNAVAILABLE");
            long total = available + rented + reserved + unavailable;

            double demandRate = total > 0 ? (rented * 100.0 / total) : 0.0;
            report.put("rentedRate", Math.round(demandRate * 100.0) / 100.0);
            report.put("demandLevel", demandRate > 60 ? "CAO" : demandRate > 30 ? "TRUNG BÌNH" : "THẤP");

        }

        return new ArrayList<>(stationReportMap.values());
    }
}
