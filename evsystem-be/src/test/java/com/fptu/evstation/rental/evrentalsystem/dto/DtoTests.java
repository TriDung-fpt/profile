package com.fptu.evstation.rental.evrentalsystem.dto;

import com.fptu.evstation.rental.evrentalsystem.entity.BookingStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.PaymentMethod;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleCondition;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp test này được thiết kế để đạt 100% coverage cho tất cả các DTO.
 * Để làm điều này, chúng ta phải gọi mọi constructor (no-args, all-args),
 * builder,
 * tất cả các setters và getters, và các phương thức equals(), hashCode(), toString()
 * mà Lombok tạo ra.
 */
class DtoTests {

    private final LocalDateTime now = LocalDateTime.now();
    private final LocalDate nowDate = LocalDate.now();
    private final MockMultipartFile mockFile = new MockMultipartFile("file", "test.png", "image/png", "test data".getBytes());

    @Test
    void testAuthResponse() {
        AuthResponse dto = new AuthResponse();
        dto.setToken("token");
        dto.setExpiresAt(now);
        dto.setFullName("Full Name");

        assertEquals("token", dto.getToken());
        assertEquals(now, dto.getExpiresAt());
        assertEquals("Full Name", dto.getFullName());

        AuthResponse allArgs = new AuthResponse("token2", now.plusDays(1), "Name 2");
        AuthResponse builder = AuthResponse.builder()
                .token("token3")
                .expiresAt(now.plusDays(2))
                .fullName("Name 3")
                .build();

        assertNotNull(allArgs.getToken());
        assertNotNull(builder.getToken());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testBillResponse() {
        BillResponse.FeeItem feeItem = BillResponse.FeeItem.builder()
                .feeName("Fee")
                .amount(100.0)
                .staffNote("Note")
                .adjustmentNote("Adj Note")
                .build();
        feeItem.setFeeName("New Fee");
        assertEquals("New Fee", feeItem.getFeeName());
        assertEquals(100.0, feeItem.getAmount());
        assertEquals("Note", feeItem.getStaffNote());
        assertEquals("Adj Note", feeItem.getAdjustmentNote());
        assertNotNull(feeItem.toString());
        assertEquals(feeItem.hashCode(), feeItem.hashCode());
        assertEquals(feeItem, feeItem);


        BillResponse dto = BillResponse.builder()
                .bookingId(1L)
                .dateTime(now)
                .userName("User")
                .baseRentalFee(1000.0)
                .totalPenaltyFee(100.0)
                .totalDiscount(50.0)
                .downpayPaid(500.0)
                .paymentDue(550.0)
                .refundToCustomer(0.0)
                .feeItems(Collections.singletonList(feeItem))
                .qrCodeUrl("qr-code")
                .invoicePdfPath("/path/to/invoice.pdf")
                .build();

        dto.setBookingId(2L);
        assertEquals(2L, dto.getBookingId());
        assertEquals(now, dto.getDateTime());
        assertEquals("User", dto.getUserName());
        assertEquals(1000.0, dto.getBaseRentalFee());
        assertEquals(100.0, dto.getTotalPenaltyFee());
        assertEquals(50.0, dto.getTotalDiscount());
        assertEquals(500.0, dto.getDownpayPaid());
        assertEquals(550.0, dto.getPaymentDue());
        assertEquals(0.0, dto.getRefundToCustomer());
        assertEquals(1, dto.getFeeItems().size());
        assertEquals("qr-code", dto.getQrCodeUrl());
        assertEquals("/path/to/invoice.pdf", dto.getInvoicePdfPath());

        assertNotNull(dto.toString());
        assertEquals(dto.hashCode(), dto.hashCode());
        assertEquals(dto, dto);
    }

    @Test
    void testBookingDetailResponse() {
        BookingDetailResponse dto = BookingDetailResponse.builder()
                .bookingId(1L)
                .status(BookingStatus.PENDING)
                .createdAt(now)
                .startDate(now.plusDays(1))
                .endDate(now.plusDays(2))
                .renterName("Renter")
                .renterPhone("12345")
                .modelName("Model")
                .vehicleLicensePlate("123-ABC")
                .stationName("Station")
                .stationAddress("Address")
                .rentalDeposit(100.0)
                .finalFee(1000.0)
                .checkInPhotoPaths(Collections.singletonList("/path/photo.png"))
                .invoicePdfPath("/path/invoice.pdf")
                .contractPdfPath("/path/contract.pdf")
                .build();

        dto.setBookingId(2L);
        assertEquals(2L, dto.getBookingId());
        assertEquals(BookingStatus.PENDING, dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now.plusDays(1), dto.getStartDate());
        assertEquals(now.plusDays(2), dto.getEndDate());
        assertEquals("Renter", dto.getRenterName());
        assertEquals("12345", dto.getRenterPhone());
        assertEquals("Model", dto.getModelName());
        assertEquals("123-ABC", dto.getVehicleLicensePlate());
        assertEquals("Station", dto.getStationName());
        assertEquals("Address", dto.getStationAddress());
        assertEquals(100.0, dto.getRentalDeposit());
        assertEquals(1000.0, dto.getFinalFee());
        assertEquals(1, dto.getCheckInPhotoPaths().size());
        assertEquals("/path/invoice.pdf", dto.getInvoicePdfPath());
        assertEquals("/path/contract.pdf", dto.getContractPdfPath());

        assertNotNull(dto.toString());
        assertEquals(dto.hashCode(), dto.hashCode());
        assertEquals(dto, dto);
    }

    @Test
    void testBookingRequest() {
        BookingRequest dto = new BookingRequest();
        dto.setVehicleId(1L);
        dto.setStartTime(now);
        dto.setEndTime(now.plusHours(2));
        dto.setAgreedToTerms(true);

        assertEquals(1L, dto.getVehicleId());
        assertEquals(now, dto.getStartTime());
        assertEquals(now.plusHours(2), dto.getEndTime());
        assertTrue(dto.isAgreedToTerms());

        BookingRequest allArgs = new BookingRequest(2L, now, now.plusHours(3), false);
        BookingRequest builder = BookingRequest.builder()
                .vehicleId(3L)
                .startTime(now)
                .endTime(now.plusHours(4))
                .agreedToTerms(true)
                .build();

        assertEquals(2L, allArgs.getVehicleId());
        assertEquals(3L, builder.getVehicleId());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testBookingSummaryResponse() {
        BookingSummaryResponse dto = BookingSummaryResponse.builder()
                .bookingId(1L)
                .renterName("Renter")
                .vehicleLicensePlate("123-ABC")
                .modelName("Model")
                .vehicleStatus(VehicleStatus.AVAILABLE)
                .batteryLevel(80)
                .currentMileage(1000.0)
                .bookingStatus(BookingStatus.PENDING)
                .createdAt(now)
                .startDate(now.plusDays(1))
                .renterPhone("12345")
                .refundAmount(100.0)
                .refundInfo("Bank info")
                .build();

        dto.setBookingId(2L);
        assertEquals(2L, dto.getBookingId());
        assertEquals("Renter", dto.getRenterName());
        assertEquals("123-ABC", dto.getVehicleLicensePlate());
        assertEquals("Model", dto.getModelName());
        assertEquals(VehicleStatus.AVAILABLE, dto.getVehicleStatus());
        assertEquals(80, dto.getBatteryLevel());
        assertEquals(1000.0, dto.getCurrentMileage());
        assertEquals(BookingStatus.PENDING, dto.getBookingStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now.plusDays(1), dto.getStartDate());
        assertEquals("12345", dto.getRenterPhone());
        assertEquals(100.0, dto.getRefundAmount());
        assertEquals("Bank info", dto.getRefundInfo());

        BookingSummaryResponse allArgs = new BookingSummaryResponse(3L, null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals(3L, allArgs.getBookingId());

        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testCancelBookingRequest() {
        CancelBookingRequest dto = new CancelBookingRequest();
        dto.setBankName("Bank");
        dto.setAccountNumber("12345");
        dto.setAccountName("Name");

        assertEquals("Bank", dto.getBankName());
        assertEquals("12345", dto.getAccountNumber());
        assertEquals("Name", dto.getAccountName());

        CancelBookingRequest sameDto = new CancelBookingRequest();
        sameDto.setBankName("Bank");
        sameDto.setAccountNumber("12345");
        sameDto.setAccountName("Name");

        assertNotNull(dto.toString());
        assertEquals(dto, sameDto);
        assertEquals(dto.hashCode(), sameDto.hashCode());
        assertNotEquals(dto, new CancelBookingRequest());
    }

    @Test
    void testCheckInRequest() {
        CheckInRequest dto = new CheckInRequest();
        dto.setDepositPaymentMethod(PaymentMethod.CASH);
        dto.setConditionBefore("Good");
        dto.setBattery(90.0);
        dto.setMileage(1000.0);
        dto.setCheckInPhotos(Collections.singletonList(mockFile));

        assertEquals(PaymentMethod.CASH, dto.getDepositPaymentMethod());
        assertEquals("Good", dto.getConditionBefore());
        assertEquals(90.0, dto.getBattery());
        assertEquals(1000.0, dto.getMileage());
        assertEquals(1, dto.getCheckInPhotos().size());

        CheckInRequest allArgs = new CheckInRequest(PaymentMethod.BANK_TRANSFER, "New", 80.0, 2000.0, null);
        CheckInRequest builder = CheckInRequest.builder()
                .depositPaymentMethod(PaymentMethod.GATEWAY)
                .build();

        assertEquals(PaymentMethod.BANK_TRANSFER, allArgs.getDepositPaymentMethod());
        assertEquals(PaymentMethod.GATEWAY, builder.getDepositPaymentMethod());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testContractSummaryResponse() {
        ContractSummaryResponse dto = ContractSummaryResponse.builder()
                .contractId(1L)
                .bookingId(2L)
                .renterName("Renter")
                .staffName("Staff")
                .vehicleLicensePlate("123-ABC")
                .signedDate(now)
                .contractPdfPath("/path/contract.pdf")
                .build();

        dto.setContractId(11L);
        assertEquals(11L, dto.getContractId());
        assertEquals(2L, dto.getBookingId());
        assertEquals("Renter", dto.getRenterName());
        assertEquals("Staff", dto.getStaffName());
        assertEquals("123-ABC", dto.getVehicleLicensePlate());
        assertEquals(now, dto.getSignedDate());
        assertEquals("/path/contract.pdf", dto.getContractPdfPath());

        assertNotNull(dto.toString());
        assertEquals(dto.hashCode(), dto.hashCode());
        assertEquals(dto, dto);
    }

    @Test
    void testCreateModelRequest() {
        CreateModelRequest dto = new CreateModelRequest();
        dto.setModelName("Model");
        dto.setVehicleType("CAR");
        dto.setSeatCount(4);
        dto.setBatteryCapacity(50.0);
        dto.setRangeKm(300.0);
        dto.setFeatures("Features");
        dto.setPricePerHour(100.0);
        dto.setInitialValue(500000.0);
        dto.setDescription("Desc");
        dto.setImagePaths(Collections.singletonList("/path/image.png"));
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        assertEquals("Model", dto.getModelName());
        assertEquals("CAR", dto.getVehicleType());
        assertEquals(4, dto.getSeatCount());
        assertEquals(50.0, dto.getBatteryCapacity());
        assertEquals(300.0, dto.getRangeKm());
        assertEquals("Features", dto.getFeatures());
        assertEquals(100.0, dto.getPricePerHour());
        assertEquals(500000.0, dto.getInitialValue());
        assertEquals("Desc", dto.getDescription());
        assertEquals(1, dto.getImagePaths().size());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());

        CreateModelRequest allArgs = new CreateModelRequest("Model2", "BIKE", 2, 20.0, 100.0, "Feat2", 50.0, 200000.0, "Desc2", null, now, now);
        CreateModelRequest builder = CreateModelRequest.builder().modelName("Model3").build();

        assertEquals("Model2", allArgs.getModelName());
        assertEquals("Model3", builder.getModelName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testCreateVehicleRequest() {
        CreateVehicleRequest dto = new CreateVehicleRequest();
        dto.setLicensePlate("123-ABC");
        dto.setBatteryLevel(80);
        dto.setModelId(1L);
        dto.setStationId(1L);
        dto.setCurrentMileage(1000.0);
        dto.setStatus("AVAILABLE");
        dto.setCondition("GOOD");

        assertEquals("123-ABC", dto.getLicensePlate());
        assertEquals(80, dto.getBatteryLevel());
        assertEquals(1L, dto.getModelId());
        assertEquals(1L, dto.getStationId());
        assertEquals(1000.0, dto.getCurrentMileage());
        assertEquals("AVAILABLE", dto.getStatus());
        assertEquals("GOOD", dto.getCondition());

        CreateVehicleRequest allArgs = new CreateVehicleRequest("321-CBA", 90, 2L, 2L, 2000.0, "RENTED", "FAIR");
        CreateVehicleRequest builder = CreateVehicleRequest.builder().licensePlate("456-DEF").build();

        assertEquals("321-CBA", allArgs.getLicensePlate());
        assertEquals("456-DEF", builder.getLicensePlate());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testDashboardSummaryDto() {
        Map<VehicleStatus, Long> summary = Map.of(VehicleStatus.AVAILABLE, 5L, VehicleStatus.RENTED, 5L);
        DashboardSummaryDto dto = DashboardSummaryDto.builder()
                .stationName("Station")
                .totalVehicles(10L)
                .statusSummary(summary)
                .build();

        dto.setStationName("Station 2");
        assertEquals("Station 2", dto.getStationName());
        assertEquals(10L, dto.getTotalVehicles());
        assertEquals(2, dto.getStatusSummary().size());

        assertNotNull(dto.toString());
        assertEquals(dto.hashCode(), dto.hashCode());
        assertEquals(dto, dto);
    }

    @Test
    void testGoogleIdTokenRequest() {
        GoogleIdTokenRequest dto = new GoogleIdTokenRequest();
        dto.setIdToken("token");
        assertEquals("token", dto.getIdToken());

        GoogleIdTokenRequest allArgs = new GoogleIdTokenRequest("token2");
        assertEquals("token2", allArgs.getIdToken());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testInvoiceSummaryResponse() {
        InvoiceSummaryResponse dto = InvoiceSummaryResponse.builder()
                .bookingId(1L)
                .renterName("Renter")
                .finalAmount(1000.0)
                .createdDate(now)
                .invoicePdfPath("/path/invoice.pdf")
                .build();

        dto.setBookingId(2L);
        assertEquals(2L, dto.getBookingId());
        assertEquals("Renter", dto.getRenterName());
        assertEquals(1000.0, dto.getFinalAmount());
        assertEquals(now, dto.getCreatedDate());
        assertEquals("/path/invoice.pdf", dto.getInvoicePdfPath());

        assertNotNull(dto.toString());
        assertEquals(dto.hashCode(), dto.hashCode());
        assertEquals(dto, dto);
    }

    @Test
    void testLoginRequest() {
        LoginRequest dto = new LoginRequest();
        dto.setIdentifier("id");
        dto.setPassword("pass");

        assertEquals("id", dto.getIdentifier());
        assertEquals("pass", dto.getPassword());

        LoginRequest allArgs = new LoginRequest("id2", "pass2");
        LoginRequest builder = LoginRequest.builder().identifier("id3").build();

        assertEquals("id2", allArgs.getIdentifier());
        assertEquals("id3", builder.getIdentifier());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testModelResponse() {
        ModelResponse dto = ModelResponse.builder()
                .modelId(1L)
                .modelName("Model")
                .vehicleType(VehicleType.CAR)
                .seatCount(4)
                .batteryCapacity(50.0)
                .rangeKm(300.0)
                .pricePerHour(100.0)
                .initialValue(500000.0)
                .features("Features")
                .description("Desc")
                .imagePaths(Collections.singletonList("/path/image.png"))
                .createdAt(now)
                .updatedAt(now)
                .build();

        dto.setModelId(2L);
        assertEquals(2L, dto.getModelId());
        assertEquals("Model", dto.getModelName());
        assertEquals(VehicleType.CAR, dto.getVehicleType());
        assertEquals(4, dto.getSeatCount());
        assertEquals(50.0, dto.getBatteryCapacity());
        assertEquals(300.0, dto.getRangeKm());
        assertEquals(100.0, dto.getPricePerHour());
        assertEquals(500000.0, dto.getInitialValue());
        assertEquals("Features", dto.getFeatures());
        assertEquals("Desc", dto.getDescription());
        assertEquals(1, dto.getImagePaths().size());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());

        assertNotNull(dto.toString());
        assertEquals(dto.hashCode(), dto.hashCode());
        assertEquals(dto, dto);
    }

    @Test
    void testPaymentConfirmationRequest() {
        PaymentConfirmationRequest dto = new PaymentConfirmationRequest();
        dto.setPaymentMethod(PaymentMethod.CASH);
        dto.setAmountReceived(1000.0);
        dto.setStaffNote("Note");
        dto.setConditionBefore("Good");
        dto.setConditionAfter("Fair");
        dto.setBattery(50.0);
        dto.setMileage(2000.0);
        dto.setConfirmPhotos(Collections.singletonList(mockFile));

        assertEquals(PaymentMethod.CASH, dto.getPaymentMethod());
        assertEquals(1000.0, dto.getAmountReceived());
        assertEquals("Note", dto.getStaffNote());
        assertEquals("Good", dto.getConditionBefore());
        assertEquals("Fair", dto.getConditionAfter());
        assertEquals(50.0, dto.getBattery());
        assertEquals(2000.0, dto.getMileage());
        assertEquals(1, dto.getConfirmPhotos().size());

        PaymentConfirmationRequest allArgs = new PaymentConfirmationRequest(PaymentMethod.BANK_TRANSFER, 2000.0, "Note2", "Good2", "Fair2", 40.0, 3000.0, null);
        assertEquals(PaymentMethod.BANK_TRANSFER, allArgs.getPaymentMethod());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testPenaltyCalculationRequest() {
        PenaltyCalculationRequest.SelectedFee selectedFee = new PenaltyCalculationRequest.SelectedFee();
        selectedFee.setFeeId(1L);
        selectedFee.setQuantity(2);
        assertEquals(1L, selectedFee.getFeeId());
        assertEquals(2, selectedFee.getQuantity());
        PenaltyCalculationRequest.SelectedFee allArgsFee = new PenaltyCalculationRequest.SelectedFee(2L, 3);
        assertEquals(2L, allArgsFee.getFeeId());
        assertNotNull(selectedFee.toString());
        assertNotEquals(selectedFee, allArgsFee);
        assertEquals(selectedFee.hashCode(), selectedFee.hashCode());


        PenaltyCalculationRequest.CustomFee customFee = new PenaltyCalculationRequest.CustomFee();
        customFee.setFeeName("Custom Fee");
        customFee.setDescription("Desc");
        customFee.setAmount(100.0);
        customFee.setPhotoFiles(Collections.singletonList(mockFile));
        assertEquals("Custom Fee", customFee.getFeeName());
        assertEquals("Desc", customFee.getDescription());
        assertEquals(100.0, customFee.getAmount());
        assertEquals(1, customFee.getPhotoFiles().size());
        PenaltyCalculationRequest.CustomFee allArgsCustom = new PenaltyCalculationRequest.CustomFee("Custom 2", "Desc 2", 200.0, null);
        assertEquals("Custom 2", allArgsCustom.getFeeName());
        assertNotNull(customFee.toString());
        assertNotEquals(customFee, allArgsCustom);
        assertEquals(customFee.hashCode(), customFee.hashCode());


        PenaltyCalculationRequest dto = new PenaltyCalculationRequest();
        dto.setSelectedFees(Collections.singletonList(selectedFee));
        dto.setCustomFee(customFee);
        assertEquals(1, dto.getSelectedFees().size());
        assertEquals(customFee, dto.getCustomFee());

        PenaltyCalculationRequest allArgs = new PenaltyCalculationRequest(null, null);
        assertNull(allArgs.getCustomFee());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testProfileResponse() {
        ProfileResponse dto = new ProfileResponse();
        dto.setFullName("Name");
        dto.setRole("ROLE");

        assertEquals("Name", dto.getFullName());
        assertEquals("ROLE", dto.getRole());

        ProfileResponse allArgs = new ProfileResponse("Name2", "ROLE2");
        ProfileResponse builder = ProfileResponse.builder().fullName("Name3").build();

        assertEquals("Name2", allArgs.getFullName());
        assertEquals("Name3", builder.getFullName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testRegisterRequest() {
        RegisterRequest dto = new RegisterRequest();
        dto.setFullName("Name");
        dto.setPassword("pass");
        dto.setConfirmPassword("pass");
        dto.setEmail("email");
        dto.setPhone("12345");
        dto.setAgreedToTerms(true);

        assertEquals("Name", dto.getFullName());
        assertEquals("pass", dto.getPassword());
        assertEquals("pass", dto.getConfirmPassword());
        assertEquals("email", dto.getEmail());
        assertEquals("12345", dto.getPhone());
        assertTrue(dto.isAgreedToTerms());

        RegisterRequest allArgs = new RegisterRequest("Name2", "pass2", "pass2", "email2", "54321", false);
        RegisterRequest builder = RegisterRequest.builder().fullName("Name3").build();

        assertEquals("Name2", allArgs.getFullName());
        assertEquals("Name3", builder.getFullName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testReportDamageRequest() {
        ReportDamageRequest dto = new ReportDamageRequest();
        dto.setDescription("Damage");
        dto.setPhotos(Collections.singletonList(mockFile));

        assertEquals("Damage", dto.getDescription());
        assertEquals(1, dto.getPhotos().size());

        ReportDamageRequest allArgs = new ReportDamageRequest("Damage 2", null);
        assertEquals("Damage 2", allArgs.getDescription());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testReportResponse() {
        ReportResponse dto = ReportResponse.builder()
                .stationName("Station")
                .stationId(1L)
                .totalBookingRevenue(1000.0)
                .totalPenaltyRevenue(100.0)
                .totalRevenue(1100.0)
                .fromDate(nowDate)
                .toDate(nowDate.plusDays(1))
                .totalTransactions(10)
                .build();

        dto.setStationName("Station 2");
        assertEquals("Station 2", dto.getStationName());
        assertEquals(1L, dto.getStationId());
        assertEquals(1000.0, dto.getTotalBookingRevenue());
        assertEquals(100.0, dto.getTotalPenaltyRevenue());
        assertEquals(1100.0, dto.getTotalRevenue());
        assertEquals(nowDate, dto.getFromDate());
        assertEquals(nowDate.plusDays(1), dto.getToDate());
        assertEquals(10, dto.getTotalTransactions());

        ReportResponse allArgs = new ReportResponse("Station 3", 2L, 2000.0, 200.0, 2200.0, nowDate, nowDate, 20);
        assertEquals("Station 3", allArgs.getStationName());

        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testRoleUpdateRequest() {
        RoleUpdateRequest dto = new RoleUpdateRequest();
        dto.setRoleId(1L);
        assertEquals(1L, dto.getRoleId());

        RoleUpdateRequest allArgs = new RoleUpdateRequest(2L);
        assertEquals(2L, allArgs.getRoleId());

        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testStationRequest() {
        StationRequest dto = new StationRequest();
        dto.setName("Station");
        dto.setAddress("Address");
        dto.setDescription("Desc");
        dto.setOpeningHours("8-17");
        dto.setHotline("12345");

        assertEquals("Station", dto.getName());
        assertEquals("Address", dto.getAddress());
        assertEquals("Desc", dto.getDescription());
        assertEquals("8-17", dto.getOpeningHours());
        assertEquals("12345", dto.getHotline());

        StationRequest allArgs = new StationRequest("Station2", "Addr2", "Desc2", "9-18", "54321");
        StationRequest builder = StationRequest.builder().name("Station3").build();

        assertEquals("Station2", allArgs.getName());
        assertEquals("Station3", builder.getName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testUpdateModelRequest() {
        UpdateModelRequest dto = new UpdateModelRequest();
        dto.setModelName("Model");
        dto.setVehicleType("CAR");
        dto.setSeatCount(4);
        dto.setBatteryCapacity(50.0);
        dto.setRangeKm(300.0);
        dto.setPricePerHour(100.0);
        dto.setInitialValue(500000.0);
        dto.setFeatures("Features");
        dto.setDescription("Desc");
        dto.setImagePaths(Collections.singletonList("/path/image.png"));

        assertEquals("Model", dto.getModelName());
        assertEquals("CAR", dto.getVehicleType());
        assertEquals(4, dto.getSeatCount());
        assertEquals(50.0, dto.getBatteryCapacity());
        assertEquals(300.0, dto.getRangeKm());
        assertEquals(100.0, dto.getPricePerHour());
        assertEquals(500000.0, dto.getInitialValue());
        assertEquals("Features", dto.getFeatures());
        assertEquals("Desc", dto.getDescription());
        assertEquals(1, dto.getImagePaths().size());

        UpdateModelRequest allArgs = new UpdateModelRequest("Model2", "BIKE", 2, 20.0, 100.0, 50.0, 200000.0, "Feat2", "Desc2", null);
        assertEquals("Model2", allArgs.getModelName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testUpdateProfileRequest() {
        UpdateProfileRequest dto = new UpdateProfileRequest();
        dto.setFullName("Name");
        dto.setEmail("email");
        dto.setPhone("12345");
        dto.setCccd("cccd");
        dto.setGplx("gplx");

        assertEquals("Name", dto.getFullName());
        assertEquals("email", dto.getEmail());
        assertEquals("12345", dto.getPhone());
        assertEquals("cccd", dto.getCccd());
        assertEquals("gplx", dto.getGplx());

        UpdateProfileRequest allArgs = new UpdateProfileRequest("Name2", "email2", "54321", "cccd2", "gplx2");
        UpdateProfileRequest builder = UpdateProfileRequest.builder().fullName("Name3").build();

        assertEquals("Name2", allArgs.getFullName());
        assertEquals("Name3", builder.getFullName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testUpdateStationRequest() {
        UpdateStationRequest dto = new UpdateStationRequest();
        dto.setName("Station");
        dto.setAddress("Address");
        dto.setDescription("Desc");
        dto.setOpeningHours("8-17");
        dto.setHotline("12345");
        dto.setStatus("ACTIVE");

        assertEquals("Station", dto.getName());
        assertEquals("Address", dto.getAddress());
        assertEquals("Desc", dto.getDescription());
        assertEquals("8-17", dto.getOpeningHours());
        assertEquals("12345", dto.getHotline());
        assertEquals("ACTIVE", dto.getStatus());

        UpdateStationRequest allArgs = new UpdateStationRequest("Station2", "Addr2", "Desc2", "9-18", "54321", "INACTIVE");
        assertEquals("Station2", allArgs.getName());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testUpdateVehicleDetailsRequest() {
        UpdateVehicleDetailsRequest dto = new UpdateVehicleDetailsRequest();
        dto.setLicensePlate("123-ABC");
        dto.setModelId(1L);
        dto.setStationId(1L);
        dto.setCurrentMileage(1000.0);
        dto.setBatteryLevel(80);
        dto.setNewCondition(VehicleCondition.GOOD);
        dto.setStatus(VehicleStatus.AVAILABLE);

        assertEquals("123-ABC", dto.getLicensePlate());
        assertEquals(1L, dto.getModelId());
        assertEquals(1L, dto.getStationId());
        assertEquals(1000.0, dto.getCurrentMileage());
        assertEquals(80, dto.getBatteryLevel());
        assertEquals(VehicleCondition.GOOD, dto.getNewCondition());
        assertEquals(VehicleStatus.AVAILABLE, dto.getStatus());

        UpdateVehicleDetailsRequest allArgs = new UpdateVehicleDetailsRequest("321-CBA", 2L, 2L, 2000.0, 70, VehicleCondition.GOOD, VehicleStatus.RENTED);
        assertEquals("321-CBA", allArgs.getLicensePlate());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testUploadVerificationRequest() {
        UploadVerificationRequest dto = new UploadVerificationRequest();
        dto.setCccd("cccd");
        dto.setGplx("gplx");
        dto.setCccdFile1(mockFile);
        dto.setCccdFile2(mockFile);
        dto.setGplxFile1(mockFile);
        dto.setGplxFile2(mockFile);
        dto.setSelfieFile(mockFile);

        assertEquals("cccd", dto.getCccd());
        assertEquals("gplx", dto.getGplx());
        assertEquals(mockFile, dto.getCccdFile1());
        assertEquals(mockFile, dto.getCccdFile2());
        assertEquals(mockFile, dto.getGplxFile1());
        assertEquals(mockFile, dto.getGplxFile2());
        assertEquals(mockFile, dto.getSelfieFile());

        UploadVerificationRequest allArgs = new UploadVerificationRequest("cccd2", "gplx2", null, null, null, null, null);
        assertEquals("cccd2", allArgs.getCccd());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testVehicleHistoryResponse() {
        VehicleHistoryResponse dto = new VehicleHistoryResponse();
        dto.setHistoryId(1L);
        dto.setVehicleType(VehicleType.CAR);
        dto.setLicensePlate("123-ABC");
        dto.setStaffName("Staff");
        dto.setRenterName("Renter");
        dto.setStationName("Station");
        dto.setActionType("DELIVERY");
        dto.setNote("Note");
        dto.setBatteryLevel(80.0);
        dto.setMileage(1000.0);
        dto.setActionTime(now);
        dto.setConditionBefore("Good");
        dto.setConditionAfter("Fair");
        dto.setPhotoPath("/path/photo.png");

        assertEquals(1L, dto.getHistoryId());
        assertEquals(VehicleType.CAR, dto.getVehicleType());
        assertEquals("123-ABC", dto.getLicensePlate());
        assertEquals("Staff", dto.getStaffName());
        assertEquals("Renter", dto.getRenterName());
        assertEquals("Station", dto.getStationName());
        assertEquals("DELIVERY", dto.getActionType());
        assertEquals("Note", dto.getNote());
        assertEquals(80.0, dto.getBatteryLevel());
        assertEquals(1000.0, dto.getMileage());
        assertEquals(now, dto.getActionTime());
        assertEquals("Good", dto.getConditionBefore());
        assertEquals("Fair", dto.getConditionAfter());
        assertEquals("/path/photo.png", dto.getPhotoPath());

        VehicleHistoryResponse allArgs = new VehicleHistoryResponse(2L, VehicleType.CAR, "321-CBA", null, null, null, "RETURN", null, null, null, null, null, null, null);
        VehicleHistoryResponse builder = VehicleHistoryResponse.builder().historyId(3L).build();

        assertEquals(2L, allArgs.getHistoryId());
        assertEquals(3L, builder.getHistoryId());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testVehicleResponse() {
        VehicleResponse dto = new VehicleResponse();
        dto.setVehicleId(1L);
        dto.setLicensePlate("123-ABC");
        dto.setBatteryLevel(80);
        dto.setModelName("Model");
        dto.setStationName("Station");
        dto.setStationId(1L);
        dto.setCurrentMileage(1000.0);
        dto.setStatus("AVAILABLE");
        dto.setCondition("GOOD");
        dto.setReservedByMe(true);
        dto.setRentedByMe(false);
        dto.setVehicleType(VehicleType.CAR);
        dto.setPricePerHour(100.0);
        dto.setSeatCount(4);
        dto.setRangeKm(300.0);
        dto.setFeatures("Features");
        dto.setDescription("Desc");
        dto.setImagePaths(Collections.singletonList("/path/image.png"));
        dto.setCreatedAt(now);

        assertEquals(1L, dto.getVehicleId());
        assertEquals("123-ABC", dto.getLicensePlate());
        assertEquals(80, dto.getBatteryLevel());
        assertEquals("Model", dto.getModelName());
        assertEquals("Station", dto.getStationName());
        assertEquals(1L, dto.getStationId());
        assertEquals(1000.0, dto.getCurrentMileage());
        assertEquals("AVAILABLE", dto.getStatus());
        assertEquals("GOOD", dto.getCondition());
        assertTrue(dto.isReservedByMe());
        assertFalse(dto.isRentedByMe());
        assertEquals(VehicleType.CAR, dto.getVehicleType());
        assertEquals(100.0, dto.getPricePerHour());
        assertEquals(4, dto.getSeatCount());
        assertEquals(300.0, dto.getRangeKm());
        assertEquals("Features", dto.getFeatures());
        assertEquals("Desc", dto.getDescription());
        assertEquals(1, dto.getImagePaths().size());
        assertEquals(now, dto.getCreatedAt());

        VehicleResponse allArgs = new VehicleResponse(2L, "321-CBA", 70, null, null, null, null, null, null, false, false, null, null, null, null, null, null, null, null);
        VehicleResponse builder = VehicleResponse.builder().vehicleId(3L).build();

        assertEquals(2L, allArgs.getVehicleId());
        assertEquals(3L, builder.getVehicleId());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }

    @Test
    void testVerifyRequest() {
        VerifyRequest dto = new VerifyRequest();
        dto.setApproved(true);
        dto.setReason("Reason");

        assertTrue(dto.isApproved());
        assertEquals("Reason", dto.getReason());

        VerifyRequest allArgs = new VerifyRequest(false, "Reason 2");
        VerifyRequest builder = VerifyRequest.builder().approved(true).build();

        assertFalse(allArgs.isApproved());
        assertTrue(builder.isApproved());
        assertNotNull(dto.toString());
        assertNotEquals(dto, allArgs);
        assertEquals(dto.hashCode(), dto.hashCode());
    }
}