package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.BillResponse;
import com.fptu.evstation.rental.evrentalsystem.dto.InvoiceSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.service.InvoiceService;
import com.fptu.evstation.rental.evrentalsystem.service.util.EmailService;
import com.fptu.evstation.rental.evrentalsystem.service.util.PdfGenerationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final BookingRepository bookingRepository;
    private final PdfGenerationService pdfGenerationService;
    private final Path invoiceBaseDir = Paths.get(System.getProperty("user.dir"), "uploads", "invoices");
    private final EmailService emailService;

    @Override
    @Transactional
    public String generateAndSaveInvoicePdf(BillResponse billDetails) {
        try {
            Booking booking = bookingRepository.findById(billDetails.getBookingId()).orElseThrow();
            String renterNameNormalized = Normalizer.normalize(booking.getUser().getFullName(), Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = String.format("HoaDon_%s_Booking_%d.pdf", renterNameNormalized, booking.getBookingId());
            Path userDir = invoiceBaseDir.resolve("user_" + booking.getUser().getUserId());
            Files.createDirectories(userDir);
            Path filePath = userDir.resolve(fileName);

            pdfGenerationService.generateInvoicePdf(filePath, billDetails);
            String relativePath = "/uploads/invoices/user_" + booking.getUser().getUserId() + "/" + fileName;

            booking.setInvoicePdfPath(relativePath);
            bookingRepository.save(booking);
            File pdfFile = filePath.toFile();
            String invoiceCode = "HD" + booking.getBookingId();

            emailService.sendInvoiceWithAttachment(
                    booking.getUser().getEmail(),
                    booking.getUser().getFullName(),
                    invoiceCode,
                    pdfFile
            );

            log.info("Đã gửi hóa đơn tự động cho khách hàng: {}", booking.getUser().getEmail());
            return relativePath;
        } catch (IOException e) {
            log.error("Lỗi khi tạo file hóa đơn PDF cho Booking ID: {}", billDetails.getBookingId(), e);
            return null;
        }
    }

    @Override
    public List<InvoiceSummaryResponse> getAllInvoicesByStation(User staff) {

        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        List<Booking> bookingsWithInvoices = bookingRepository.findAllByInvoicePdfPathIsNotNullAndStation(staff.getStation(), Sort.by(Sort.Direction.DESC, "createdAt"));

        return bookingsWithInvoices.stream()
                .map(booking -> InvoiceSummaryResponse.builder()
                        .bookingId(booking.getBookingId())
                        .renterName(booking.getUser().getFullName())
                        .finalAmount(booking.getFinalFee())
                        .createdDate(booking.getCreatedAt())
                        .invoicePdfPath(booking.getInvoicePdfPath())
                        .build())
                .toList();
    }
}
