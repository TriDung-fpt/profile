package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.*;
import com.fptu.evstation.rental.evrentalsystem.service.ModelService;
import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import com.fptu.evstation.rental.evrentalsystem.service.VehicleService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;
    private final VehicleHistoryRepository historyRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Path damageReportDir = Paths.get(System.getProperty("user.dir"), "uploads", "damage_reports");

    private final StationService stationService;
    private final ModelService modelService;

    @Override
    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new RuntimeException("Biển số xe đã tồn tại!");
        }
        Model model = modelService.getModelById(request.getModelId());
        Station station = stationService.getStationById(request.getStationId());

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate())
                .batteryLevel(request.getBatteryLevel())
                .model(model)
                .station(station)
                .currentMileage(request.getCurrentMileage())
                .status(VehicleStatus.valueOf(request.getStatus()))
                .condition(VehicleCondition.valueOf(request.getCondition()))
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);

        return VehicleResponse.builder()
                .vehicleId(savedVehicle.getVehicleId())
                .licensePlate(savedVehicle.getLicensePlate())
                .batteryLevel(savedVehicle.getBatteryLevel())
                .modelName(model.getModelName())
                .stationName(station.getName())
                .stationId(station.getStationId())
                .currentMileage(savedVehicle.getCurrentMileage())
                .status(savedVehicle.getStatus().name())
                .condition(savedVehicle.getCondition().name())
                .vehicleType(model.getVehicleType())
                .pricePerHour(model.getPricePerHour())
                .seatCount(model.getSeatCount())
                .rangeKm(model.getRangeKm())
                .features(model.getFeatures())
                .description(model.getDescription())
                .imagePaths(getModelImagePaths(model))
                .createdAt(savedVehicle.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public Vehicle updateVehicle(Long id, UpdateVehicleDetailsRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy xe ID " + id));
        if (request.getLicensePlate() != null)
            vehicle.setLicensePlate(request.getLicensePlate());
        if (request.getCurrentMileage() != null)
            vehicle.setCurrentMileage(request.getCurrentMileage());
        if (request.getBatteryLevel() != null)
            vehicle.setBatteryLevel(request.getBatteryLevel());
        if (request.getNewCondition() != null)
            vehicle.setCondition(request.getNewCondition());
        if (request.getStatus() != null)
            vehicle.setStatus(request.getStatus());
        if (request.getModelId() != null) {
            Model model = modelService.getModelById(request.getModelId());
            vehicle.setModel(model);
        }

        if (request.getStationId() != null) {
            Station station = stationService.getStationById(request.getStationId());
            vehicle.setStation(station);
        }

        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy xe ID " + id));
        if (vehicle.getStatus() == VehicleStatus.RENTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể xóa xe đang trong trạng thái 'RENTED'.");
        }
        vehicleRepository.delete(vehicle);
    }

    @Override
    public List<VehicleResponse> getAllVehiclesByStation(Station station) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Vehicle> vehicles = vehicleRepository.findByStationWithDetails(station, sort);

        return vehicles.stream().map(vehicle -> {
            Model model = vehicle.getModel();
            return VehicleResponse.builder()
                    .vehicleId(vehicle.getVehicleId())
                    .licensePlate(vehicle.getLicensePlate())
                    .batteryLevel(vehicle.getBatteryLevel())
                    .modelName(model.getModelName())
                    .stationName(vehicle.getStation().getName())
                    .stationId(vehicle.getStation().getStationId())
                    .currentMileage(vehicle.getCurrentMileage())
                    .status(vehicle.getStatus().name())
                    .condition(vehicle.getCondition().name())
                    .vehicleType(model.getVehicleType())
                    .pricePerHour(model.getPricePerHour())
                    .seatCount(model.getSeatCount())
                    .rangeKm(model.getRangeKm())
                    .features(model.getFeatures())
                    .description(model.getDescription())
                    .imagePaths(getModelImagePaths(model))
                    .build();
        }).toList();
    }

    @Override
    public List<VehicleResponse> getVehiclesByModelAndStation(Long modelId, Long stationId, User requestingUser){
        Station station = stationService.getStationById(stationId);
        Model model = modelService.getModelById(modelId);

        List<VehicleStatus> excluded = List.of(VehicleStatus.UNAVAILABLE);
        List<Vehicle> vehicles = vehicleRepository.findByStationAndModelAndStatusNotIn(station, model, excluded);

        final Map<Long, BookingStatus> userActiveBookingMap;

        if (requestingUser != null) {
            List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.RENTING);
            List<Booking> userActiveBookings = bookingRepository.findByUserAndStatusIn(requestingUser, activeStatuses);

            userActiveBookingMap = userActiveBookings.stream()
                    .filter(b -> b.getVehicle() != null)
                    .collect(Collectors.toMap(
                            b -> b.getVehicle().getVehicleId(),
                            Booking::getStatus,
                            (BookingStatus existing, BookingStatus replacement) -> existing
                    ));
        } else {
            userActiveBookingMap = new HashMap<>();
        }

        return vehicles.stream().map(v -> convertToResponse(v, userActiveBookingMap)).toList();
    }

    @Override
    public Map<String, Object> checkVehicleSchedule(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        Vehicle vehicle = getVehicleById(vehicleId);

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            return Map.of("isAvailable", false, "message", "Xe này hiện không còn khả dụng.");
        }

        List<BookingStatus> excludedStatuses = List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED);
        long conflicts = bookingRepository.countOverlappingBookingsForVehicle(
                vehicle, startTime, endTime, excludedStatuses
        );

        if (conflicts > 0) {
            return Map.of("isAvailable", false, "message", "Xe đã có lịch đặt trong khung giờ này.");
        }

        return Map.of("isAvailable", true, "message", "Xe khả dụng trong khung giờ này.");
    }

    @Override
    public List<VehicleResponse> getAllVehicles(Long modelId, Long stationId, VehicleType vehicleType, String sortBy, String order) {
        String sortField = "createdAt".equalsIgnoreCase(sortBy) ? "createdAt" : "model.pricePerHour";
        Sort sort = Sort.by(Sort.Direction.fromString((order == null || order.isBlank()) ? "DESC" : order), sortField);

        Specification<Vehicle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (modelId != null) {
                predicates.add(cb.equal(root.get("model").get("modelId"), modelId));
            }
            if (stationId != null) {
                predicates.add(cb.equal(root.get("station").get("stationId"), stationId));
            }
            if (vehicleType != null) {
                predicates.add(cb.equal(root.get("model").get("vehicleType"), vehicleType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Vehicle> vehicles = vehicleRepository.findAll(spec, sort);

        return vehicles.stream().map(vehicle -> {
            Model model = vehicle.getModel();
            return VehicleResponse.builder()
                    .vehicleId(vehicle.getVehicleId())
                    .licensePlate(vehicle.getLicensePlate())
                    .batteryLevel(vehicle.getBatteryLevel())
                    .modelName(model.getModelName())
                    .stationName(vehicle.getStation().getName())
                    .stationId(vehicle.getStation().getStationId())
                    .currentMileage(vehicle.getCurrentMileage())
                    .status(vehicle.getStatus() != null ? vehicle.getStatus().name() : null)
                    .condition(vehicle.getCondition() != null ? vehicle.getCondition().name() : null)
                    .vehicleType(model.getVehicleType())
                    .pricePerHour(model.getPricePerHour())
                    .seatCount(model.getSeatCount())
                    .rangeKm(model.getRangeKm())
                    .features(model.getFeatures())
                    .description(model.getDescription())
                    .imagePaths(getModelImagePaths(model))
                    .build();
        }).toList();
    }

    @Override
    public Vehicle getVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe với ID: " + vehicleId));
    }

    @Override
    public VehicleResponse getVehicleDetailsById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy xe với ID: " + id));

        Model model = vehicle.getModel();
        Station station = vehicle.getStation();
        List<String> imagePathsList = getModelImagePaths(model);

        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .licensePlate(vehicle.getLicensePlate())
                .status(vehicle.getStatus().name())
                .condition(vehicle.getCondition().name())
                .batteryLevel(vehicle.getBatteryLevel())
                .currentMileage(vehicle.getCurrentMileage())
                .modelName(model != null ? model.getModelName() : null)
                .stationId(station != null ? station.getStationId() : null)
                .stationName(station != null ? station.getName() : null)
                .vehicleType(model != null ? model.getVehicleType() : null)
                .pricePerHour(model != null ? model.getPricePerHour() : null)
                .seatCount(model != null ? model.getSeatCount() : null)
                .rangeKm(model != null ? model.getRangeKm() : null)
                .features(model != null ? model.getFeatures() : null)
                .description(model != null ? model.getDescription() : null)
                .imagePaths(imagePathsList)
                .createdAt(vehicle.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public Vehicle saveVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public Vehicle updateVehicleDetails(User staff, Long vehicleId, UpdateVehicleDetailsRequest request) {
        Vehicle vehicle = getVehicleById(vehicleId);

        if (staff.getStation() == null || !vehicle.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền cập nhật thông tin xe ở trạm khác.");
        }

        if (request.getBatteryLevel() != null) {
            vehicle.setBatteryLevel(request.getBatteryLevel());
            log.info("Cập nhật mức pin cho xe {} thành {}%", vehicle.getLicensePlate(), request.getBatteryLevel());
        }

        if (request.getNewCondition() != null) {
            vehicle.setCondition(request.getNewCondition());
            log.info("Cập nhật tình trạng xe {} thành {}", vehicle.getLicensePlate(), request.getNewCondition());
        }

        return vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public Vehicle reportMajorDamage(User staff, Long vehicleId, ReportDamageRequest request) {
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        Vehicle vehicle = getVehicleById(vehicleId);

        if (staff.getStation() == null || !vehicle.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền báo cáo hư hỏng cho xe ở trạm khác.");
        }

        vehicle.setCondition(VehicleCondition.MAINTENANCE_REQUIRED);
        vehicle.setStatus(VehicleStatus.UNAVAILABLE);

        List<String> photoPaths = new ArrayList<>();
        if (request.getPhotos() != null && !request.getPhotos().isEmpty()) {
            for (int i = 0; i < request.getPhotos().size(); i++) {
                MultipartFile photo = request.getPhotos().get(i);
                String savedPath = saveDamageReportPhoto(photo, vehicleId, i + 1);
                photoPaths.add(savedPath);
            }
        }

        VehicleHistory lastHistory = historyRepository.findFirstByVehicleOrderByActionTimeDesc(vehicle);
        String lastKnownCondition = "Không rõ (Xe mới)";

        if (lastHistory != null) {
            if (lastHistory.getConditionAfter() != null && !lastHistory.getConditionAfter().isBlank()) {
                lastKnownCondition = lastHistory.getConditionAfter();
            }
            else if (lastHistory.getConditionBefore() != null) {
                lastKnownCondition = lastHistory.getConditionBefore();
            }
        }

        try {
            String photoPathsJson = objectMapper.writeValueAsString(photoPaths);
            vehicle.setDamageReportPhotos(photoPathsJson);

            recordVehicleAction(
                    vehicleId,
                    staff.getUserId(),
                    null,
                    staff.getStation().getStationId(),
                    VehicleActionType.MAINTENANCE,
                    request.getDescription(),
                    lastKnownCondition,
                    null,
                    (double) vehicle.getBatteryLevel(),
                    vehicle.getCurrentMileage(),
                    photoPathsJson
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu ảnh hoặc ghi lịch sử báo cáo hư hỏng.", e);
        }

        log.warn("XE HƯ HỎNG NẶNG: Xe {} đã được đưa vào trạng thái bảo trì. Lý do: {}", vehicle.getLicensePlate(), request.getDescription());
        return vehicleRepository.save(vehicle);
    }

    protected String saveDamageReportPhoto(MultipartFile file, Long vehicleId, int index) {
        try {
            Path vehicleDir = damageReportDir.resolve("vehicle_" + vehicleId);
            Files.createDirectories(vehicleDir);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String fileName = String.format("damage-%d%s", index, extension);
            Path filePath = vehicleDir.resolve(fileName);
            file.transferTo(filePath);

            String relativePath = "/uploads/damage_reports/vehicle_" + vehicleId + "/" + fileName;
            log.info("Đã lưu ảnh báo cáo hư hỏng tại: {}", relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("Lỗi khi lưu ảnh báo cáo hư hỏng cho xe ID: " + vehicleId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi lưu ảnh báo cáo.");
        }
    }

    @Override
    public VehicleHistory recordVehicleAction(Long vehicleId, Long staffId, Long renterId,Long stationId, VehicleActionType type, String note, String conditionBefore, String conditionAfter, Double battery, Double mileage, String photoPathsJson) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy xe"));
        User staff = staffId != null ? userRepository.findById(staffId).orElse(null) : null;
        User renter = renterId != null ? userRepository.findById(renterId).orElse(null) : null;
        Station station = stationId != null ? stationService.getStationById(stationId) : null;

        VehicleHistory history = VehicleHistory.builder()
                .vehicle(vehicle)
                .staff(staff)
                .renter(renter)
                .station(station)
                .actionType(type)
                .note(note)
                .conditionBefore(conditionBefore)
                .conditionAfter(conditionAfter)
                .batteryLevel(battery)
                .mileage(mileage)
                .photoPaths(photoPathsJson)
                .build();

        return historyRepository.save(history);
    }
    @Override
    public List<VehicleHistoryResponse> getVehicleHistory(Long stationId, LocalDate from, LocalDate to, VehicleType vehicleType, String licensePlate) {
        Sort sort = Sort.by(Sort.Direction.DESC, "actionTime");

        Specification<VehicleHistory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (stationId != null) {
                predicates.add(cb.equal(root.get("station").get("stationId"), stationId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("actionTime"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("actionTime"), to.atTime(LocalTime.MAX)));
            }
            if (vehicleType != null) {
                Join<VehicleHistory, Vehicle> vehicleJoin = root.join("vehicle", JoinType.INNER);
                Join<Vehicle, Model> modelJoin = vehicleJoin.join("model", JoinType.INNER);
                predicates.add(cb.equal(modelJoin.get("vehicleType"), vehicleType));
            }
            if (licensePlate != null && !licensePlate.isBlank()) {
                Join<VehicleHistory, Vehicle> vehicleJoin = root.getJoins().stream()
                        .filter(j -> j.getAttribute().getName().equals("vehicle"))
                        .map(j -> (Join<VehicleHistory, Vehicle>) j)
                        .findFirst()
                        .orElseGet(() -> root.join("vehicle", JoinType.INNER));

                predicates.add(cb.like(cb.lower(vehicleJoin.get("licensePlate")), "%" + licensePlate.toLowerCase() + "%"));
            }

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("vehicle", JoinType.LEFT).fetch("model", JoinType.LEFT);
                root.fetch("staff", JoinType.LEFT);
                root.fetch("renter", JoinType.LEFT);
                root.fetch("station", JoinType.LEFT);
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<VehicleHistory> histories = historyRepository.findAll(spec, sort);

        return histories.stream()
                .map(h -> VehicleHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .vehicleType(h.getVehicle() != null ? h.getVehicle().getModel().getVehicleType() : null)
                        .licensePlate(h.getVehicle() != null ? h.getVehicle().getLicensePlate() : null)
                        .staffName(h.getStaff() != null ? h.getStaff().getFullName() : null)
                        .renterName(h.getRenter() != null ? h.getRenter().getFullName() : null)
                        .stationName(h.getStation() != null ? h.getStation().getName() : null)
                        .actionType(h.getActionType().name())
                        .note(h.getNote())
                        .conditionBefore(h.getConditionBefore())
                        .conditionAfter(h.getConditionAfter())
                        .batteryLevel(h.getBatteryLevel())
                        .mileage(h.getMileage())
                        .photoPath(h.getPhotoPaths())
                        .actionTime(h.getActionTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleHistoryResponse> getHistoryByVehicle(Long vehicleId) {
        return historyRepository.findByVehicle_VehicleIdOrderByActionTimeDesc(vehicleId)
                .stream()
                .map(h -> VehicleHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .vehicleType(h.getVehicle().getModel().getVehicleType())
                        .licensePlate(h.getVehicle().getLicensePlate())
                        .staffName(h.getStaff() != null ? h.getStaff().getFullName() : null)
                        .renterName(h.getRenter() != null ? h.getRenter().getFullName() : null)
                        .stationName(h.getStation() != null ? h.getStation().getName() : null)
                        .actionType(h.getActionType().name())
                        .note(h.getNote())
                        .batteryLevel(h.getBatteryLevel())
                        .mileage(h.getMileage())
                        .photoPath(h.getPhotoPaths())
                        .actionTime(h.getActionTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleHistoryResponse> getHistoryByRenter(Long renterId) {
        return historyRepository.findByRenter_UserIdOrderByActionTimeDesc(renterId)
                .stream()
                .map(h -> VehicleHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .vehicleType(h.getVehicle().getModel().getVehicleType())
                        .licensePlate(h.getVehicle().getLicensePlate())
                        .staffName(h.getStaff() != null ? h.getStaff().getFullName() : null)
                        .renterName(h.getRenter() != null ? h.getRenter().getFullName() : null)
                        .stationName(h.getStation() != null ? h.getStation().getName() : null)
                        .actionType(h.getActionType().name())
                        .note(h.getNote())
                        .batteryLevel(h.getBatteryLevel())
                        .mileage(h.getMileage())
                        .photoPath(h.getPhotoPaths())
                        .actionTime(h.getActionTime())
                        .build())
                .collect(Collectors.toList());
    }

    private List<String> getModelImagePaths(Model model) {
        if (model == null || model.getImagePaths() == null || model.getImagePaths().isBlank()) {
            return new ArrayList<>();
        }
        return List.of(model.getImagePaths().split(","));
    }

    private VehicleResponse convertToResponse(Vehicle vehicle, Map<Long, BookingStatus> userActiveBookingMap) {
        Model model = vehicle.getModel();
        List<String> paths = getModelImagePaths(model);

        boolean isReservedByMe = false;
        boolean isRentedByMe = false;

        BookingStatus userBookingStatus = userActiveBookingMap.get(vehicle.getVehicleId());

        if (userBookingStatus != null) {
            if (userBookingStatus == BookingStatus.CONFIRMED && vehicle.getStatus() == VehicleStatus.RESERVED) {
                isReservedByMe = true;
            }
            else if (userBookingStatus == BookingStatus.RENTING && vehicle.getStatus() == VehicleStatus.RENTED) {
                isRentedByMe = true;
            }
        }

        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .licensePlate(vehicle.getLicensePlate())
                .batteryLevel(vehicle.getBatteryLevel())
                .modelName(model.getModelName())
                .stationName(vehicle.getStation().getName())
                .stationId(vehicle.getStation().getStationId())
                .currentMileage(vehicle.getCurrentMileage())
                .status(vehicle.getStatus().name())
                .condition(vehicle.getCondition().name())
                .vehicleType(model.getVehicleType())
                .pricePerHour(model.getPricePerHour())
                .seatCount(model.getSeatCount())
                .rangeKm(model.getRangeKm())
                .features(model.getFeatures())
                .description(model.getDescription())
                .imagePaths(paths)
                .createdAt(vehicle.getCreatedAt())
                .isReservedByMe(isReservedByMe)
                .isRentedByMe(isRentedByMe)
                .build();
    }

    @Override
    public List<VehicleAvailabilityResponse> getAvailableVehiclesByModel(Long modelId, Long stationId,
                                                                          LocalDateTime startTime, LocalDateTime endTime) {
        Station station = stationService.getStationById(stationId);
        Model model = modelService.getModelById(modelId);

        List<VehicleStatus> excludedStatuses = List.of(VehicleStatus.UNAVAILABLE);
        List<Vehicle> vehicles = vehicleRepository.findByStationAndModelAndStatusNotIn(station, model, excludedStatuses);

        List<BookingStatus> excludedBookingStatuses = List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED);

        return vehicles.stream()
                .map(vehicle -> {
                    boolean isAvailable = true;
                    String message = "Xe khả dụng trong khung giờ này";

                    if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                        isAvailable = false;
                        message = "Xe hiện không còn khả dụng";
                    } else if (startTime != null && endTime != null) {
                        long conflicts = bookingRepository.countOverlappingBookingsForVehicle(
                                vehicle, startTime, endTime, excludedBookingStatuses);

                        if (conflicts > 0) {
                            isAvailable = false;
                            message = "Xe đã có lịch đặt trong khung giờ này";
                        }
                    }

                    return VehicleAvailabilityResponse.builder()
                            .vehicleId(vehicle.getVehicleId())
                            .licensePlate(vehicle.getLicensePlate())
                            .batteryLevel(vehicle.getBatteryLevel())
                            .currentMileage(vehicle.getCurrentMileage())
                            .status(vehicle.getStatus())
                            .condition(vehicle.getCondition())
                            .isAvailable(isAvailable)
                            .availabilityNote(message)
                            .modelId(model.getModelId())
                            .modelName(model.getModelName())
                            .stationId(station.getStationId())
                            .stationName(station.getName())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
