package com.fptu.evstation.rental.evrentalsystem.service.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class QrCodeService {

    private static final String VIETQR_API = "https://img.vietqr.io/image/";
    private static final String BANK_CODE = "tpb";
    private static final String ACCOUNT_NUMBER = "88303062005";
    private static final String ACCOUNT_NAME = "LE TRAN TRUONG HAI";

    /**
     * Tạo một đường link VietQR hợp lệ.
     * @param amount Số tiền cần thanh toán.
     * @param bookingId ID của booking để làm nội dung chuyển khoản.
     * @return Một URL hoàn chỉnh.
     */
    private String createVietQrLink(double amount, Long bookingId) {
        try {
            String info = URLEncoder.encode("TT BOOKING " + bookingId, StandardCharsets.UTF_8);
            String amountStr = String.valueOf((int) Math.round(amount));
            return String.format(
                    "%s%s-%s-qr_only.png?amount=%s&addInfo=%s&accountName=%s",
                    VIETQR_API,
                    BANK_CODE,
                    ACCOUNT_NUMBER,
                    amountStr,
                    info,
                    URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo QR link", e);
        }
    }

    /**
     * Tạo ảnh QR Code (dưới dạng Base64) từ một đường link VietQR.
     */
    public String generateQrCodeBase64(double amount, Long bookingId) {
        String qrImageUrl = createVietQrLink(amount, bookingId);

        try (InputStream inputStream = new URL(qrImageUrl).openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            inputStream.transferTo(outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            log.error("Lỗi khi tải ảnh VietQR", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải ảnh QR thanh toán.");
        }
    }
}