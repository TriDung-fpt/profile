package com.fptu.evstation.rental.evrentalsystem.service.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @InjectMocks
    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGenerateQrCodeBase64_Success_ValidAmount() {
        double amount = 200000.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
        assertTrue(result.length() > 100);
    }

    @Test
    void testGenerateQrCodeBase64_Success_DifferentAmounts() {
        Long bookingId = 1L;

        String result1 = qrCodeService.generateQrCodeBase64(100000.0, bookingId);
        String result2 = qrCodeService.generateQrCodeBase64(500000.0, bookingId);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.startsWith("data:image/png;base64,"));
        assertTrue(result2.startsWith("data:image/png;base64,"));
        assertNotEquals(result1, result2);
    }

    @Test
    void testGenerateQrCodeBase64_Success_DifferentBookingIds() {
        double amount = 200000.0;

        String result1 = qrCodeService.generateQrCodeBase64(amount, 1L);
        String result2 = qrCodeService.generateQrCodeBase64(amount, 2L);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.startsWith("data:image/png;base64,"));
        assertTrue(result2.startsWith("data:image/png;base64,"));
        assertNotEquals(result1, result2);
    }

    @Test
    void testGenerateQrCodeBase64_Success_ZeroAmount() {
        double amount = 0.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_LargeAmount() {
        double amount = 10000000.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_DecimalAmount() {
        double amount = 200000.50;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_Base64Format() {
        double amount = 200000.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertTrue(result.startsWith("data:image/png;base64,"));
        String base64Part = result.substring("data:image/png;base64,".length());
        
        try {
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Part);
            assertTrue(decodedBytes.length > 0);
        } catch (IllegalArgumentException e) {
            fail("Base64 string is not valid");
        }
    }

    @Test
    void testGenerateQrCodeBase64_Success_ConsistentFormat() {
        double amount = 200000.0;
        Long bookingId = 1L;

        String result1 = qrCodeService.generateQrCodeBase64(amount, bookingId);
        String result2 = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.startsWith("data:image/png;base64,"));
        assertTrue(result2.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_LongBookingId() {
        double amount = 200000.0;
        Long bookingId = 999999999L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_SmallBookingId() {
        double amount = 200000.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_NegativeAmountHandling() {
        double amount = -100000.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);

        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
    }

    @Test
    void testGenerateQrCodeBase64_Success_ReturnsValidPngImage() {
        double amount = 200000.0;
        Long bookingId = 1L;

        String result = qrCodeService.generateQrCodeBase64(amount, bookingId);
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/"));
        assertTrue(result.contains(";base64,"));

        byte[] bytes = decodeDataUri(result);
        assertTrue(bytes.length > 0, "Ảnh rỗng");

        boolean png = isPng(bytes);
        boolean jpeg = isJpeg(bytes);
        assertTrue(png || jpeg, String.format(
                "Không phải PNG/JPEG. Header: %02X %02X %02X %02X",
                bytes[0], bytes[1], bytes[2], bytes[3]
        ));
    }

    private static byte[] decodeDataUri(String dataUri) {
        String prefix = "data:image/";
        int comma = dataUri.indexOf(',');
        if (!dataUri.startsWith(prefix) || comma < 0) {
            fail("Sai định dạng data URI");
        }
        String base64 = dataUri.substring(comma + 1);
        try {
            return java.util.Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            fail("Base64 không hợp lệ");
            return null;
        }
    }

    private static boolean isPng(byte[] b) {
        return b.length >= 8
                && b[0]==(byte)0x89 && b[1]==(byte)0x50 && b[2]==(byte)0x4E && b[3]==(byte)0x47
                && b[4]==(byte)0x0D && b[5]==(byte)0x0A && b[6]==(byte)0x1A && b[7]==(byte)0x0A;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length >= 3
                && b[0]==(byte)0xFF && b[1]==(byte)0xD8 && b[2]==(byte)0xFF;
    }
}
