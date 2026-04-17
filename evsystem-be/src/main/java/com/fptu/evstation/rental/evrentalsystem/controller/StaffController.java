package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fptu.evstation.rental.evrentalsystem.dto.*;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.Contract;
import com.fptu.evstation.rental.evrentalsystem.entity.PenaltyFee;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.PenaltyFeeRepository;
import com.fptu.evstation.rental.evrentalsystem.service.*;
import com.fptu.evstation.rental.evrentalsystem.service.impl.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {
    private final AuthService authService;
    private final UserServiceImpl userService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final DashboardService dashboardService;
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper;
    private final BookingService bookingService;
    private final ContractService contractService;

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingSummaryResponse>> getAllBookings(
                                    @RequestHeader("Authorization") String authHeader,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String date) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        return ResponseEntity.ok(bookingService.getAllBookingsByStation(staff, keyword, status, date));
    }

    @GetMapping("/refund-requests")
    public ResponseEntity<List<BookingSummaryResponse>> getRefundRequests(
            @RequestHeader("Authorization") String authHeader) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        List<BookingSummaryResponse> list = bookingService.getPendingRefundsByStation(staff);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/bookings/{bookingId}/confirm-refund")
    public ResponseEntity<?> confirmRefund(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId) {

        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        bookingService.confirmRefund(staff, bookingId);

        return ResponseEntity.ok(Map.of("message", "Đã xác nhận hoàn tiền thành công. Khách hàng sẽ thấy trạng thái 'Đã hoàn tiền'."));
    }

    @PostMapping("/bookings/{bookingId}/confirm-deposit")
    public ResponseEntity<?> confirmDeposit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId) {

        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));

        paymentService.confirmDeposit(staff, bookingId);

        return ResponseEntity.ok(Map.of(
                "message", "Xác nhận cọc 500k thành công. Xe đã được giữ chỗ."
        ));
    }

    @PostMapping("/rentals/initiate-check-in/{bookingId}")
    public ResponseEntity<?> initiateCheckIn(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        Map<String, Object> result = bookingService.initiateCheckIn(bookingId, staff);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/rentals/check-in/{bookingId}")
    public ResponseEntity<?> checkIn(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId,
            @Valid @ModelAttribute CheckInRequest req) {
        String token = authService.getTokenFromHeader(authHeader);
        User staff = authService.validateTokenAndGetUser(token);

        Contract contract = bookingService.processCheckIn(bookingId, req, staff);

        return ResponseEntity.ok(Map.of(
                "message", "Check-in thành công. Đã thu cọc thuê xe và tạo hợp đồng.",
                "contractUrl", contract.getContractPdfPath()
        ));
    }

    @GetMapping("/contracts")
    public ResponseEntity<List<ContractSummaryResponse>> getAllContracts(@RequestHeader("Authorization") String authHeader) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        return ResponseEntity.ok(contractService.getAllContractsByStation(staff));
    }

    @GetMapping("/verifications/pending")
    public ResponseEntity<List<User>> getPendingVerifications(@RequestHeader("Authorization") String authHeader) {
        authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        return ResponseEntity.ok(userService.getPendingVerifications());
    }

    @PostMapping("/verifications/{userId}/process")
    public ResponseEntity<?> verifyUser(@RequestHeader("Authorization") String authHeader, @PathVariable Long userId, @RequestBody VerifyRequest req) {
        authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        String message = userService.processVerification(userId, req);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId) {

        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        Booking booking = bookingService.getBookingById(bookingId);

        if (!booking.getStation().getStationId().equals(staff.getStation().getStationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thao tác trên đơn hàng của trạm khác.");
        }

        bookingService.cancelBookingByStaff(bookingId, staff);
        return ResponseEntity.ok(Map.of("message", "Nhân viên đã hủy booking thành công."));
    }

    @PostMapping(value = "/bookings/{bookingId}/calculate-bill", consumes = { "multipart/form-data" })
    public ResponseEntity<BillResponse> calculateFinalBill(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId,
            @RequestPart(value = "selectedFeesJson", required = false) String selectedFeesJson,
            @ModelAttribute PenaltyCalculationRequest request) {

        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        if (selectedFeesJson != null && !selectedFeesJson.isBlank()) {
            try {
                List<PenaltyCalculationRequest.SelectedFee> selectedFees = objectMapper.readValue(selectedFeesJson, new TypeReference<>() {});
                request.setSelectedFees(selectedFees);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng của selectedFees không hợp lệ.");
            }
        }

        BillResponse bill = paymentService.calculateFinalBill(staff, bookingId, request);
        return ResponseEntity.ok(bill);
    }

    @PostMapping(value = "/bookings/{bookingId}/confirm-payment", consumes = { "multipart/form-data" })
    public ResponseEntity<?> confirmPayment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long bookingId,
            @Valid @ModelAttribute PaymentConfirmationRequest req) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));

        Map<String, Object> result = paymentService.confirmFinalPayment(bookingId, req, staff);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/penalty-fees")
    public ResponseEntity<List<PenaltyFee>> getAllPenaltyFees(@RequestHeader("Authorization") String authHeader) {
        authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        List<PenaltyFee> fees = paymentService.getAllPenaltyFees();
        return ResponseEntity.ok(fees);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<InvoiceSummaryResponse>> getAllInvoices(@RequestHeader("Authorization") String authHeader) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        List<InvoiceSummaryResponse> invoices = invoiceService.getAllInvoicesByStation(staff);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/my-station/vehicles")
    public ResponseEntity<List<VehicleResponse>> getVehiclesForStaffStation(@RequestHeader("Authorization") String authHeader) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chưa được gán cho trạm nào.");
        }
        List<VehicleResponse> vehicles = vehicleService.getAllVehiclesByStation(staff.getStation());
        return ResponseEntity.ok(vehicles);
    }

    @PutMapping("/vehicles/{vehicleId}/details")
    public ResponseEntity<?> updateVehicleDetails(@RequestHeader("Authorization") String authHeader, @PathVariable Long vehicleId, @Valid @RequestBody UpdateVehicleDetailsRequest req) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        vehicleService.updateVehicleDetails(staff, vehicleId, req);
        return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin xe thành công."));
    }

    @PostMapping("/vehicles/{vehicleId}/report-damage")
    public ResponseEntity<?> reportMajorDamage(@RequestHeader("Authorization") String authHeader, @PathVariable Long vehicleId, @Valid @ModelAttribute ReportDamageRequest req) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        vehicleService.reportMajorDamage(staff, vehicleId, req);
        return ResponseEntity.ok(Map.of("message", "Báo cáo hư hỏng đã được ghi nhận. Xe đã được chuyển vào trạng thái bảo trì."));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(@RequestHeader("Authorization") String authHeader) {
        User staff = authService.validateTokenAndGetUser(authService.getTokenFromHeader(authHeader));
        return ResponseEntity.ok(dashboardService.getSummaryForStaff(staff));
    }
}
