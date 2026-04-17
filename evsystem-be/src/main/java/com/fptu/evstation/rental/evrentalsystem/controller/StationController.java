package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.dto.ModelResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelSearchRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.ModelWithAvailabilityResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.VehicleAvailabilityResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.VehicleResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {
    private final StationService stationService;
    private final ModelService modelService;
    private final RatingService ratingService;
    private final AuthService authService;
    private final VehicleService vehicleService;


    @GetMapping
    public ResponseEntity<List<Station>> getAllPublicStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<Station> getPublicStationDetail(@PathVariable Long stationId) {
        Station station = stationService.getStationById(stationId);
        if (!StationStatus.ACTIVE.equals(station.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy trạm với ID: " + stationId);
        }
        return ResponseEntity.ok(station);
    }

    @GetMapping("/{stationId}/models")
    public ResponseEntity<List<ModelResponse>> getModelsByStation(
            @PathVariable Long stationId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VehicleType vehicleType) {
        return ResponseEntity.ok(modelService.getModelsByStation(stationId, keyword, vehicleType));
    }

    @GetMapping("/{stationId}/models/{modelId}/vehicles")
    public ResponseEntity<List<VehicleResponse>> getVehiclesByModelAndStation(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long stationId,
            @PathVariable Long modelId) {
        User user = null;
        if (authHeader != null && !authHeader.isBlank()) {
            try {
                user = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
            } catch (Exception e) {
                user = null;
            }
        }

        return ResponseEntity.ok(vehicleService.getVehiclesByModelAndStation(modelId, stationId, user));
    }

    @PostMapping("/rating")
    public ResponseEntity<?> addRating(@RequestHeader("Authorization") String authHeader,
                                       @RequestBody Map<String, Object> request) {
        User user = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản bị khóa không thể đánh giá.");
        }

        Long stationId = ((Number) request.get("stationId")).longValue();
        int stars = (int) request.get("stars");
        String comment = (String) request.get("comment");

        Rating saved = ratingService.saveRating(stationId, stars, comment, user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/rating/{stationId}")
    public ResponseEntity<List<Rating>> getRatingsByStation(@PathVariable Long stationId) {
        return ResponseEntity.ok(ratingService.getRatingsByStation(stationId));
    }

    @GetMapping("/rating/{stationId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long stationId) {
        Double average = ratingService.getAverageRating(stationId);
        return ResponseEntity.ok(average);
    }

    @GetMapping("/{stationId}/models/search")
    public ResponseEntity<List<ModelWithAvailabilityResponse>> searchAvailableModels(
            @PathVariable Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer seatCount,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String order) {

        ModelSearchRequest searchRequest = ModelSearchRequest.builder()
                .stationId(stationId)
                .startTime(startTime)
                .endTime(endTime)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .seatCount(seatCount)
                .vehicleType(vehicleType)
                .sortBy(sortBy)
                .order(order)
                .build();

        List<ModelWithAvailabilityResponse> models = modelService.getAvailableModelsByStation(searchRequest);
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{stationId}/models/{modelId}/available-vehicles")
    public ResponseEntity<List<VehicleAvailabilityResponse>> getAvailableVehicles(
            @PathVariable Long stationId,
            @PathVariable Long modelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<VehicleAvailabilityResponse> vehicles = vehicleService.getAvailableVehiclesByModel(
                modelId, stationId, startTime, endTime);
        return ResponseEntity.ok(vehicles);
    }
}
