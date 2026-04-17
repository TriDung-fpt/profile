package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.entity.Vehicle;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStationController {
    private final StationService stationService;
    private final ModelService modelService;
    private final VehicleService vehicleService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final BookingService bookingService;

    // --- 1. Quản lý Trạm (Stations) ---
    @PostMapping("/stations")
    public ResponseEntity<Station> createStation(@RequestBody StationRequest request) {
        return ResponseEntity.ok(stationService.addStation(request));
    }

    @GetMapping("/stations")
    public ResponseEntity<List<Station>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @PutMapping("/stations/{id}")
    public ResponseEntity<Station> updateStation(@PathVariable Long id, @RequestBody UpdateStationRequest request) {
        return ResponseEntity.ok(stationService.updateStation(id, request));
    }

    @DeleteMapping("/stations/{id}")
    public ResponseEntity<Map<String, String>> deleteStation(@PathVariable Long id) {
        try {
            stationService.deleteStation(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Cửa hàng với ID " + id + " đã được xóa thành công!"
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // --- 2. Quản lý Xe (Vehicles) ---
    @PostMapping("/vehicles")
    public ResponseEntity<VehicleResponse> createVehicle(@RequestBody CreateVehicleRequest request) {
        return ResponseEntity.ok(vehicleService.createVehicle(request));
    }

    @GetMapping("/vehicles")
    public ResponseEntity<List<VehicleResponse>> getAllVehicles(
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {
        return ResponseEntity.ok(vehicleService.getAllVehicles(modelId, stationId, vehicleType, sortBy, order));
    }

    @GetMapping("/vehicles/{id}")
    public ResponseEntity<?> getVehicleDetailsById(@PathVariable Long id) {
        try {
            VehicleResponse response = vehicleService.getVehicleDetailsById(id);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
    @PutMapping("/vehicles/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody UpdateVehicleDetailsRequest  request) {
        try {
            Vehicle updated = vehicleService.updateVehicle(id, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Xe đã được cập nhật thành công!",
                    "vehicleId", updated.getVehicleId()
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Xe với ID " + id + " đã được xóa thành công!"
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // --- 3. Quản lý Mẫu xe (Models) ---
    @GetMapping("/models")
    public ResponseEntity<List<ModelResponse>> getAllModels(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(modelService.getAllModels(keyword));
    }

    @PostMapping("/models")
    public ResponseEntity<Model> createModel(
            @ModelAttribute CreateModelRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return ResponseEntity.ok(modelService.createModel(request, images));
    }

    @PutMapping("/models/{id}")
    public ResponseEntity<?> updateModel(
            @PathVariable Long id,
            @ModelAttribute UpdateModelRequest request,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {
        try {
            Model updated = modelService.updateModel(id, request, newImages);
            return ResponseEntity.ok(Map.of(
                    "message", " Cập nhật model thành công!",
                    "model", updated
            ));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/models/{id}")
    public ResponseEntity<?> getModelDetailsById(@PathVariable Long id) {
        try {
            ModelResponse response = modelService.getModelDetailsById(id);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @DeleteMapping("/models/{id}")
    public ResponseEntity<?> deleteModel(@PathVariable Long id) {
        try {
            modelService.deleteModel(id);
            return ResponseEntity.ok(Map.of("message", " Model với ID " + id + " đã được xóa thành công!"));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // --- 4. Quản lý Người dùng (Users) ---
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam AccountStatus status) {

        User user = userService.getUserById(userId);
        user.setStatus(status);
        if (status == AccountStatus.ACTIVE) {
            user.setCancellationCount(0);
        }
        userService.saveUser(user);

        return ResponseEntity.ok(Map.of(
                "message", "Trạng thái người dùng đã được cập nhật!",
                "userId", userId,
                "newStatus", status.name()
        ));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateUserRole(
            @PathVariable Long id,
            @RequestBody RoleUpdateRequest request) {
        User updatedUser = userService.updateUserRole(id, request.getRoleId());
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/staff/station/{stationId}")
    public ResponseEntity<List<User>> getStaffByStation(@PathVariable Long stationId) {
        List<User> staffList = userRepository.findByStation_StationId(stationId);
        return ResponseEntity.ok(staffList);
    }

    @PutMapping("/{userId}/station/{stationId}")
    public ResponseEntity<User> updateUserStation(
            @PathVariable Long userId,
            @PathVariable Long stationId) {
        User updatedUser = userService.updateUserStation(userId, stationId);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/revenue")
    public ResponseEntity<ReportResponse> getRevenueReport(
            @RequestParam(required = false) Long stationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        ReportResponse report;
        if (stationId != null) {
            report = reportService.getRevenueByStation(stationId, from, to);
        }
        else {
            report = reportService.getTotalRevenue(from, to);
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/stations/report")
    public ResponseEntity<?> getAllStationReports() {
        List<Map<String, Object>> reports = stationService.getAllStationReports();
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/vehicle-history")
    public ResponseEntity<List<VehicleHistoryResponse>> getAllVehicleHistory(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) VehicleType vehicleType,
            @RequestParam(required = false) String licensePlate
    ) {
        List<VehicleHistoryResponse> historyList = vehicleService.getVehicleHistory(
                stationId, from, to, vehicleType, licensePlate
        );
        return ResponseEntity.ok(historyList);
    }

    @GetMapping("/vehicle-history/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleHistoryResponse>> getHistoryByVehicle(
            @PathVariable Long vehicleId
    ) {
        List<VehicleHistoryResponse> historyList = vehicleService.getHistoryByVehicle(vehicleId);
        return ResponseEntity.ok(historyList);
    }

    @GetMapping("/vehicle-history/renter/{renterId}")
    public ResponseEntity<List<VehicleHistoryResponse>> getHistoryByRenter(
            @PathVariable Long renterId
    ) {
        List<VehicleHistoryResponse> historyList = vehicleService.getHistoryByRenter(renterId);
        return ResponseEntity.ok(historyList);
    }
    @GetMapping("/statistics/peak-hour")
    public ResponseEntity<Map<String, Object>> getPeakHourStats(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(bookingService.getPeakHourStatistics(stationId, fromDate, toDate));
    }

}
