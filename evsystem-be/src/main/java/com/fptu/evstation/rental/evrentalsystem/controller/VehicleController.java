package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import com.fptu.evstation.rental.evrentalsystem.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final StationService stationService;
    private final VehicleService vehicleService;

    @GetMapping("/station/{stationId}/stats")
    public ResponseEntity<?> getVehicleStatsByStation(@PathVariable Long stationId) {
        Map<String, Object> stats = stationService.getVehicleStatsByStation(stationId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{vehicleId}/availability")
    public ResponseEntity<?> checkVehicleScheduleAvailability(
            @PathVariable Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        Map<String, Object> availability = vehicleService.checkVehicleSchedule(vehicleId, startTime, endTime);

        if (!(Boolean) availability.get("isAvailable")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(availability);
        }
        return ResponseEntity.ok(availability);
    }
}
