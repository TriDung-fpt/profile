package com.fptu.evstation.rental.evrentalsystem.controller;

import com.fptu.evstation.rental.evrentalsystem.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    private final PayOS payOS;
    private final PaymentService paymentService;
    private static final int SUFFIX_LENGTH = 4; // 4 số 0 ngẫu nhiên

    @PostMapping(value = "/webhook", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleWebhook(@RequestBody(required = false) String rawBody) {

        if (rawBody == null || rawBody.isBlank()) {
            log.warn("Webhook nhận được body rỗng, có thể là test ping từ PayOS. Bỏ qua.");
            return ResponseEntity.ok("ignored-empty-body");
        }

        try {
            WebhookData verifiedData = null;
            try {
                verifiedData = payOS.webhooks().verify(rawBody);
            } catch (Exception sigEx) {
                log.warn("PayOS webhook verify failed: {}", sigEx.getMessage());
            }

            if (verifiedData != null && verifiedData.getOrderCode() == 0) {
                log.warn("Nhận được payload với orderCode = 0 (có thể là test). Bỏ qua.");
                return ResponseEntity.ok("ignored-test-payload");
            }

            if (verifiedData != null && "00".equals(verifiedData.getCode())) {
                long orderCode = verifiedData.getOrderCode();
                String orderCodeStr = String.valueOf(orderCode);

                if (orderCodeStr.length() < (SUFFIX_LENGTH + 2)) {
                    log.warn("Webhook nhận được orderCode quá ngắn: {}. Không thể xử lý.", orderCode);
                    return ResponseEntity.ok("ignored-short-ordercode");
                }

                String mainPart = orderCodeStr.substring(0, orderCodeStr.length() - SUFFIX_LENGTH);
                char paymentType = mainPart.charAt(0);
                long bookingId = Long.parseLong(mainPart.substring(1));

                log.info("Webhook nhận được thanh toán cho orderCode: {}, paymentType: {}, bookingId: {}",
                        orderCode, paymentType, bookingId);

                if (paymentType == '1') {
                    paymentService.autoConfirmDeposit(bookingId);
                    log.info("Xử lý thành công webhook (cọc 500k) cho bookingId: {}", bookingId);
                    return ResponseEntity.ok("processed-deposit-booking-" + bookingId);

                } else if (paymentType == '2') {
                    paymentService.autoConfirmRentalDeposit(bookingId);
                    log.info("Xử lý thành công webhook (cọc 2%) cho bookingId: {}", bookingId);
                    return ResponseEntity.ok("processed-rental-booking-" + bookingId);

                } else {
                    log.warn("Webhook nhận được paymentType không xác định: {}. Bỏ qua.", paymentType);
                    return ResponseEntity.ok("ignored-unknown-type");
                }

            } else if (verifiedData != null) {
                log.warn("Nhận được webhook payOS nhưng code không phải '00'. Code: {}, Desc: {}",
                        verifiedData.getCode(), verifiedData.getDesc());
                return ResponseEntity.ok("ignored-non-success-code");
            } else {
                log.warn("Nhận được webhook nhưng không thể xác thực (verifiedData is null).");
                return ResponseEntity.ok("ignored-verification-failed");
            }

        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng xử lý webhook payOS: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("processing-error: " + e.getMessage());
        }
    }
}