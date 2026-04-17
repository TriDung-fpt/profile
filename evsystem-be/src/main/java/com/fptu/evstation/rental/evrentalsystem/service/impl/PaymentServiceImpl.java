package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.*;
import com.fptu.evstation.rental.evrentalsystem.service.InvoiceService;
import com.fptu.evstation.rental.evrentalsystem.service.PaymentService;
import com.fptu.evstation.rental.evrentalsystem.service.VehicleService;
import com.fptu.evstation.rental.evrentalsystem.service.util.QrCodeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final PenaltyFeeRepository penaltyFeeRepository;
    private final TransactionDetailRepository transactionDetailRepository;
    private final TransactionRepository transactionRepository;
    private final VehicleHistoryRepository historyRepository;
    private final VehicleService vehicleService;
    private final QrCodeService qrCodeService;
    private final InvoiceService invoiceService;

    private final Path handoverPhotoDir = Paths.get(System.getProperty("user.dir"), "uploads", "handover_photos");
    private final Path adjustmentPhotoDir = Paths.get(System.getProperty("user.dir"), "uploads", "adjustments");
    private final ObjectMapper objectMapper;

    @Transactional
    public BillResponse calculateFinalBill(User staff, Long bookingId, PenaltyCalculationRequest req) {
        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Booking"));

        List<TransactionDetail> oldDetails = transactionDetailRepository.findByBooking(booking);
        for (TransactionDetail oldDetail : oldDetails) {
            if (oldDetail.getPenaltyFee() != null &&
                    oldDetail.getPenaltyFee().getIsAdjustment() != null &&
                    oldDetail.getPenaltyFee().getIsAdjustment() &&
                    oldDetail.getPhotoPaths() != null) {

                try {
                    List<String> oldPhotoPaths = objectMapper.readValue(oldDetail.getPhotoPaths(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                    for (String oldPath : oldPhotoPaths) {
                        if (oldPath == null || oldPath.isBlank()) continue;
                        Path oldPhotoFile = Paths.get(System.getProperty("user.dir"), oldPath.substring(1));
                        Files.deleteIfExists(oldPhotoFile);
                    }
                } catch (Exception e) {
                    log.warn("Không thể xóa ảnh adjustment cũ cho booking {}: {}", bookingId, e.getMessage());
                }
            }
        }

        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        if (booking.getStatus() != BookingStatus.RENTING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể tính phí khi đơn đang ở trạng thái RENTING.");
        }

        LocalDateTime actualEndDate = LocalDateTime.now();
        Vehicle vehicle = booking.getVehicle();
        Model model = vehicle.getModel();

        Duration actualDuration = Duration.between(booking.getStartDate(), actualEndDate);
        double actualHours = actualDuration.toMinutes() / 60.0;
        double totalBaseFee = model.getPricePerHour() * actualHours;

        double lateFee = 0.0;
        if (actualEndDate.isAfter(booking.getEndDate())) {
            Duration lateDuration = Duration.between(booking.getEndDate(), actualEndDate);
            double lateHours = lateDuration.toMinutes() / 60.0;
            double lateFeePerHour = 100_000;
            lateFee = lateHours * lateFeePerHour;
        }
        double finalBaseFee = Math.round(totalBaseFee + lateFee);

        transactionDetailRepository.deleteByBooking(booking);
        double totalPositivePenalty = 0.0;
        double totalDiscount = 0.0;

        List<BillResponse.FeeItem> feeItemsForBill = new ArrayList<>();
        if (req.getSelectedFees() != null && !req.getSelectedFees().isEmpty()) {
            for (PenaltyCalculationRequest.SelectedFee selected : req.getSelectedFees()) {
                PenaltyFee feeType = penaltyFeeRepository.findById(selected.getFeeId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy loại phí với ID: " + selected.getFeeId()));
                double appliedAmount = feeType.getFixedAmount() * selected.getQuantity();
                totalPositivePenalty += appliedAmount;

                TransactionDetail detail = TransactionDetail.builder()
                        .booking(booking)
                        .penaltyFee(feeType)
                        .appliedAmount(appliedAmount)
                        .staffNote(feeType.getFeeName() + " (Số lượng: " + selected.getQuantity() + ")")
                        .build();
                transactionDetailRepository.save(detail);

                feeItemsForBill.add(BillResponse.FeeItem.builder()
                        .feeName(detail.getStaffNote())
                        .amount(appliedAmount)
                        .staffNote(staff.getFullName() + " (Staff) đã áp dụng.")
                        .build());
            }
        }

        if (req.getCustomFee() != null && req.getCustomFee().getAmount() != null && req.getCustomFee().getAmount() != 0) {
            PenaltyCalculationRequest.CustomFee custom = req.getCustomFee();

            if (custom.getAmount() < 0) {
                totalDiscount += Math.abs(custom.getAmount());
            } else {
                totalPositivePenalty += custom.getAmount();
            }

            List<String> photoPaths = new ArrayList<>();
            if (req.getCustomFee().getPhotoFiles() != null && !req.getCustomFee().getPhotoFiles().isEmpty()) {
                for (int i = 0; i < req.getCustomFee().getPhotoFiles().size(); i++) {
                    MultipartFile photo = req.getCustomFee().getPhotoFiles().get(i);
                    String savedPath = saveAdjustmentPhoto(photo, bookingId, i + 1);
                    photoPaths.add(savedPath);
                }
            }

            String photoPathsJson = null;
            try {
                photoPathsJson = objectMapper.writeValueAsString(photoPaths);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi xử lý ảnh phạt.");
            }


            PenaltyFee adjustmentFeeType = penaltyFeeRepository.findByIsAdjustmentTrue()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Thiếu cấu hình phí tùy chỉnh trong DB."));

            TransactionDetail detail = TransactionDetail.builder()
                    .booking(booking)
                    .penaltyFee(adjustmentFeeType)
                    .appliedAmount(custom.getAmount())
                    .staffNote(custom.getFeeName())
                    .adjustmentNote(custom.getDescription())
                    .photoPaths(photoPathsJson)
                    .build();
            transactionDetailRepository.save(detail);

            feeItemsForBill.add(BillResponse.FeeItem.builder()
                    .feeName(custom.getFeeName())
                    .amount(custom.getAmount())
                    .staffNote(staff.getFullName() + " (Staff) đã thêm phí: " + custom.getFeeName())
                    .adjustmentNote(custom.getDescription())
                    .build());
        }

        double totalDebit = finalBaseFee + totalPositivePenalty;

        double downpayPaid_2_percent = booking.getRentalDeposit() != null ? booking.getRentalDeposit() : 0;
        double downpayPaid_500k = booking.isReservationDepositPaid() ? 500000.0 : 0.0;
        double totalDownpayPaid = downpayPaid_2_percent + downpayPaid_500k;

        double totalCredit = totalDownpayPaid + totalDiscount;

        double netSettlement = totalDebit - totalCredit;

        booking.setFinalFee(totalDebit);
        bookingRepository.save(booking);

        double paymentDue = Math.max(0, netSettlement);
        double refundToCustomer = Math.max(0, -netSettlement);

        String qrCodeBase64 = (paymentDue > 0) ? qrCodeService.generateQrCodeBase64(paymentDue, bookingId) : null;

        BillResponse billResponse = BillResponse.builder()
                .bookingId(bookingId)
                .userName(booking.getUser().getFullName())
                .dateTime(actualEndDate)
                .baseRentalFee(finalBaseFee)
                .totalPenaltyFee(totalPositivePenalty)
                .downpayPaid(totalDownpayPaid)
                .totalDiscount(totalDiscount)
                .paymentDue(paymentDue)
                .refundToCustomer(refundToCustomer)
                .feeItems(feeItemsForBill)
                .qrCodeUrl(qrCodeBase64)
                .build();

        String invoicePath = invoiceService.generateAndSaveInvoicePdf(billResponse);
        billResponse.setInvoicePdfPath(invoicePath);
        return billResponse;
    }

    @Override
    @Transactional
    public void confirmDeposit(User staff, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Booking."));

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn chưa được gán cho trạm nào.");
        }
        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn này đã được xác nhận cọc trước đó (có thể qua PayOS).");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể xác nhận cọc cho đơn ở trạng thái PENDING.");
        }

        Booking confirmedBooking = confirmDepositLogic(bookingId);
        createTransaction(
                confirmedBooking,
                500_000.0,
                PaymentMethod.BANK_TRANSFER,
                staff,
                "Nhân viên xác nhận cọc giữ chỗ 500k"
        );
    }

    @Override
    @Transactional
    public void autoConfirmDeposit(Long bookingId) {
        Booking before = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Booking."));
        BookingStatus beforeStatus = before.getStatus();
        boolean beforePaid = before.isReservationDepositPaid();

        Booking after = confirmDepositLogic(bookingId);

        if (beforeStatus == BookingStatus.PENDING
                && after.getStatus() == BookingStatus.CONFIRMED
                && !beforePaid
                && after.isReservationDepositPaid()) {
            createTransaction(
                    after,
                    500_000.0,
                    PaymentMethod.GATEWAY,
                    null,
                    "Hệ thống tự động xác nhận cọc 500k (payOS)"
            );
        }
    }

    @Override
    @Transactional
    public void autoConfirmRentalDeposit(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook 2%: Không tìm thấy Booking."));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("Webhook 2% nhận được cho booking {} nhưng trạng thái không phải CONFIRMED (status={}). Bỏ qua.", bookingId, booking.getStatus());
            return;
        }

        if (booking.isRentalDepositPaid()) {
            log.warn("Webhook 2% nhận được cho booking {} nhưng cọc 2% đã được thanh toán trước đó. Bỏ qua.", bookingId);
            return;
        }

        if (booking.getRentalDeposit() == null || booking.getRentalDeposit() <= 0) {
            log.error("Webhook 2% cho booking {} thất bại: không tìm thấy số tiền cọc 2% (RentalDeposit).", bookingId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi logic: Không tìm thấy số tiền cọc 2%");
        }

        booking.setRentalDepositPaid(true);
        bookingRepository.save(booking);

        createTransaction(
                booking,
                booking.getRentalDeposit(),
                PaymentMethod.GATEWAY,
                null,
                "Hệ thống tự động xác nhận cọc thuê xe 2% (payOS)"
        );

        log.info("Hệ thống tự động xác nhận cọc 2% (PayOS) thành công cho Booking ID: {}", bookingId);
    }

    @Override
    @Transactional
    public Map<String, Object> confirmFinalPayment(Long bookingId, PaymentConfirmationRequest req, User staff) {
        if (staff.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản nhân viên của bạn đã bị khóa.");
        }

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Booking"));

        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        if (req.getConfirmPhotos() == null || req.getConfirmPhotos().isEmpty() || req.getConfirmPhotos().get(0).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ảnh check-out (lúc trả xe) là bắt buộc.");
        }

        if (booking.getStatus() != BookingStatus.RENTING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn này không ở trạng thái chờ thanh toán.");
        }

        VehicleHistory checkInHistory = historyRepository
                .findFirstByVehicleAndRenterAndActionTypeOrderByActionTimeDesc(
                        booking.getVehicle(),
                        booking.getUser(),
                        VehicleActionType.DELIVERY
                );

        String conditionAtCheckIn;
        if (checkInHistory != null) {
            conditionAtCheckIn = checkInHistory.getConditionBefore();
        } else {
            conditionAtCheckIn = "Không rõ (Lỗi không tìm thấy lịch sử check-in)";
            log.warn("Không tìm thấy lịch sử check-in (DELIVERY) cho Booking ID: {}", bookingId);
        }

        List<String> photoPaths = new ArrayList<>();
        for (int i = 0; i < req.getConfirmPhotos().size(); i++) {
            MultipartFile photo = req.getConfirmPhotos().get(i);

            photoPaths.add(saveHandoverPhoto(photo, bookingId, "checkout", i + 1));
        }
        String photoPathsJson = null;
        try {
            photoPathsJson = objectMapper.writeValueAsString(photoPaths);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển ảnh sang JSON", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi xử lý ảnh check-out.");
        }

        double totalDue = booking.getFinalFee() != null ? booking.getFinalFee() : 0.0;

        double depositPaid_2_percent = booking.getRentalDeposit() != null ? booking.getRentalDeposit() : 0.0;
        double depositPaid_500k = booking.isReservationDepositPaid() ? 500000.0 : 0.0;
        double totalDepositPaid = depositPaid_2_percent + depositPaid_500k;

        Double totalDiscount = transactionDetailRepository.findTotalDiscountByBooking(booking);
        if (totalDiscount == null) totalDiscount = 0.0;

        double totalCredit = totalDepositPaid + Math.abs(totalDiscount);

        double netSettlement = totalDue - totalCredit;

        Vehicle vehicle = booking.getVehicle();
        if (req.getBattery() != null) vehicle.setBatteryLevel(req.getBattery().intValue());
        if (req.getMileage() != null) vehicle.setCurrentMileage(req.getMileage());
        vehicle.setStatus(VehicleStatus.UNAVAILABLE);

        double actualRefundAmount = Math.max(0, -netSettlement);
        booking.setRefund(actualRefundAmount);
        completeBooking(booking);
        vehicleService.saveVehicle(vehicle);

        if (netSettlement != 0) {
            String finalStaffNote;

            if (netSettlement > 0) {
                finalStaffNote = String.format("Quyết toán cuối (Khách trả thêm): %,.0f VNĐ. %s",
                        netSettlement, req.getStaffNote());
            } else {
                finalStaffNote = String.format("Quyết toán cuối (Hoàn tiền cho khách): %,.0f VNĐ. %s",
                        Math.abs(netSettlement), req.getStaffNote());
            }

            Transaction finalTransaction = Transaction.builder()
                    .booking(booking)
                    .amount(netSettlement)
                    .paymentMethod(req.getPaymentMethod())
                    .transactionDate(LocalDateTime.now())
                    .staff(staff)
                    .staffNote(finalStaffNote)
                    .build();

            transactionRepository.save(finalTransaction);
        }

        try {
            vehicleService.recordVehicleAction(
                    vehicle.getVehicleId(),
                    staff.getUserId(),
                    booking.getUser().getUserId(),
                    vehicle.getStation().getStationId(),
                    VehicleActionType.RETURN,
                    "Khách hàng " + booking.getUser().getFullName() + " đã trả xe tại trạm "
                            + vehicle.getStation().getName(),
                    conditionAtCheckIn,
                    req.getConditionAfter(),
                    req.getBattery(),
                    req.getMileage(),
                    photoPathsJson
            );
        } catch (Exception e) {
            log.warn("Không thể ghi lịch sử trả xe cho Booking ID {}: {}", bookingId, e.getMessage());
        }

        log.info("Nhân viên {} đã xác nhận thanh toán và hoàn tất Booking ID: {}", staff.getFullName(), bookingId);

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        nf.setMaximumFractionDigits(0);

        double amountAbs = Math.abs(netSettlement);
        String amountStr = nf.format(amountAbs);

        String message;
        if (netSettlement > 0) {
            message = "Xác nhận thanh toán thêm " + amountStr + " VNĐ thành công. Đơn hàng đã hoàn tất.";
        } else if (netSettlement < 0) {
            message = "Xác nhận hoàn trả cho khách " + amountStr + " VNĐ thành công. Đơn hàng đã hoàn tất.";
        } else {
            message = "Xác nhận quyết toán thành công (không phát sinh). Đơn hàng đã hoàn tất.";
        }

        return Map.of("message", message);
    }

    private void completeBooking(Booking booking) {
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return;
        }
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);
    }

    @Transactional
    protected String saveAdjustmentPhoto(MultipartFile file, Long bookingId, int index) {
        if (file == null || file.isEmpty()) return null;
        try {
            Path bookingDir = adjustmentPhotoDir.resolve("booking_" + bookingId);
            Files.createDirectories(bookingDir);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }

            String fileName = String.format("adjustment-%d%s", index, extension);
            Path filePath = bookingDir.resolve(fileName);
            file.transferTo(filePath);

            return "/uploads/adjustments/booking_" + bookingId + "/" + fileName;

        } catch (IOException e) {
            log.error("Lỗi khi lưu ảnh adjustment", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi lưu file ảnh.");
        }
    }

    private Booking confirmDepositLogic(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Booking."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            if (booking.getStatus() == BookingStatus.CONFIRMED) {
                log.warn("Booking {} đã được xác nhận trước đó (có thể do webhook gọi lại).", bookingId);
                return booking;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking này không ở trạng thái chờ thanh toán cọc.");
        }

        Vehicle vehicle = booking.getVehicle();
        if (vehicle == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Booking này bị lỗi: không tìm thấy thông tin xe.");
        }

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            log.warn("Xe {} đã bị đặt bởi một giao dịch khác.", vehicle.getVehicleId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Xe này vừa được người khác đặt. Vui lòng hủy booking và tạo lại.");
        }

        vehicle.setStatus(VehicleStatus.RESERVED);
        vehicleService.saveVehicle(vehicle);

        booking.setReservationDepositPaid(true);
        booking.setStatus(BookingStatus.CONFIRMED);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public void createTransaction(Booking booking, Double amount, PaymentMethod paymentMethod, User staff, String note) {
        Transaction transaction = Transaction.builder()
                .booking(booking)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .transactionDate(LocalDateTime.now())
                .staff(staff)
                .staffNote(note)
                .build();
        transactionRepository.save(transaction);
    }

    @Override
    public List<PenaltyFee> getAllPenaltyFees() {
        return penaltyFeeRepository.findAll().stream()
                .filter(fee -> fee.getIsAdjustment() == null || !fee.getIsAdjustment())
                .toList();
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
}
