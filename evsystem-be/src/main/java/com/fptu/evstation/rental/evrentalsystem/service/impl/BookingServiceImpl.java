package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.ContractRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.UserRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.VehicleRepository;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import com.fptu.evstation.rental.evrentalsystem.service.util.QrCodeService;
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
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final VehicleService vehicleService;
    private final PayOS payOS;
    private final StationService stationService;
    private final ObjectMapper objectMapper;
    private final ContractRepository contractRepository;
    private final ContractService contractService;
    private final Random random = new Random();

    private final Path handoverPhotoDir = Paths.get(System.getProperty("user.dir"), "uploads", "handover_photos");

    private static final long MIN_RENTAL_HOURS = 1;

    @Override
    @Transactional
    public Map<String, Object> createBooking(User renter, BookingRequest req) {

        if (renter.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ trạm.");
        }

        if (!req.isAgreedToTerms()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn phải đồng ý với các điều khoản và điều kiện thuê xe.");
        }

        Duration rentalDuration = Duration.between(req.getStartTime(), req.getEndTime());

        if (rentalDuration.isNegative() || rentalDuration.isZero()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian trả xe phải sau thời gian nhận xe.");
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime oneWeekLater = now.plusDays(2).with(LocalTime.MAX);
        if (req.getStartTime().isBefore(now) || req.getStartTime().isAfter(oneWeekLater)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn chỉ có thể đặt xe trong vòng 2 ngày tới.");
        }


        if (rentalDuration.toHours() < MIN_RENTAL_HOURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời gian thuê tối thiểu là " + MIN_RENTAL_HOURS + " giờ.");
        }

        if (renter.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn chưa được xác minh để có thể đặt xe.");
        }

        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING ,BookingStatus.CONFIRMED, BookingStatus.RENTING);
        long userActiveBookings = bookingRepository.countByUserAndStatusIn(renter, activeStatuses);

        if (userActiveBookings >= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã có một đơn đặt xe đang hoạt động. Vui lòng hoàn thành đơn hiện tại trước khi đặt xe mới.");
        }

        Vehicle vehicle = vehicleService.getVehicleById(req.getVehicleId());
        Station station = vehicle.getStation();
        Model model = vehicle.getModel();

        if (!StationStatus.ACTIVE.equals(station.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạm " + station.getName() + " hiện không hoạt động, không thể đặt xe.");
        }

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xe không khả dụng hoặc đã được thuê.");
        }

        long totalVehiclesOfModel = vehicleRepository.countByModelAndStation(model, station);
        if (totalVehiclesOfModel == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạm này không có loại xe bạn chọn.");
        }

        List<BookingStatus> excludedStatuses = List.of(BookingStatus.CANCELLED, BookingStatus.COMPLETED, BookingStatus.CANCELLED_AWAIT_REFUND, BookingStatus.REFUNDED);
        long conflicts = bookingRepository.countOverlappingBookingsForVehicle(
                vehicle, req.getStartTime(), req.getEndTime(), excludedStatuses
        );

        if (conflicts > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xe này đã bị đặt trong khung giờ bạn chọn. Vui lòng thử lại.");
        }

        double totalRentalCost = model.getPricePerHour() * rentalDuration.toHours();

        Booking booking = Booking.builder()
                .user(renter)
                .station(station)
                .vehicle(vehicle)
                .startDate(req.getStartTime())
                .endDate(req.getEndTime())
                .finalFee(totalRentalCost)
                .downpay(500000.0)
                .status(BookingStatus.PENDING)
                .build();
        booking = bookingRepository.save(booking);

        try {
            final String description = "Thanh toán 500K giữ xe";
            final String returnUrl = "http://localhost:3000/payment-success";
            final String cancelUrl = "http://localhost:3000/payment-failed";
            final long amount = 2000L;
            final long bookingId = booking.getBookingId();

            String randomSuffix = String.format("%04d", random.nextInt(10000));
            final long orderCode = Long.parseLong("1" + bookingId + randomSuffix);

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();
            CreatePaymentLinkResponse paymentResult = payOS.paymentRequests().create(paymentData);

            return Map.of(
                    "message", "Yêu cầu đặt xe thành công. Vui lòng thanh toán cọc 500.000 VNĐ.",
                    "bookingId", booking.getBookingId(),
                    "paymentUrl", paymentResult.getCheckoutUrl()
            );
        } catch (PayOSException e) {
            log.error("Lỗi khi tạo link thanh toán payOS: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo link thanh toán: " + e.getMessage());
        } catch (Exception e_general) {
            log.error("Lỗi hệ thống: {}", e_general.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống không xác định.");
        }
    }

    @Override
    public Booking getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt xe."));
    }

    @Override
    public BookingDetailResponse getBookingDetailsById(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        User renter = booking.getUser();
        Station station = booking.getStation();

        Vehicle vehicle = booking.getVehicle();
        Model model = (vehicle != null) ? vehicle.getModel() : null;

        String licensePlate = (vehicle != null) ? vehicle.getLicensePlate() : "Chưa nhận xe";
        String modelName = (model != null) ? model.getModelName() : "N/A";
        Double pricePerHour = (model != null) ? model.getPricePerHour() : null; // Dùng null hoặc 0.0
        Double initialValue = (model != null) ? model.getInitialValue() : null; // Dùng null hoặc 0.0

        Optional<Contract> contractOpt = contractRepository.findByBooking(booking);
        String contractPath = contractOpt
                .map(Contract::getContractPdfPath)
                .orElse(null);

        List<String> checkInPhotos = List.of();
        if (booking.getCheckInPhotoPaths() != null) {
            try {
                checkInPhotos = objectMapper.readValue(booking.getCheckInPhotoPaths(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Lỗi đọc JSON ảnh check-in: {}", e.getMessage());
            }
        }

        return BookingDetailResponse.builder()
                .bookingId(booking.getBookingId())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .renterName(renter.getFullName())
                .renterPhone(renter.getPhone())
                .renterEmail(renter.getEmail())
                .vehicleLicensePlate(licensePlate)
                .pricePerHour(pricePerHour)
                .modelName(modelName)
                .initialValue(initialValue)
                .stationName(station.getName())
                .stationAddress(station.getAddress())
                .rentalDeposit(booking.getRentalDeposit())
                .reservationDepositPaid(booking.isReservationDepositPaid())
                .finalFee(booking.getFinalFee())
                .checkInPhotoPaths(checkInPhotos)
                .invoicePdfPath(booking.getInvoicePdfPath())
                .contractPdfPath(contractPath)
                .build();
    }

    @Override
    public List<BookingSummaryResponse> getMyBookings(User renter) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Booking> bookings = bookingRepository.findByUserWithDetails(renter, sort);

        return bookings.stream()
                .map(b -> {
                    String licensePlate = "Chưa nhận xe";
                    String modelName = "N/A";
                    VehicleStatus vehicleStatus = null;
                    Integer batteryLevel = null;
                    Double currentMileage = null;

                    Vehicle vehicle = b.getVehicle();

                    if (vehicle != null) {
                        licensePlate = vehicle.getLicensePlate();
                        vehicleStatus = vehicle.getStatus();
                        batteryLevel = vehicle.getBatteryLevel();
                        currentMileage = vehicle.getCurrentMileage();
                        if (vehicle.getModel() != null) {
                            modelName = vehicle.getModel().getModelName();
                        }
                    }

                    return BookingSummaryResponse.builder()
                            .bookingId(b.getBookingId())
                            .renterName(b.getUser().getFullName())
                            .vehicleLicensePlate(licensePlate)
                            .modelName(modelName)
                            .vehicleStatus(vehicleStatus)
                            .bookingStatus(b.getStatus())
                            .batteryLevel(batteryLevel)
                            .currentMileage(currentMileage)
                            .createdAt(b.getCreatedAt())
                            .startDate(b.getStartDate())
                            .build();
                })
                .toList();
    }

    @Override
    public List<BookingSummaryResponse> getAllBookingsByStation(User staff, String keyword, String status, String date) {
        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }
        List<Booking> bookings = bookingRepository.findAllByStationWithDetails(staff.getStation(), Sort.by(Sort.Direction.DESC, "createdAt"));

        Stream<Booking> stream = bookings.stream();

        if (keyword != null && !keyword.isBlank()) {
            String likePattern = keyword.toLowerCase();
            stream = stream.filter(b ->
                    (b.getUser().getFullName() != null && b.getUser().getFullName().toLowerCase().contains(likePattern)) ||
                            (b.getUser().getPhone() != null && b.getUser().getPhone().contains(likePattern))
            );
        }

        if (status != null && !status.isBlank()) {
            try {
                BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
                stream = stream.filter(b -> b.getStatus() == bookingStatus);
            } catch (IllegalArgumentException e) {

            }
        }

        return stream.map(b -> {
                    String licensePlate = "Chưa nhận xe";
                    String modelName = "N/A";
                    VehicleStatus vehicleStatus = null;
                    Integer batteryLevel = null;
                    Double currentMileage = null;

                    Vehicle vehicle = b.getVehicle();

                    if (vehicle != null) {
                        licensePlate = vehicle.getLicensePlate();
                        vehicleStatus = vehicle.getStatus();
                        batteryLevel = vehicle.getBatteryLevel();
                        currentMileage = vehicle.getCurrentMileage();
                        if (vehicle.getModel() != null) {
                            modelName = vehicle.getModel().getModelName();
                        }
                    }

                    return BookingSummaryResponse.builder()
                            .bookingId(b.getBookingId())
                            .renterName(b.getUser().getFullName())
                            .vehicleLicensePlate(licensePlate)
                            .modelName(modelName)
                            .vehicleStatus(vehicleStatus)
                            .bookingStatus(b.getStatus())
                            .batteryLevel(batteryLevel)
                            .currentMileage(currentMileage)
                            .createdAt(b.getCreatedAt())
                            .startDate(b.getStartDate())
                            .build();
                })
                .toList();
    }


    @Override
    public Map<String, Object> initiateCheckIn(Long bookingId, User staff) {
        Booking booking = getBookingById(bookingId);
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }
        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }
        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking này không ở trạng thái sẵn sàng để check-in.");
        }

        Model model = booking.getVehicle().getModel();
        double rentalDepositAmount = model.getInitialValue() * 0.02;

        booking.setRentalDeposit(rentalDepositAmount);
        bookingRepository.save(booking);

        try {
            final String description = "Cọc 2% cho Booking " + booking.getBookingId();
            final String returnUrl = "http://localhost:3000/payment-success";
            final String cancelUrl = "http://localhost:3000/payment-failed";
            final long amount = (long) 2000.0;

            String randomSuffix = String.format("%04d", random.nextInt(10000));
            final long orderCode = Long.parseLong("2" + booking.getBookingId() + randomSuffix);

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            CreatePaymentLinkResponse paymentResult = payOS.paymentRequests().create(paymentData);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đã tạo link thanh toán cọc thuê xe 2%. Vui lòng đưa khách thanh toán.");
            response.put("rentalDepositAmount", rentalDepositAmount);
            response.put("paymentUrl", paymentResult.getCheckoutUrl());

            return response;

        } catch (PayOSException e) {
            log.error("Lỗi khi tạo link thanh toán cọc 2% (PayOS): {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo link thanh toán cọc 2%: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String cancelBookingByRenter(User renter, Long bookingId, CancelBookingRequest req) {
        Booking booking = getBookingById(bookingId);

        if (renter.getStatus() == AccountStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ trạm.");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy booking ở trạng thái " + booking.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (booking.getStartDate().isBefore(now.plusHours(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy booking quá sát giờ nhận xe (trong vòng 2 giờ). Vui lòng liên hệ trạm.");
        }

        int newCount = renter.getCancellationCount() + 1;
        renter.setCancellationCount(newCount);
        String penaltyMessage = "";

        if (newCount >= 3) {
            renter.setStatus(AccountStatus.INACTIVE);
            penaltyMessage = "TÀI KHOẢN BỊ KHÓA do hủy 3 lần.";
        } else if (newCount == 2) {
            penaltyMessage = "CẢNH BÁO: Hủy thêm 1 lần nữa, tài khoản sẽ bị khóa.";
        } else {
            penaltyMessage = "Bạn còn " + (2 - newCount) + " lần hủy an toàn.";
        }
        userRepository.save(renter);

        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);
            return "Đã hủy đơn (chưa thanh toán) thành công. " + penaltyMessage;
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            Vehicle vehicle = booking.getVehicle();

            if (vehicle != null && vehicle.getStatus() == VehicleStatus.RESERVED) {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleService.saveVehicle(vehicle);
                log.info("Đã trả xe (ID: {}) về AVAILABLE do booking (ID: {}) bị hủy.", vehicle.getVehicleId(), booking.getBookingId());
            } else {
                log.warn("Booking (ID: {}) bị hủy nhưng xe (ID: {}) không ở trạng thái RESERVED.",
                        booking.getBookingId(), (vehicle != null ? vehicle.getVehicleId() : "null"));
            }

            LocalDateTime refundCutoff = booking.getCreatedAt().plusHours(12);
            if (now.isBefore(refundCutoff)) {
                if (req == null || req.getBankName() == null || req.getAccountNumber() == null || req.getAccountName() == null ||
                        req.getBankName().isBlank() || req.getAccountNumber().isBlank() || req.getAccountName().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng cung cấp đầy đủ thông tin tài khoản ngân hàng để nhận hoàn cọc.");
                }

                booking.setStatus(BookingStatus.CANCELLED_AWAIT_REFUND);
                booking.setRefund(500000.0);
                String refundNote = String.format("Ngân hàng: %s, STK: %s, Chủ TK: %s",
                        req.getBankName(), req.getAccountNumber(), req.getAccountName());
                booking.setRefundNote(refundNote);

                bookingRepository.save(booking);
                return "Đã hủy đơn thành công. " + penaltyMessage + " Yêu cầu hoàn cọc 500k về tài khoản (" + req.getAccountNumber() + ") đã được ghi nhận và đang chờ xử lý.";

            } else {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);

                return "Bạn đã hủy đơn quá 12 giờ kể từ khi đặt, bạn sẽ bị mất cọc. " + penaltyMessage;
            }
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy booking ở trạng thái " + booking.getStatus());
    }

    @Override
    public List<BookingSummaryResponse> getPendingRefundsByStation(User staff) {
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        List<Booking> bookings = bookingRepository.findAllByStationWithDetails(
                staff.getStation(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED_AWAIT_REFUND)
                .map(b -> BookingSummaryResponse.builder()
                        .bookingId(b.getBookingId())
                        .renterName(b.getUser().getFullName())
                        .renterPhone(b.getUser().getPhone())
                        .vehicleLicensePlate(b.getVehicle() != null ? b.getVehicle().getLicensePlate() : "N/A")
                        .modelName(b.getVehicle() != null ? b.getVehicle().getModel().getModelName() : "N/A")
                        .bookingStatus(b.getStatus())
                        .refundAmount(b.getRefund())
                        .refundInfo(b.getRefundNote())
                        .createdAt(b.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void confirmRefund(User staff, Long bookingId) {
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        Booking booking = getBookingById(bookingId);

        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        if (booking.getStatus() != BookingStatus.CANCELLED_AWAIT_REFUND) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking này không ở trạng thái chờ hoàn tiền.");
        }

        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        paymentService.createTransaction(
                booking,
                -500000.0,
                PaymentMethod.BANK_TRANSFER,
                staff,
                "Nhân viên xác nhận hoàn cọc 500k (thủ công)"
        );
        log.info("Nhân viên {} đã xác nhận hoàn cọc 500k cho booking {}", staff.getFullName(), bookingId);
    }

    @Override
    @Transactional
    public void cancelBookingByStaff(Long bookingId, User staff) {
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        Booking booking = getBookingById(bookingId);

        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy booking ở trạng thái " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    public Contract processCheckIn(Long bookingId, CheckInRequest req, User staff) {
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt xe."));

        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        if (booking.getStatus() == BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đặt xe này chưa được xác nhận thanh toán phí giữ chỗ.");
        }else if(booking.getStatus() == BookingStatus.RENTING){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đặt xe này đã được thuê.");
        }else if(booking.getStatus() == BookingStatus.COMPLETED){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đặt xe này đã được hoàn thành.");
        }else if(booking.getStatus() == BookingStatus.CANCELLED){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đặt xe này đã bị hủy.");
        }


        Vehicle vehicle = booking.getVehicle();
        vehicle.setStatus(VehicleStatus.RENTED);
        vehicleService.saveVehicle(vehicle);

        if (req.getCheckInPhotos() == null || req.getCheckInPhotos().isEmpty() || req.getCheckInPhotos().get(0).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh check-in (lúc giao xe) là bắt buộc.");
        }


        double rentalDepositAmount = booking.getVehicle().getModel().getInitialValue() * 0.02;
        if (!booking.isRentalDepositPaid()) {
            log.info("Booking {} chưa trả cọc 2% qua PayOS. Xử lý thanh toán thủ công...", bookingId);

            if (req.getDepositPaymentMethod() == PaymentMethod.GATEWAY) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thanh toán qua cổng PayOS chưa được xác nhận. Vui lòng đợi hoặc chọn phương thức thanh toán thủ công (Tiền mặt/Chuyển khoản).");
            }

            paymentService.createTransaction(booking, rentalDepositAmount, req.getDepositPaymentMethod(), staff, "Thu cọc thuê xe 2% (thủ công)");
            booking.setRentalDepositPaid(true);

        } else {
            log.info("Booking {} đã trả cọc 2% qua PayOS. Bỏ qua tạo giao dịch 2%...", bookingId);
        }

        List<String> photoPaths = new ArrayList<>();
        String photoPathsJson = null;
        for (int i = 0; i < req.getCheckInPhotos().size(); i++) {
            MultipartFile photo = req.getCheckInPhotos().get(i);
            String savedPath = saveHandoverPhoto(photo, bookingId, "checkin", i + 1);
            photoPaths.add(savedPath);
        }

        try {
            photoPathsJson = objectMapper.writeValueAsString(photoPaths);
            booking.setCheckInPhotoPaths(photoPathsJson);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi danh sách ảnh thành JSON", e);
        }

        booking.setRentalDeposit(rentalDepositAmount);
        booking.setStartDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.RENTING);
        bookingRepository.save(booking);

        Contract contract = contractService.generateAndSaveContract(booking, staff);
        try {
            vehicleService.recordVehicleAction(
                    booking.getVehicle().getVehicleId(),
                    staff.getUserId(),
                    booking.getUser().getUserId(),
                    staff.getStation().getStationId(),
                    VehicleActionType.DELIVERY,
                    "Nhân viên " + staff.getFullName() + " đã giao xe cho khách " + booking.getUser().getFullName(),
                    req.getConditionBefore(),
                    null,
                    req.getBattery(),
                    req.getMileage(),
                    photoPathsJson
            );
        } catch (Exception e) {
            log.warn("Không thể ghi lịch sử giao xe cho booking ID {}: {}", bookingId, e.getMessage());
        }
        log.info("Check-in thành công cho Booking ID: {}", bookingId);
        return contract;
    }

    @Transactional
    public String saveHandoverPhoto(MultipartFile file, Long bookingId, String type, int index) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh bàn giao không được để trống.");
        }
        try {
            Path bookingDir = handoverPhotoDir.resolve("booking_" + bookingId);
            Files.createDirectories(bookingDir);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }
            String fileName = String.format("%s-%d%s", type, index, extension);
            Path filePath = bookingDir.resolve(fileName);
            file.transferTo(filePath);

            return "/uploads/handover_photos/booking_" + bookingId + "/" + fileName;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi lưu ảnh.");
        }
    }

    public Map<String, Object> getPeakHourStatistics(Long stationId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime to = (toDate != null) ? toDate.atTime(23, 59, 59) : LocalDate.now().atTime(23, 59, 59);

        List<Booking> bookings;

        if (stationId != null) {
            Station station = stationService.getStationById(stationId);
            bookings = bookingRepository.findAllByStationAndStartDateBetween(station, from, to);
        } else {
            bookings = bookingRepository.findAllByStartDateBetween(from, to);
        }

        Map<Integer, Long> countByHour = new HashMap<>();
        for (Booking b : bookings) {
            int hour = b.getStartDate().getHour();
            countByHour.put(hour, countByHour.getOrDefault(hour, 0L) + 1);
        }

        long total = countByHour.values().stream().mapToLong(Long::longValue).sum();
        int peakHour = countByHour.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        List<Map<String, Object>> hourlyStats = new ArrayList<>();
        long peakValue = countByHour.getOrDefault(peakHour, 0L);

        for (int i = 0; i < 24; i++) {
            long rented = countByHour.getOrDefault(i, 0L);
            double percent = (peakValue == 0) ? 0 : (rented * 100.0 / peakValue);

            Map<String, Object> item = new HashMap<>();
            item.put("hourRange", String.format("%02d:00 - %02d:00", i, (i + 1) % 24));
            item.put("rentedVehicles", rented);
            item.put("percentOfPeak", percent);
            hourlyStats.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("scope", stationId == null ? "Tất cả các trạm" : "Trạm ID " + stationId);
        result.put("fromDate", from.toLocalDate());
        result.put("toDate", to.toLocalDate());
        result.put("totalRentals", total);
        result.put("peakHour", peakHour == -1 ? "Không có dữ liệu" :
                String.format("%02d:00 - %02d:00", peakHour, (peakHour + 1) % 24));
        result.put("data", hourlyStats);

        return result;
    }

}
