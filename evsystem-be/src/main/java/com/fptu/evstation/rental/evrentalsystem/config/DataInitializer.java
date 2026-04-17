package com.fptu.evstation.rental.evrentalsystem.config;

import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepo;
    private final PenaltyFeeRepository feeRepo;
    private final ModelRepository modelRepository;
    private final StationRepository stationRepository;
    private final VehicleRepository vehicleRepository;

    private final Path modelBaseDir = Paths.get(System.getProperty("user.dir"), "uploads", "models_img");

    @Override
    public void run(String... args) throws Exception {
        log.info("Bắt đầu khởi tạo dữ liệu cơ sở...");
        initRoles();
        initPenaltyFees();
        initStations();
        initModels();
        initVehicles();
        log.info("Khởi tạo dữ liệu hoàn tất.");
    }

    private void initRoles() {
        List<String> roles = List.of("EV_RENTER", "STATION_STAFF", "ADMIN");
        for(String r : roles) {
            roleRepo.findByRoleName(r).orElseGet(() ->
                    roleRepo.save(Role.builder().roleName(r).build())
            );
        }
    }

    private void initPenaltyFees() {
        Map<String, Double> fees = Map.of(
                "Phí vệ sinh xe", 200000.0,
                "Phí hư hỏng nhẹ (trầy xước, móp nhỏ)", 200000.0,
                "Phí xử lý vi phạm giao thông", 2000000.0,
                "Phí trả xe quá hạn (mỗi giờ)", 300000.0,
                "Phí cứu hộ / kéo xe", 750000.0
        );
        fees.forEach((name, amount) -> {
            if (!feeRepo.existsByFeeName(name)) {
                PenaltyFee fee = PenaltyFee.builder()
                        .feeName(name)
                        .fixedAmount(amount)
                        .description("Phí cố định được tạo tự động bởi hệ thống.")
                        .isAdjustment(false)
                        .build();
                feeRepo.save(fee);
            }
        });

        String adjustmentFeeName = "Phí tùy chỉnh (Adjustment)";
        if (!feeRepo.existsByFeeName(adjustmentFeeName)) {
            PenaltyFee adjustmentFee = PenaltyFee.builder()
                    .feeName(adjustmentFeeName)
                    .fixedAmount(0.0)
                    .description("Loại phí dùng để đại diện cho các khoản phí tùy chỉnh.")
                    .isAdjustment(true)
                    .build();
            feeRepo.save(adjustmentFee);
        }
    }

    private void initStations() {
        record StationData(String name, String address, String description, Double latitude, Double longitude, String openingHours, String hotline, String status) {}
        List<StationData> stationsToCreate = List.of(
                new StationData("Trạm Evolve - Quận 1", "Số 2 Lê Lợi, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh", "Trạm đặt tại trung tâm Quận 1, gần chợ Bến Thành, thuận tiện cho khách du lịch và nhân viên văn phòng.", 10.774034, 106.698958, "07:00 - 22:00", "1900123456", "ACTIVE"),
                new StationData("Trạm Evolve - Quận 3", "Số 135 Điện Biên Phủ, Phường 15, Quận 3, TP. Hồ Chí Minh", "Trạm nằm trên trục Điện Biên Phủ, gần nhiều văn phòng và khu dịch vụ, phù hợp cho thuê ngắn hạn.", 10.778912, 106.693421, "07:00 - 22:00", "1900123457", "ACTIVE"),
                new StationData("Trạm Evolve - Bình Thạnh", "Số 88 Nguyễn Hữu Cảnh, Phường 22, Quận Bình Thạnh, TP. Hồ Chí Minh", "Trạm gần cầu Sài Gòn, thuận tiện kết nối về Quận 1, Quận 2/Thủ Đức và khu vực lân cận.", 10.792546, 106.708005, "07:00 - 22:00", "1900123458", "ACTIVE")
        );
        for (StationData data : stationsToCreate) {
            if (!stationRepository.existsByName(data.name())) {
                Station station = Station.builder()
                        .name(data.name())
                        .address(data.address())
                        .description(data.description())
                        .latitude(data.latitude())
                        .longitude(data.longitude())
                        .openingHours(data.openingHours())
                        .hotline(data.hotline())
                        .status(StationStatus.valueOf(data.status()))
                        .build();
                stationRepository.save(station);
            }
        }
    }

    private void initModels() {
        record ModelData(String name, Double initial, Double price, String desc, String features, String type, int seats, Double battery, Double range) {}
        List<ModelData> modelsToCreate = List.of(
                new ModelData("VinFast VF 3", 299_000_000.0, 25_000.0, "Micro-SUV đô thị nhỏ gọn, thiết kế trẻ trung, chi phí vận hành thấp.", "Pin LFP nhỏ gọn, kết nối thông minh, sạc nhanh.", "CAR", 4, 18.6, 210.0),
                new ModelData("VinFast VF 5", 529_000_000.0, 30_000.0, "CUV nhỏ tiện dụng cho gia đình nhỏ, cân bằng giữa tầm hoạt động và tiện nghi.", "ADAS cơ bản, trợ lý ảo, sạc nhanh 10–70% ~33 phút.", "CAR", 5, 37.23, 326.0),
                new ModelData("VinFast VF 6", 719_000_000.0, 50_000.0, "Compact SUV hiện đại, phù hợp cho gia đình và dịch vụ thuê.", "ADAS, kết nối thông minh, sạc nhanh 25–30 phút.", "CAR", 5, 59.6, 399.0),
                new ModelData("VinFast VF 7", 799_000_000.0, 70_000.0, "SUV cỡ vừa, rộng rãi, nhiều trang bị tiện nghi và an toàn.", "ADAS cấp cao, FWD/AWD, pin 70–75 kWh.", "CAR", 5, 75.3, 431.0),
                new ModelData("VinFast VF 8", 1_099_000_000.0, 75_000.0, "SUV cỡ D thuần điện, công nghệ cao, phù hợp dịch vụ thuê cao cấp.", "Pin dung lượng lớn, sạc nhanh, nội thất tiện nghi.", "CAR", 5, 87.7, 471.0),
                new ModelData("VinFast VF 9", 1_499_000_000.0, 105_000.0, "Full-size SUV 7 chỗ phiên bản tiêu chuẩn, hướng đến thuê dùng dịch vụ.", "Cấu hình 7 chỗ, da tổng hợp, an toàn cơ bản.", "CAR", 7, 92.0, 438.0),
                new ModelData("VinFast VF e34", 750_000_000.0, 39_000.0, "SUV cỡ C, nhỏ gọn, hướng đến người dùng đô thị.", "Động cơ 110 kW, mô-men xoắn 242 Nm, thuê hoặc mua pin linh hoạt.", "CAR", 5, 42.0, 318.0),
                new ModelData("Dat Bike Weaver 200", 45_000_000.0, 10_000.0, "Xe máy điện hiệu năng cao, quãng đường dài, phù hợp thuê ngắn/dài hạn.", "Pin dung lượng lớn, tăng tốc tốt, thiết kế hiện đại.", "MOTORBIKE", 2, 5.5, 200.0),
                new ModelData("Dat Bike Quantum S", 42_000_000.0, 9_000.0, "Xe máy điện thể thao, tăng tốc mạnh, pin tầm xa.", "Động cơ mạnh, pin bền, thiết kế thể thao.", "MOTORBIKE", 2, 4.8, 270.0),
                new ModelData("PEGA NewTech", 23_000_000.0, 5_000.0, "Xe máy điện phổ thông, ổn định, phù hợp thuê ngắn hạn và dịch vụ chia sẻ.", "Động cơ 1.5–2 kW, pin 72V–20Ah, quãng đường ~100 km/lần sạc.", "MOTORBIKE", 2, 1.44, 100.0)
        );
        for (ModelData data : modelsToCreate) {
            if (!modelRepository.existsByModelName(data.name())) {
                String imagePaths = getDynamicImagePaths(data.name());
                if (imagePaths == null) {
                    log.warn("Không tìm thấy thư mục hoặc ảnh cho model: {}. Sẽ lưu mà không có ảnh.", data.name());
                }
                Model model = Model.builder()
                        .modelName(data.name())
                        .initialValue(data.initial())
                        .pricePerHour(data.price())
                        .description(data.desc())
                        .features(data.features())
                        .vehicleType(VehicleType.valueOf(data.type().toUpperCase()))
                        .seatCount(data.seats())
                        .batteryCapacity(data.battery())
                        .rangeKm(data.range())
                        .imagePaths(imagePaths)
                        .build();
                modelRepository.save(model);
            }
        }
    }

    private void initVehicles() {
        Map<Long, Model> modelMap = modelRepository.findAll().stream()
                .collect(Collectors.toMap(Model::getModelId, model -> model));
        Map<Long, Station> stationMap = stationRepository.findAll().stream()
                .collect(Collectors.toMap(Station::getStationId, station -> station));

        record VehicleData(String licensePlate, Double battery, Long modelId, Double mileage, String status, String condition, Long stationId) {}
        List<VehicleData> vehiclesToCreate = List.of(
                // --- XE CHO TRẠM 1 ---
                new VehicleData("51A-123.45", 87.3, 1L, 1500.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("59C-987.21", 42.0, 1L, 4200.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("51B-234.56", 65.5, 2L, 3200.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("50F-010.99", 12.4, 2L, 8800.0, "AVAILABLE", "MINOR_DAMAGE", 1L),
                new VehicleData("51C-555.66", 99.9, 3L, 2100.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("54A-301.77", 34.2, 3L, 7600.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("51D-408.12", 58.6, 4L, 5050.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("59H-222.33", 7.8, 4L, 12300.0, "AVAILABLE", "MINOR_DAMAGE", 1L),
                new VehicleData("51E-777.88", 46.4, 5L, 3400.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("52B-444.55", 81.0, 5L, 2700.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("51F-303.21", 30.7, 6L, 9900.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("53C-111.22", 55.5, 6L, 4300.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("51G-909.09", 76.8, 7L, 6100.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("58A-202.02", 4.5, 7L, 20400.0, "AVAILABLE", "MINOR_DAMAGE", 1L),
                new VehicleData("51H-120.12", 68.2, 8L, 1800.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("59K-830.83", 21.9, 8L, 7800.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("51J-505.50", 92.1, 9L, 1600.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("56B-404.40", 39.3, 9L, 10300.0, "AVAILABLE", "GOOD", 1L),
                new VehicleData("51K-707.07", 50.0, 10L, 1500.0, "AVAILABLE", "EXCELLENT", 1L),
                new VehicleData("57C-313.13", 15.6, 10L, 9200.0, "AVAILABLE", "MINOR_DAMAGE", 1L),

                // --- XE CHO TRẠM 2 ---
                new VehicleData("36A-123.45", 36.7, 1L, 1800.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("79B-987.65", 72.4, 1L, 2400.0, "AVAILABLE", "EXCELLENT", 2L),
                new VehicleData("30C-001.11", 9.3, 2L, 5600.0, "AVAILABLE", "MINOR_DAMAGE", 2L),
                new VehicleData("29D-222.33", 54.0, 2L, 3100.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("43A-445.56", 83.1, 3L, 1950.0, "AVAILABLE", "EXCELLENT", 2L),
                new VehicleData("47B-667.78", 27.5, 3L, 7200.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("66C-808.08", 45.9, 4L, 5300.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("71D-909.90", 12.0, 4L, 15800.0, "AVAILABLE", "MINOR_DAMAGE", 2L),
                new VehicleData("88A-314.15", 99.9, 5L, 1200.0, "AVAILABLE", "EXCELLENT", 2L),
                new VehicleData("68B-271.82", 67.3, 5L, 4100.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("36C-333.44", 58.2, 6L, 8900.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("79D-555.66", 3.7, 6L, 24100.0, "AVAILABLE", "MINOR_DAMAGE", 2L),
                new VehicleData("30A-777.88", 71.0, 7L, 3000.0, "AVAILABLE", "EXCELLENT", 2L),
                new VehicleData("29B-888.99", 49.6, 7L, 7200.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("43C-101.01", 26.4, 8L, 2700.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("47D-202.02", 88.8, 8L, 1400.0, "AVAILABLE", "EXCELLENT", 2L),
                new VehicleData("66A-303.03", 15.2, 9L, 10400.0, "AVAILABLE", "MINOR_DAMAGE", 2L),
                new VehicleData("71B-404.04", 62.6, 9L, 3600.0, "AVAILABLE", "GOOD", 2L),
                new VehicleData("88C-505.05", 40.0, 10L, 1500.0, "AVAILABLE", "EXCELLENT", 2L),
                new VehicleData("68D-606.06", 5.9, 10L, 9500.0, "AVAILABLE", "MINOR_DAMAGE", 2L),

                // --- XE CHO TRẠM 3 ---
                new VehicleData("36A-246.80", 36.7, 1L, 1500.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("79B-753.21", 79.4, 1L, 4200.0, "AVAILABLE", "EXCELLENT", 3L),
                new VehicleData("30C-110.22", 30.5, 2L, 3200.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("29D-224.56", 29.9, 2L, 5600.0, "AVAILABLE", "MINOR_DAMAGE", 3L),
                new VehicleData("43A-337.11", 43.2, 3L, 1950.0, "AVAILABLE", "EXCELLENT", 3L),
                new VehicleData("47B-668.77", 47.8, 3L, 7200.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("66C-801.09", 66.1, 4L, 5300.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("71D-919.19", 71.0, 4L, 15800.0, "AVAILABLE", "MINOR_DAMAGE", 3L),
                new VehicleData("88A-330.14", 88.6, 5L, 1200.0, "AVAILABLE", "EXCELLENT", 3L),
                new VehicleData("68B-276.82", 68.3, 5L, 4100.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("36C-334.44", 36.4, 6L, 8900.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("79D-559.66", 79.7, 6L, 24100.0, "AVAILABLE", "MINOR_DAMAGE", 3L),
                new VehicleData("30A-777.18", 30.1, 7L, 3000.0, "AVAILABLE", "EXCELLENT", 3L),
                new VehicleData("29B-883.99", 29.4, 7L, 7200.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("43C-107.01", 43.9, 8L, 2700.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("47D-204.62", 47.2, 8L, 1400.0, "AVAILABLE", "EXCELLENT", 3L),
                new VehicleData("66A-306.03", 66.5, 9L, 10400.0, "AVAILABLE", "MINOR_DAMAGE", 3L),
                new VehicleData("71B-409.04", 71.6, 9L, 3600.0, "AVAILABLE", "GOOD", 3L),
                new VehicleData("88C-507.05", 88.0, 10L, 1500.0, "AVAILABLE", "EXCELLENT", 3L),
                new VehicleData("68D-609.06", 68.9, 10L, 9500.0, "AVAILABLE", "MINOR_DAMAGE", 3L)
        );
        for (VehicleData data : vehiclesToCreate) {
            if (!vehicleRepository.existsByLicensePlate(data.licensePlate())) {
                Model model = modelMap.get(data.modelId());
                Station station = stationMap.get(data.stationId());
                if (model == null) {
                    log.warn("Không tìm thấy Model ID {} cho xe {}. Bỏ qua.", data.modelId(), data.licensePlate());
                    continue;
                }
                if (station == null) {
                    log.warn("Không tìm thấy Station ID {} cho xe {}. Bỏ qua.", data.stationId(), data.licensePlate());
                    continue;
                }
                Vehicle vehicle = Vehicle.builder()
                        .licensePlate(data.licensePlate())
                        .batteryLevel(data.battery().intValue())
                        .model(model)
                        .station(station)
                        .currentMileage(data.mileage())
                        .status(VehicleStatus.valueOf(data.status()))
                        .condition(VehicleCondition.valueOf(data.condition()))
                        .build();
                vehicleRepository.save(vehicle);
            }
        }
    }

    private String getDynamicImagePaths(String modelName) {
        try {
            String modelDirName = modelName.replaceAll("[^a-zA-Z0-9-]", "_");
            Path modelDirPath = modelBaseDir.resolve(modelDirName);

            if (!Files.exists(modelDirPath) || !Files.isDirectory(modelDirPath)) {
                log.warn("Thư mục ảnh không tồn tại: {}", modelDirPath);
                return null;
            }

            List<String> relativePaths;
            try (var pathStream = Files.list(modelDirPath)) {
                relativePaths = pathStream
                        .filter(Files::isRegularFile)
                        .sorted()
                        .map(filePath -> "/uploads/models_img/" + modelDirName + "/" + filePath.getFileName().toString())
                        .collect(Collectors.toList());
            }

            if (relativePaths.isEmpty()) {
                log.warn("Không tìm thấy file ảnh nào trong: {}", modelDirPath);
                return null;
            }

            return String.join(",", relativePaths);

        } catch (IOException e) {
            log.error("Lỗi khi đọc thư mục ảnh cho {}: {}", modelName, e.getMessage());
            return null;
        }
    }
}
