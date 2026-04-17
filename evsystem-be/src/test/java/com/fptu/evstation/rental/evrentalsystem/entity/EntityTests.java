package com.fptu.evstation.rental.evrentalsystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntityTests {

    // --- User Tests ---

    @Test
    void testUserBuilder() {
        Role role = Role.builder().roleId(1L).build();
        Station station = Station.builder().stationId(1L).build();

        User user = User.builder()
                .userId(1L)
                .email("test@example.com")
                .fullName("Test User")
                .phone("0900000000")
                .password("password123")
                .status(AccountStatus.ACTIVE)
                .verificationStatus(VerificationStatus.PENDING)
                .cancellationCount(0)
                .cccd("123456")
                .gplx("789012")
                .cccdPath1("/path/1")
                .cccdPath2("/path/2")
                .gplxPath1("/path/3")
                .gplxPath2("/path/4")
                .selfiePath("/path/5")
                .rejectionReason("Reason")
                .role(role)
                .station(station)
                .build();

        assertEquals(1L, user.getUserId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getFullName());
        assertEquals("0900000000", user.getPhone());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertEquals(VerificationStatus.PENDING, user.getVerificationStatus());
        assertEquals(0, user.getCancellationCount());
        assertEquals("123456", user.getCccd());
        assertEquals("789012", user.getGplx());
        assertEquals("/path/1", user.getCccdPath1());
        assertEquals("/path/2", user.getCccdPath2());
        assertEquals("/path/3", user.getGplxPath1());
        assertEquals("/path/4", user.getGplxPath2());
        assertEquals("/path/5", user.getSelfiePath());
        assertEquals("Reason", user.getRejectionReason());
        assertEquals(role, user.getRole());
        assertEquals(station, user.getStation());
    }

    @Test
    void testUserNoArgsConstructorAndSetters() {
        User user = new User();
        Role role = new Role();
        Station station = new Station();

        user.setUserId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPhone("0900000000");
        user.setPassword("password123");
        user.setStatus(AccountStatus.INACTIVE);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        user.setCancellationCount(1);
        user.setCccd("123456");
        user.setGplx("789012");
        user.setCccdPath1("/path/1");
        user.setCccdPath2("/path/2");
        user.setGplxPath1("/path/3");
        user.setGplxPath2("/path/4");
        user.setSelfiePath("/path/5");
        user.setRejectionReason("Reason");
        user.setRole(role);
        user.setStation(station);

        assertEquals(1L, user.getUserId());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getFullName());
        assertEquals("0900000000", user.getPhone());
        assertEquals("password123", user.getPassword());
        assertEquals(AccountStatus.INACTIVE, user.getStatus());
        assertEquals(VerificationStatus.APPROVED, user.getVerificationStatus());
        assertEquals(1, user.getCancellationCount());
        assertEquals("123456", user.getCccd());
        assertEquals("789012", user.getGplx());
        assertEquals("/path/1", user.getCccdPath1());
        assertEquals("/path/2", user.getCccdPath2());
        assertEquals("/path/3", user.getGplxPath1());
        assertEquals("/path/4", user.getGplxPath2());
        assertEquals("/path/5", user.getSelfiePath());
        assertEquals("Reason", user.getRejectionReason());
        assertEquals(role, user.getRole());
        assertEquals(station, user.getStation());
    }

    @Test
    void testUserAllArgsConstructor() {
        Role role = new Role();
        Station station = new Station();
        User user = new User(1L, "pass", "email", "full", "phone", "cccd", "gplx",
                "p1", "p2", "p3", "p4", "p5", VerificationStatus.REJECTED,
                "reason", AccountStatus.INACTIVE, 2, role, station);

        assertEquals(1L, user.getUserId());
        assertEquals("email", user.getEmail());
        assertEquals(AccountStatus.INACTIVE, user.getStatus());
        assertEquals(2, user.getCancellationCount());
    }

    @Test
    void testUserDefaultValues() {
        User user = new User();
        assertEquals(VerificationStatus.PENDING, user.getVerificationStatus());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertEquals(0, user.getCancellationCount());
    }

    @Test
    void testUserToString() {
        User user = User.builder().fullName("Test User").build();
        // Kiểm tra @ToString(exclude = {"station"})
        assertTrue(user.toString().contains("fullName=Test User"));
        assertFalse(user.toString().contains("station="));
    }

    // --- Station Tests ---

    @Test
    void testStationBuilder() {
        Station station = Station.builder()
                .stationId(1L)
                .name("Trạm Evolve - Quận 1")
                .address("123 Nguyễn Huệ")
                .status(StationStatus.ACTIVE)
                .description("Desc")
                .latitude(10.0)
                .longitude(106.0)
                .openingHours("24/7")
                .rating(4.5)
                .hotline("1900")
                .build();

        assertEquals(1L, station.getStationId());
        assertEquals("Trạm Evolve - Quận 1", station.getName());
        assertEquals("123 Nguyễn Huệ", station.getAddress());
        assertEquals(StationStatus.ACTIVE, station.getStatus());
        assertEquals("Desc", station.getDescription());
        assertEquals(10.0, station.getLatitude());
        assertEquals(106.0, station.getLongitude());
        assertEquals("24/7", station.getOpeningHours());
        assertEquals(4.5, station.getRating());
        assertEquals("1900", station.getHotline());
    }

    @Test
    void testStationNoArgsConstructorAndSetters() {
        Station station = new Station();
        LocalDateTime time = LocalDateTime.now();

        station.setStationId(1L);
        station.setName("Test Station");
        station.setAddress("123 Test Street");
        station.setStatus(StationStatus.INACTIVE);
        station.setDescription("Desc");
        station.setLatitude(10.0);
        station.setLongitude(106.0);
        station.setOpeningHours("24/7");
        station.setRating(4.5);
        station.setHotline("1900");
        station.setCreatedAt(time);
        station.setUpdatedAt(time);

        assertEquals(1L, station.getStationId());
        assertEquals("Test Station", station.getName());
        assertEquals("123 Test Street", station.getAddress());
        assertEquals(StationStatus.INACTIVE, station.getStatus());
        assertEquals("Desc", station.getDescription());
        assertEquals(10.0, station.getLatitude());
        assertEquals(106.0, station.getLongitude());
        assertEquals("24/7", station.getOpeningHours());
        assertEquals(4.5, station.getRating());
        assertEquals("1900", station.getHotline());
        assertEquals(time, station.getCreatedAt());
        assertEquals(time, station.getUpdatedAt());
    }

    @Test
    void testStationAllArgsConstructor() {
        LocalDateTime time = LocalDateTime.now();
        Station station = new Station(1L, "Name", "Addr", "Desc", 10.0, 106.0,
                "24/7", 4.5, "1900", StationStatus.MAINTENANCE, time, time);
        assertEquals(1L, station.getStationId());
        assertEquals("Name", station.getName());
        assertEquals(StationStatus.MAINTENANCE, station.getStatus());
    }

    @Test
    void testStationDefaultValues() {
        Station station = new Station();
        assertEquals(StationStatus.ACTIVE, station.getStatus());
    }

    @Test
    void testStationPrePersistAndPreUpdate() throws InterruptedException {
        Station station = new Station();
        station.onCreate();
        assertNotNull(station.getCreatedAt());
        assertNotNull(station.getUpdatedAt());
        assertEquals(station.getCreatedAt(), station.getUpdatedAt());

        // Chờ một chút để đảm bảo updatedAt sẽ khác
        Thread.sleep(10);
        station.onUpdate();
        assertNotNull(station.getUpdatedAt());
        assertTrue(station.getUpdatedAt().isAfter(station.getCreatedAt()));
    }

    // --- Model Tests ---

    @Test
    void testModelBuilder() {
        Model model = Model.builder()
                .modelId(1L)
                .modelName("VinFast VF 3")
                .vehicleType(VehicleType.CAR)
                .seatCount(4)
                .batteryCapacity(40.0)
                .rangeKm(200.0)
                .features("AI")
                .pricePerHour(150000.0)
                .initialValue(300000000.0)
                .description("Desc")
                .imagePaths("/path/img.jpg")
                .build();

        assertEquals(1L, model.getModelId());
        assertEquals("VinFast VF 3", model.getModelName());
        assertEquals(VehicleType.CAR, model.getVehicleType());
        assertEquals(4, model.getSeatCount());
        assertEquals(40.0, model.getBatteryCapacity());
        assertEquals(200.0, model.getRangeKm());
        assertEquals("AI", model.getFeatures());
        assertEquals(150000.0, model.getPricePerHour());
        assertEquals(300000000.0, model.getInitialValue());
        assertEquals("Desc", model.getDescription());
        assertEquals("/path/img.jpg", model.getImagePaths());
    }

    @Test
    void testModelNoArgsConstructorAndSetters() {
        Model model = new Model();
        LocalDateTime time = LocalDateTime.now();

        model.setModelId(1L);
        model.setModelName("VF 3");
        model.setVehicleType(VehicleType.CAR);
        model.setSeatCount(4);
        model.setBatteryCapacity(40.0);
        model.setRangeKm(200.0);
        model.setFeatures("AI");
        model.setPricePerHour(150000.0);
        model.setInitialValue(300000000.0);
        model.setDescription("Desc");
        model.setImagePaths("/path/img.jpg");
        model.setCreatedAt(time);
        model.setUpdatedAt(time);

        assertEquals(1L, model.getModelId());
        assertEquals("VF 3", model.getModelName());
        assertEquals(VehicleType.CAR, model.getVehicleType());
        assertEquals(4, model.getSeatCount());
        assertEquals(40.0, model.getBatteryCapacity());
        assertEquals(200.0, model.getRangeKm());
        assertEquals("AI", model.getFeatures());
        assertEquals(150000.0, model.getPricePerHour());
        assertEquals(300000000.0, model.getInitialValue());
        assertEquals("Desc", model.getDescription());
        assertEquals("/path/img.jpg", model.getImagePaths());
        assertEquals(time, model.getCreatedAt());
        assertEquals(time, model.getUpdatedAt());
    }

    @Test
    void testModelAllArgsConstructor() {
        LocalDateTime time = LocalDateTime.now();
        Model model = new Model(1L, "VF 3", VehicleType.CAR, 4, 40.0, 200.0, "AI",
                150000.0, 300000000.0, "Desc", "/path/img.jpg", time, time);
        assertEquals(1L, model.getModelId());
        assertEquals("VF 3", model.getModelName());
    }

    // --- Vehicle Tests ---

    @Test
    void testVehicleBuilder() {
        Station station = Station.builder().stationId(1L).name("Station 1").build();
        Model model = Model.builder().modelId(1L).modelName("Model 1").build();
        LocalDateTime time = LocalDateTime.now();

        Vehicle vehicle = Vehicle.builder()
                .vehicleId(1L)
                .licensePlate("51A-12345")
                .station(station)
                .model(model)
                .status(VehicleStatus.AVAILABLE)
                .batteryLevel(100)
                .currentMileage(1500.0)
                .condition(VehicleCondition.GOOD)
                .createdAt(time)
                .DamageReportPhotos("/path/dmg.jpg")
                .build();

        assertEquals(1L, vehicle.getVehicleId());
        assertEquals("51A-12345", vehicle.getLicensePlate());
        assertEquals(station, vehicle.getStation());
        assertEquals(model, vehicle.getModel());
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        assertEquals(100, vehicle.getBatteryLevel());
        assertEquals(1500.0, vehicle.getCurrentMileage());
        assertEquals(VehicleCondition.GOOD, vehicle.getCondition());
        assertEquals(time, vehicle.getCreatedAt());
        assertEquals("/path/dmg.jpg", vehicle.getDamageReportPhotos());
    }

    @Test
    void testVehicleNoArgsConstructorAndSetters() {
        Vehicle vehicle = new Vehicle();
        Station station = new Station();
        Model model = new Model();
        LocalDateTime time = LocalDateTime.now();

        vehicle.setVehicleId(1L);
        vehicle.setLicensePlate("51A-12345");
        vehicle.setStation(station);
        vehicle.setModel(model);
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicle.setBatteryLevel(100);
        vehicle.setCurrentMileage(1500.0);
        vehicle.setCondition(VehicleCondition.MINOR_DAMAGE);
        vehicle.setCreatedAt(time);
        vehicle.setDamageReportPhotos("/path/dmg.jpg");

        assertEquals(1L, vehicle.getVehicleId());
        assertEquals("51A-12345", vehicle.getLicensePlate());
        assertEquals(station, vehicle.getStation());
        assertEquals(model, vehicle.getModel());
        assertEquals(VehicleStatus.RENTED, vehicle.getStatus());
        assertEquals(100, vehicle.getBatteryLevel());
        assertEquals(1500.0, vehicle.getCurrentMileage());
        assertEquals(VehicleCondition.MINOR_DAMAGE, vehicle.getCondition());
        assertEquals(time, vehicle.getCreatedAt());
        assertEquals("/path/dmg.jpg", vehicle.getDamageReportPhotos());
    }

    @Test
    void testVehicleAllArgsConstructor() {
        Vehicle vehicle = new Vehicle(1L, "51A", 100, new Model(), VehicleStatus.RESERVED,
                new Station(), 1500.0, VehicleCondition.MAINTENANCE_REQUIRED,
                LocalDateTime.now(), "/path/dmg.jpg");
        assertEquals(1L, vehicle.getVehicleId());
        assertEquals(VehicleStatus.RESERVED, vehicle.getStatus());
    }

    @Test
    void testVehicleDefaultValues() {
        Vehicle vehicle = new Vehicle();
        assertEquals(VehicleStatus.AVAILABLE, vehicle.getStatus());
        assertEquals(VehicleCondition.GOOD, vehicle.getCondition());
    }

    // --- Booking Tests ---

    @Test
    void testBookingBuilder() {
        User user = User.builder().userId(1L).email("test@test.com").build();
        Station station = Station.builder().stationId(1L).name("Station 1").build();
        Vehicle vehicle = Vehicle.builder().vehicleId(1L).licensePlate("51A-12345").build();

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        LocalDateTime time = LocalDateTime.now();

        Booking booking = Booking.builder()
                .bookingId(1L)
                .user(user)
                .vehicle(vehicle)
                .station(station)
                .startDate(startDate)
                .endDate(endDate)
                .status(BookingStatus.CONFIRMED)
                .reservationDepositPaid(true)
                .rentalDepositPaid(true)
                .refund(500000.0)
                .refundNote("Note")
                .rentalDeposit(200000.0)
                .finalFee(1000000.0)
                .invoicePdfPath("/invoice.pdf")
                .checkInPhotoPaths("[]")
                .createdAt(time)
                .build();

        assertEquals(1L, booking.getBookingId());
        assertEquals(user, booking.getUser());
        assertEquals(vehicle, booking.getVehicle());
        assertEquals(station, booking.getStation());
        // Sửa: Booking không có trường model
        // assertEquals(model, booking.getModel());
        assertEquals(startDate, booking.getStartDate());
        assertEquals(endDate, booking.getEndDate());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertTrue(booking.isReservationDepositPaid());
        assertTrue(booking.isRentalDepositPaid());
        assertEquals(500000.0, booking.getRefund());
        assertEquals("Note", booking.getRefundNote());
        assertEquals(200000.0, booking.getRentalDeposit());
        assertEquals(1000000.0, booking.getFinalFee());
        assertEquals("/invoice.pdf", booking.getInvoicePdfPath());
        assertEquals("[]", booking.getCheckInPhotoPaths());
        assertEquals(time, booking.getCreatedAt());
    }

    @Test
    void testBookingNoArgsConstructorAndSetters() {
        Booking booking = new Booking();
        User user = new User();
        Vehicle vehicle = new Vehicle();
        Station station = new Station();
        LocalDateTime time = LocalDateTime.now();

        booking.setBookingId(1L);
        booking.setUser(user);
        booking.setVehicle(vehicle);
        booking.setStation(station);
        booking.setStartDate(time);
        booking.setEndDate(time.plusHours(1));
        booking.setReservationDepositPaid(true);
        booking.setRentalDepositPaid(true);
        booking.setRefund(500000.0);
        booking.setRefundNote("Note");
        booking.setRentalDeposit(200000.0);
        booking.setFinalFee(1000000.0);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setInvoicePdfPath("/invoice.pdf");
        booking.setCheckInPhotoPaths("[]");
        booking.setCreatedAt(time);

        assertEquals(1L, booking.getBookingId());
        assertEquals(user, booking.getUser());
        assertEquals(vehicle, booking.getVehicle());
        assertEquals(station, booking.getStation());
        assertEquals(time, booking.getStartDate());
        assertEquals(time.plusHours(1), booking.getEndDate());
        assertTrue(booking.isReservationDepositPaid());
        assertTrue(booking.isRentalDepositPaid());
        assertEquals(500000.0, booking.getRefund());
        assertEquals("Note", booking.getRefundNote());
        assertEquals(200000.0, booking.getRentalDeposit());
        assertEquals(1000000.0, booking.getFinalFee());
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertEquals("/invoice.pdf", booking.getInvoicePdfPath());
        assertEquals("[]", booking.getCheckInPhotoPaths());
        assertEquals(time, booking.getCreatedAt());
    }

    @Test
    void testBookingAllArgsConstructor() {
        Booking booking = new Booking(1L, new User(), new Vehicle(), new Station(),
                LocalDateTime.now(), LocalDateTime.now(), true, true,
                500.0, "Note", 200.0, 1000.0, BookingStatus.PENDING,
                "/path", "[]", LocalDateTime.now());
        assertEquals(1L, booking.getBookingId());
        assertEquals(BookingStatus.PENDING, booking.getStatus());
    }

    @Test
    void testBookingDefaultValues() {
        Booking booking = new Booking();
        assertFalse(booking.isReservationDepositPaid());
        assertFalse(booking.isRentalDepositPaid());
    }

    // --- Contract Tests ---

    @Test
    void testContractBuilder() {
        Booking booking = Booking.builder().bookingId(1L).build();
        LocalDateTime time = LocalDateTime.now();

        Contract contract = Contract.builder()
                .contractId(1L)
                .booking(booking)
                .contractPdfPath("/uploads/contracts/contract_1.pdf")
                .signedDate(time)
                .termsSnapshot("Terms v1")
                .build();

        assertEquals(1L, contract.getContractId());
        assertEquals(booking, contract.getBooking());
        assertEquals("/uploads/contracts/contract_1.pdf", contract.getContractPdfPath());
        assertEquals(time, contract.getSignedDate());
        assertEquals("Terms v1", contract.getTermsSnapshot());
    }

    @Test
    void testContractNoArgsConstructorAndSetters() {
        Contract contract = new Contract();
        Booking booking = new Booking();
        LocalDateTime time = LocalDateTime.now();

        contract.setContractId(1L);
        contract.setBooking(booking);
        contract.setContractPdfPath("/path");
        contract.setSignedDate(time);
        contract.setTermsSnapshot("Terms");

        assertEquals(1L, contract.getContractId());
        assertEquals(booking, contract.getBooking());
        assertEquals("/path", contract.getContractPdfPath());
        assertEquals(time, contract.getSignedDate());
        assertEquals("Terms", contract.getTermsSnapshot());
    }

    @Test
    void testContractAllArgsConstructor() {
        Contract contract = new Contract(1L, new Booking(), "/path", LocalDateTime.now(), "Terms");
        assertEquals(1L, contract.getContractId());
        assertEquals("/path", contract.getContractPdfPath());
    }


    // --- Role Tests ---

    @Test
    void testRoleBuilder() {
        Role role = Role.builder()
                .roleId(1L)
                .roleName("EV_RENTER")
                .users(new ArrayList<>())
                .build();

        assertEquals(1L, role.getRoleId());
        assertEquals("EV_RENTER", role.getRoleName());
        assertNotNull(role.getUsers());
        assertTrue(role.getUsers().isEmpty());
    }

    @Test
    void testRoleNoArgsConstructorAndSetters() {
        Role role = new Role();
        List<User> users = new ArrayList<>();
        users.add(new User());

        role.setRoleId(1L);
        role.setRoleName("ADMIN");
        role.setUsers(users);

        assertEquals(1L, role.getRoleId());
        assertEquals("ADMIN", role.getRoleName());
        assertEquals(1, role.getUsers().size());
    }

    @Test
    void testRoleAllArgsConstructor() {
        Role role = new Role(1L, "ADMIN", new ArrayList<>());
        assertEquals(1L, role.getRoleId());
        assertEquals("ADMIN", role.getRoleName());
    }

    @Test
    void testRoleDefaultValues() {
        Role role = new Role();
        assertNotNull(role.getUsers());
        assertTrue(role.getUsers().isEmpty());
    }

    @Test
    void testRoleToString() {
        Role role = new Role();
        role.setRoleName("ADMIN");
        // Kiểm tra @ToString.Exclude
        assertFalse(role.toString().contains("users="));
        assertTrue(role.toString().contains("roleName=ADMIN"));
    }

    // --- AuthToken Tests ---

    @Test
    void testAuthTokenBuilder() {
        User user = new User();
        LocalDateTime time = LocalDateTime.now();
        // Sửa: expiresAt là LocalDateTime, không phải Instant
        AuthToken token = AuthToken.builder()
                .id(1L)
                .token("jwt-token-123")
                .expiresAt(time.plusSeconds(3600))
                .createdAt(time)
                .user(user)
                .build();

        assertEquals(1L, token.getId());
        assertEquals("jwt-token-123", token.getToken());
        assertEquals(user, token.getUser());
        assertEquals(time, token.getCreatedAt());
        assertNotNull(token.getExpiresAt());
    }

    @Test
    void testAuthTokenNoArgsConstructorAndSetters() {
        AuthToken token = new AuthToken();
        User user = new User();
        LocalDateTime time = LocalDateTime.now();

        token.setId(1L);
        token.setToken("token");
        token.setUser(user);
        token.setCreatedAt(time);
        token.setExpiresAt(time.plusHours(1));

        assertEquals(1L, token.getId());
        assertEquals("token", token.getToken());
        assertEquals(user, token.getUser());
        assertEquals(time, token.getCreatedAt());
        assertEquals(time.plusHours(1), token.getExpiresAt());
    }

    @Test
    void testAuthTokenAllArgsConstructor() {
        AuthToken token = new AuthToken(1L, "token", new User(), LocalDateTime.now(), LocalDateTime.now());
        assertEquals(1L, token.getId());
        assertEquals("token", token.getToken());
    }

    // --- Rating Tests ---

    @Test
    void testRatingBuilder() {
        Station station = new Station();
        LocalDateTime time = LocalDateTime.now();

        Rating rating = Rating.builder()
                .id(1L)
                .userId(1L)
                .station(station)
                .stars(5)
                .comment("Great service!")
                .createdAt(time)
                .build();

        assertEquals(1L, rating.getId());
        assertEquals(1L, rating.getUserId());
        assertEquals(station, rating.getStation());
        assertEquals(5, rating.getStars());
        assertEquals("Great service!", rating.getComment());
        assertEquals(time, rating.getCreatedAt());
    }

    @Test
    void testRatingNoArgsConstructorAndSetters() {
        Rating rating = new Rating();
        Station station = new Station();
        LocalDateTime time = LocalDateTime.now();

        rating.setId(1L);
        rating.setUserId(1L);
        rating.setStation(station);
        rating.setStars(5);
        rating.setComment("Comment");
        rating.setCreatedAt(time);

        assertEquals(1L, rating.getId());
        assertEquals(1L, rating.getUserId());
        assertEquals(station, rating.getStation());
        assertEquals(5, rating.getStars());
        assertEquals("Comment", rating.getComment());
        assertEquals(time, rating.getCreatedAt());
    }

    @Test
    void testRatingAllArgsConstructor() {
        Rating rating = new Rating(1L, new Station(), 1L, 5, "Comment", LocalDateTime.now());
        assertEquals(1L, rating.getId());
        assertEquals(5, rating.getStars());
    }

    @Test
    void testRatingDefaultValues() {
        Rating rating = new Rating();
        assertNotNull(rating.getCreatedAt());
    }

    // --- Transaction Tests ---

    @Test
    void testTransactionBuilder() {
        Booking booking = Booking.builder().bookingId(1L).build();
        User staff = User.builder().userId(10L).build();
        LocalDateTime time = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .transactionId(1L)
                .booking(booking)
                .staff(staff)
                .amount(200000.0)
                .paymentMethod(PaymentMethod.CASH)
                .transactionDate(time)
                .staffNote("Note")
                .build();

        assertEquals(1L, transaction.getTransactionId());
        assertEquals(booking, transaction.getBooking());
        assertEquals(staff, transaction.getStaff());
        assertEquals(200000.0, transaction.getAmount());
        assertEquals(PaymentMethod.CASH, transaction.getPaymentMethod());
        assertEquals(time, transaction.getTransactionDate());
        assertEquals("Note", transaction.getStaffNote());
    }

    @Test
    void testTransactionNoArgsConstructorAndSetters() {
        Transaction transaction = new Transaction();
        Booking booking = new Booking();
        User staff = new User();
        LocalDateTime time = LocalDateTime.now();

        transaction.setTransactionId(1L);
        transaction.setBooking(booking);
        transaction.setStaff(staff);
        transaction.setAmount(200.0);
        transaction.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        transaction.setTransactionDate(time);
        transaction.setStaffNote("Note");

        assertEquals(1L, transaction.getTransactionId());
        assertEquals(booking, transaction.getBooking());
        assertEquals(staff, transaction.getStaff());
        assertEquals(200.0, transaction.getAmount());
        assertEquals(PaymentMethod.BANK_TRANSFER, transaction.getPaymentMethod());
        assertEquals(time, transaction.getTransactionDate());
        assertEquals("Note", transaction.getStaffNote());
    }

    @Test
    void testTransactionAllArgsConstructor() {
        Transaction transaction = new Transaction(1L, new Booking(), 200.0,
                PaymentMethod.GATEWAY, LocalDateTime.now(), "Note", new User());
        assertEquals(1L, transaction.getTransactionId());
        assertEquals(200.0, transaction.getAmount());
    }

    // --- TransactionDetail Tests (MỚI) ---

    @Test
    void testTransactionDetailBuilder() {
        Booking booking = new Booking();
        PenaltyFee fee = new PenaltyFee();
        TransactionDetail detail = TransactionDetail.builder()
                .detailId(1L)
                .booking(booking)
                .penaltyFee(fee)
                .appliedAmount(100.0)
                .staffNote("Staff Note")
                .adjustmentNote("Adjustment Note")
                .photoPaths("[]")
                .build();

        assertEquals(1L, detail.getDetailId());
        assertEquals(booking, detail.getBooking());
        assertEquals(fee, detail.getPenaltyFee());
        assertEquals(100.0, detail.getAppliedAmount());
        assertEquals("Staff Note", detail.getStaffNote());
        assertEquals("Adjustment Note", detail.getAdjustmentNote());
        assertEquals("[]", detail.getPhotoPaths());
    }

    @Test
    void testTransactionDetailNoArgsConstructorAndSetters() {
        TransactionDetail detail = new TransactionDetail();
        Booking booking = new Booking();
        PenaltyFee fee = new PenaltyFee();

        detail.setDetailId(1L);
        detail.setBooking(booking);
        detail.setPenaltyFee(fee);
        detail.setAppliedAmount(100.0);
        detail.setStaffNote("Staff Note");
        detail.setAdjustmentNote("Adjustment Note");
        detail.setPhotoPaths("[]");

        assertEquals(1L, detail.getDetailId());
        assertEquals(booking, detail.getBooking());
        assertEquals(fee, detail.getPenaltyFee());
        assertEquals(100.0, detail.getAppliedAmount());
        assertEquals("Staff Note", detail.getStaffNote());
        assertEquals("Adjustment Note", detail.getAdjustmentNote());
        assertEquals("[]", detail.getPhotoPaths());
    }

    @Test
    void testTransactionDetailAllArgsConstructor() {
        TransactionDetail detail = new TransactionDetail(1L, new Booking(), new PenaltyFee(), 100.0,
                "Staff", "Adj", "[]");
        assertEquals(1L, detail.getDetailId());
        assertEquals(100.0, detail.getAppliedAmount());
    }

    // --- VehicleHistory Tests ---

    @Test
    void testVehicleHistoryBuilder() {
        Vehicle vehicle = Vehicle.builder().vehicleId(1L).build();
        User staff = new User();
        User renter = new User();
        Station station = new Station();
        LocalDateTime time = LocalDateTime.now();

        VehicleHistory history = VehicleHistory.builder()
                .historyId(1L)
                .vehicle(vehicle)
                .staff(staff)
                .renter(renter)
                .station(station)
                .actionType(VehicleActionType.DELIVERY)
                .note("Vehicle created")
                .conditionBefore("New")
                .conditionAfter("Used")
                .batteryLevel(100.0)
                .mileage(10.0)
                .photoPaths("[]")
                .actionTime(time)
                .build();

        assertEquals(1L, history.getHistoryId());
        assertEquals(vehicle, history.getVehicle());
        assertEquals(staff, history.getStaff());
        assertEquals(renter, history.getRenter());
        assertEquals(station, history.getStation());
        assertEquals(VehicleActionType.DELIVERY, history.getActionType());
        assertEquals("Vehicle created", history.getNote());
        assertEquals("New", history.getConditionBefore());
        assertEquals("Used", history.getConditionAfter());
        assertEquals(100.0, history.getBatteryLevel());
        assertEquals(10.0, history.getMileage());
        assertEquals("[]", history.getPhotoPaths());
        assertEquals(time, history.getActionTime());
    }

    @Test
    void testVehicleHistoryNoArgsConstructorAndSetters() {
        VehicleHistory history = new VehicleHistory();
        Vehicle vehicle = new Vehicle();
        User staff = new User();
        User renter = new User();
        Station station = new Station();
        LocalDateTime time = LocalDateTime.now();

        history.setHistoryId(1L);
        history.setVehicle(vehicle);
        history.setStaff(staff);
        history.setRenter(renter);
        history.setStation(station);
        history.setActionType(VehicleActionType.RETURN);
        history.setNote("Note");
        history.setConditionBefore("New");
        history.setConditionAfter("Used");
        history.setBatteryLevel(100.0);
        history.setMileage(10.0);
        history.setPhotoPaths("[]");
        history.setActionTime(time);

        assertEquals(1L, history.getHistoryId());
        assertEquals(vehicle, history.getVehicle());
        assertEquals(staff, history.getStaff());
        assertEquals(renter, history.getRenter());
        assertEquals(station, history.getStation());
        assertEquals(VehicleActionType.RETURN, history.getActionType());
        assertEquals("Note", history.getNote());
        assertEquals("New", history.getConditionBefore());
        assertEquals("Used", history.getConditionAfter());
        assertEquals(100.0, history.getBatteryLevel());
        assertEquals(10.0, history.getMileage());
        assertEquals("[]", history.getPhotoPaths());
        assertEquals(time, history.getActionTime());
    }

    @Test
    void testVehicleHistoryAllArgsConstructor() {
        VehicleHistory history = new VehicleHistory(1L, new Vehicle(), new User(), new User(), new Station(),
                VehicleActionType.MAINTENANCE, "Note", "Before", "After",
                100.0, 10.0, "[]", LocalDateTime.now());
        assertEquals(1L, history.getHistoryId());
        assertEquals("Note", history.getNote());
    }

    // --- PenaltyFee Tests ---

    @Test
    void testPenaltyFeeBuilder() {
        PenaltyFee penalty = PenaltyFee.builder()
                .feeId(1L)
                .feeName("Broken")
                .fixedAmount(100000.0)
                .description("Late return")
                .isAdjustment(true)
                .build();

        assertEquals(1L, penalty.getFeeId());
        assertEquals("Broken", penalty.getFeeName());
        assertEquals(100000.0, penalty.getFixedAmount());
        assertEquals("Late return", penalty.getDescription());
        assertTrue(penalty.getIsAdjustment());
    }

    @Test
    void testPenaltyFeeNoArgsConstructorAndSetters() {
        PenaltyFee penalty = new PenaltyFee();
        penalty.setFeeId(1L);
        penalty.setFeeName("Name");
        penalty.setFixedAmount(100.0);
        penalty.setDescription("Desc");
        penalty.setIsAdjustment(true);

        assertEquals(1L, penalty.getFeeId());
        assertEquals("Name", penalty.getFeeName());
        assertEquals(100.0, penalty.getFixedAmount());
        assertEquals("Desc", penalty.getDescription());
        assertTrue(penalty.getIsAdjustment());
    }

    @Test
    void testPenaltyFeeAllArgsConstructor() {
        PenaltyFee penalty = new PenaltyFee(1L, "Name", 100.0, "Desc", true);
        assertEquals(1L, penalty.getFeeId());
        assertTrue(penalty.getIsAdjustment());
    }

    @Test
    void testPenaltyFeeDefaultValues() {
        PenaltyFee penalty = new PenaltyFee();
        assertFalse(penalty.getIsAdjustment());
    }

    // --- PasswordReset Tests ---

    @Test
    void testPasswordResetBuilder() {
        User user = User.builder().userId(1L).build();
        LocalDateTime time = LocalDateTime.now().plusHours(1);

        PasswordReset reset = PasswordReset.builder()
                .id(1L)
                .user(user)
                .otpCode("95742")
                .expiryDate(time)
                .build();

        assertEquals(1L, reset.getId());
        assertEquals(user, reset.getUser());
        assertEquals("95742", reset.getOtpCode());
        assertEquals(time, reset.getExpiryDate());
    }

    @Test
    void testPasswordResetNoArgsConstructorAndSetters() {
        PasswordReset reset = new PasswordReset();
        User user = new User();
        LocalDateTime time = LocalDateTime.now();

        reset.setId(1L);
        reset.setUser(user);
        reset.setOtpCode("123");
        reset.setExpiryDate(time);

        assertEquals(1L, reset.getId());
        assertEquals(user, reset.getUser());
        assertEquals("123", reset.getOtpCode());
        assertEquals(time, reset.getExpiryDate());
    }

    @Test
    void testPasswordResetAllArgsConstructor() {
        PasswordReset reset = new PasswordReset(1L, "123", LocalDateTime.now(), new User());
        assertEquals(1L, reset.getId());
        assertEquals("123", reset.getOtpCode());
    }

    // --- Enum Tests (Đầy đủ) ---

    @Test
    void testAccountStatusEnum() {
        assertEquals(AccountStatus.ACTIVE, AccountStatus.valueOf("ACTIVE"));
        assertEquals(AccountStatus.INACTIVE, AccountStatus.valueOf("INACTIVE"));
    }

    @Test
    void testVerificationStatusEnum() {
        assertEquals(VerificationStatus.PENDING, VerificationStatus.valueOf("PENDING"));
        assertEquals(VerificationStatus.APPROVED, VerificationStatus.valueOf("APPROVED"));
        assertEquals(VerificationStatus.REJECTED, VerificationStatus.valueOf("REJECTED"));
    }

    @Test
    void testBookingStatusEnum() {
        assertEquals(BookingStatus.PENDING, BookingStatus.valueOf("PENDING"));
        assertEquals(BookingStatus.CONFIRMED, BookingStatus.valueOf("CONFIRMED"));
        assertEquals(BookingStatus.RENTING, BookingStatus.valueOf("RENTING"));
        assertEquals(BookingStatus.COMPLETED, BookingStatus.valueOf("COMPLETED"));
        assertEquals(BookingStatus.CANCELLED, BookingStatus.valueOf("CANCELLED"));
        assertEquals(BookingStatus.CANCELLED_AWAIT_REFUND, BookingStatus.valueOf("CANCELLED_AWAIT_REFUND"));
        assertEquals(BookingStatus.REFUNDED, BookingStatus.valueOf("REFUNDED"));
    }

    @Test
    void testVehicleStatusEnum() {
        assertEquals(VehicleStatus.AVAILABLE, VehicleStatus.valueOf("AVAILABLE"));
        assertEquals(VehicleStatus.RENTED, VehicleStatus.valueOf("RENTED"));
        assertEquals(VehicleStatus.RESERVED, VehicleStatus.valueOf("RESERVED"));
        assertEquals(VehicleStatus.UNAVAILABLE, VehicleStatus.valueOf("UNAVAILABLE"));
    }

    @Test
    void testStationStatusEnum() {
        assertEquals(StationStatus.ACTIVE, StationStatus.valueOf("ACTIVE"));
        assertEquals(StationStatus.INACTIVE, StationStatus.valueOf("INACTIVE"));
        assertEquals(StationStatus.MAINTENANCE, StationStatus.valueOf("MAINTENANCE"));
    }

    @Test
    void testVehicleActionTypeEnum() {
        assertEquals(VehicleActionType.DELIVERY, VehicleActionType.valueOf("DELIVERY"));
        assertEquals(VehicleActionType.RETURN, VehicleActionType.valueOf("RETURN"));
        assertEquals(VehicleActionType.MAINTENANCE, VehicleActionType.valueOf("MAINTENANCE"));
        assertEquals(VehicleActionType.TRANSFER, VehicleActionType.valueOf("TRANSFER"));
    }

    @Test
    void testVehicleConditionEnum() {
        assertEquals(VehicleCondition.EXCELLENT, VehicleCondition.valueOf("EXCELLENT"));
        assertEquals(VehicleCondition.GOOD, VehicleCondition.valueOf("GOOD"));
        assertEquals(VehicleCondition.MINOR_DAMAGE, VehicleCondition.valueOf("MINOR_DAMAGE"));
        assertEquals(VehicleCondition.MAINTENANCE_REQUIRED, VehicleCondition.valueOf("MAINTENANCE_REQUIRED"));
    }

    @Test
    void testPaymentMethodEnum() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.valueOf("CASH"));
        assertEquals(PaymentMethod.BANK_TRANSFER, PaymentMethod.valueOf("BANK_TRANSFER"));
        assertEquals(PaymentMethod.GATEWAY, PaymentMethod.valueOf("GATEWAY"));
    }

    @Test
    void testVehicleTypeEnum() {
        assertEquals(VehicleType.CAR, VehicleType.valueOf("CAR"));
        assertEquals(VehicleType.MOTORBIKE, VehicleType.valueOf("MOTORBIKE"));
    }
}