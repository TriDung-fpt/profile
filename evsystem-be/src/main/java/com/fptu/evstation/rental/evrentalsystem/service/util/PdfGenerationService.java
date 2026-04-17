package com.fptu.evstation.rental.evrentalsystem.service.util;

import com.fptu.evstation.rental.evrentalsystem.dto.BillResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.BookingRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.PenaltyFeeRepository;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
@Slf4j
public class PdfGenerationService {
    public static final String FONT_PATH = "fonts/times.ttf";
    public static final String BOLD_FONT_PATH = "fonts/timesbd.ttf";
    public static final String ITALIC_FONT_PATH = "fonts/timesi.ttf";

    private final PenaltyFeeRepository penaltyFeeRepository;
    private final BookingRepository bookingRepository;

    public PdfGenerationService(PenaltyFeeRepository penaltyFeeRepository, BookingRepository bookingRepository) {
        this.penaltyFeeRepository = penaltyFeeRepository;
        this.bookingRepository = bookingRepository;
    }

    public void generateContractPdf(Path filePath, Booking booking, User staff) {
        try {
            PdfWriter writer = new PdfWriter(filePath.toString());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(30, 30, 30, 30);

            PdfFont regularFont = PdfFontFactory.createFont(FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            PdfFont boldFont = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            PdfFont italicFont = PdfFontFactory.createFont(ITALIC_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            document.setFont(regularFont);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd/MM/yyyy");

            User renter = booking.getUser();
            Vehicle vehicle = booking.getVehicle();
            Model model = vehicle.getModel();
            Station station = booking.getStation();

            document.add(new Paragraph("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM").setTextAlignment(TextAlignment.CENTER).setFont(boldFont));
            document.add(new Paragraph("Độc lập – Tự do – Hạnh phúc").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("---o0o---").setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("HỢP ĐỒNG THUÊ XE ĐIỆN TỬ").setTextAlignment(TextAlignment.CENTER).setFont(boldFont).setFontSize(16).setMarginTop(20));
            document.add(new Paragraph("Số hợp đồng: " + booking.getBookingId() + "_CONTRACT").setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Mã Booking: " + booking.getBookingId()).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("BÊN CHO THUÊ (BÊN A)").setFont(boldFont).setMarginTop(15));
            document.add(new Paragraph("Công ty: CÔNG TY TNHH CÔNG NGHỆ EVOLVE"));
            document.add(new Paragraph("Địa chỉ: " + station.getAddress()));
            document.add(new Paragraph("Đại diện bởi: " + staff.getFullName() + "   Chức vụ: Nhân viên Vận hành Trạm"));

            document.add(new Paragraph("BÊN THUÊ (BÊN B)").setFont(boldFont).setMarginTop(10));
            document.add(new Paragraph("Họ và tên: " + renter.getFullName()));
            document.add(new Paragraph("Số CCCD: " + renter.getCccd()));
            document.add(new Paragraph("Số điện thoại: " + renter.getPhone()));
            document.add(new Paragraph("Email: " + renter.getEmail()));
            document.add(new Paragraph("Số GPLX: " + renter.getGplx()));

            document.add(new Paragraph("Sau khi trao đổi, hai bên thống nhất ký kết Hợp đồng Thuê xe Điện tử với các điều khoản sau:").setMarginTop(15));

            document.add(new Paragraph("Điều 1. Đối tượng và Tình trạng Xe Thuê").setFont(boldFont).setMarginTop(15));
            Table vehicleTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            vehicleTable.addCell(new Paragraph("Loại xe").setFont(boldFont));
            vehicleTable.addCell(new Paragraph(model.getVehicleType().name()));
            vehicleTable.addCell(new Paragraph("Nhãn hiệu").setFont(boldFont));
            vehicleTable.addCell(new Paragraph(model.getModelName()));
            vehicleTable.addCell(new Paragraph("Biển số xe").setFont(boldFont));
            vehicleTable.addCell(new Paragraph(vehicle.getLicensePlate()));
            document.add(vehicleTable);

            document.add(new Paragraph("Điều 2. Thời hạn và Lịch trình Thuê").setFont(boldFont).setMarginTop(15));
            Table timeTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            timeTable.addCell(new Paragraph("Thời gian nhận xe").setFont(boldFont));
            timeTable.addCell(new Paragraph(booking.getStartDate().format(formatter)));
            timeTable.addCell(new Paragraph("Thời gian trả xe dự kiến").setFont(boldFont));
            timeTable.addCell(new Paragraph(booking.getEndDate().format(formatter)));
            document.add(timeTable);

            document.add(new Paragraph("Điều 3. Chi phí và Thanh toán").setFont(boldFont).setMarginTop(15));
            Table feeTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            feeTable.addCell(new Paragraph("Đơn giá thuê").setFont(boldFont));
            feeTable.addCell(new Paragraph(String.format("%,.0f VNĐ/giờ", model.getPricePerHour())));
            feeTable.addCell(new Paragraph("Tiền cọc thuê xe (2%)").setFont(boldFont));
            feeTable.addCell(new Paragraph(String.format("%,.0f VNĐ", booking.getRentalDeposit())));
            document.add(feeTable);

            document.add(new Paragraph("Điều 4. Quyền và Nghĩa vụ các Bên").setFont(boldFont).setMarginTop(15));
            document.add(new Paragraph("Quyền và nghĩa vụ các Bên được thực hiện theo quy định chi tiết tại ").add(new Text("Điều Khoản Dịch Vụ của Evolve ").setFont(italicFont)).add("được công bố trên website/ứng dụng."));

            document.add(new Paragraph("Điều 5. Các Khoản Phí Phát Sinh Tiềm Năng").setFont(boldFont).setMarginTop(15));
            document.add(new Paragraph("Bên B lưu ý rằng các khoản phí sau có thể được áp dụng khi trả xe tùy thuộc vào tình trạng thực tế và sẽ được trừ vào tiền cọc hoặc yêu cầu thanh toán thêm:"));

            List<PenaltyFee> penaltyFees = penaltyFeeRepository.findByIsAdjustmentIsFalse();

            if (!penaltyFees.isEmpty()) {
                Table priceTable = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth().setMarginTop(10);
                priceTable.addCell(new Paragraph("Loại phí").setFont(boldFont).setTextAlignment(TextAlignment.CENTER));
                priceTable.addCell(new Paragraph("Giá tham khảo (VNĐ)").setFont(boldFont).setTextAlignment(TextAlignment.CENTER));

                for (PenaltyFee fee : penaltyFees) {
                    priceTable.addCell(new Paragraph(fee.getFeeName()));
                    priceTable.addCell(new Paragraph(String.format("%,.0f", fee.getFixedAmount())).setTextAlignment(TextAlignment.RIGHT));
                }

                document.add(priceTable);
            }
            Paragraph note = new Paragraph("Ghi chú: Mức phí trên chỉ mang tính tham khảo. Chi phí thực tế sẽ được áp dụng dựa trên đánh giá tình trạng xe khi hoàn trả.")
                    .setFont(italicFont)
                    .setFontSize(9)
                    .setMarginTop(5);
            document.add(note);

            com.itextpdf.layout.element.List feeList = new com.itextpdf.layout.element.List()
                    .setSymbolIndent(12)
                    .setListSymbol("\u2022")
                    .setFont(regularFont)
                    .setMarginTop(10);

            feeList.add(new ListItem("Phí vệ sinh xe: Áp dụng khi xe bị bẩn, có mùi, hoặc có rác bên trong."));
            feeList.add(new ListItem("Phí hư hỏng nhẹ: Áp dụng cho các trường hợp trầy xước, móp nhỏ, nứt gương, vỡ đèn..."));
            feeList.add(new ListItem("Các phí khác: Bao gồm phí xử lý vi phạm giao thông, phí cứu hộ, hư hỏng nặng... sẽ được áp dụng theo bảng giá hiện hành của Bên A."));
            document.add(feeList);

            document.add(new Paragraph("Điều 9. Xác nhận và Chữ ký").setFont(boldFont).setMarginTop(25));
            Table signatureTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            signatureTable.addCell(new Paragraph("ĐẠI DIỆN BÊN A\n(Ký và ghi rõ họ tên)").setTextAlignment(TextAlignment.CENTER));
            signatureTable.addCell(new Paragraph("BÊN B\n(Ký và ghi rõ họ tên)").setTextAlignment(TextAlignment.CENTER));
            signatureTable.addCell(new Paragraph("\n\n\n\n" + staff.getFullName()).setTextAlignment(TextAlignment.CENTER));
            signatureTable.addCell(new Paragraph("\n\n\n\n" + renter.getFullName()).setTextAlignment(TextAlignment.CENTER));
            document.add(signatureTable);

            document.close();
            log.info("Đã tạo thành công hợp đồng PDF tại: {}", filePath);
        } catch (IOException e) {
            log.error("Lỗi khi tạo file PDF cho hợp đồng", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo file PDF hợp đồng.");
        }
    }

    public void generateInvoicePdf(Path filePath, BillResponse billDetails) {
        try {
            PdfWriter writer = new PdfWriter(filePath.toString());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            PdfFont regularFont = PdfFontFactory.createFont(FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            PdfFont boldFont = PdfFontFactory.createFont(BOLD_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            PdfFont italicFont = PdfFontFactory.createFont(ITALIC_FONT_PATH, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            document.setFont(regularFont);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm, 'ngày' dd/MM/yyyy");
            LocalDateTime dateTime = billDetails.getDateTime() != null ? billDetails.getDateTime() : LocalDateTime.now();

            Booking booking = bookingRepository.findById(billDetails.getBookingId()).orElseThrow();
            User renter = booking.getUser();
            Vehicle vehicle = booking.getVehicle();

            document.add(new Paragraph("HÓA ĐƠN THANH TOÁN DỊCH VỤ").setTextAlignment(TextAlignment.CENTER).setFont(boldFont).setFontSize(18));
            document.add(new Paragraph("Mã Booking: #" + billDetails.getBookingId()).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Ngày lập: " + dateTime.format(formatter)).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("THÔNG TIN DỊCH VỤ").setFont(boldFont).setMarginTop(15));
            Table serviceTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            serviceTable.addCell(new Paragraph("Tên xe:"));
            serviceTable.addCell(new Paragraph(vehicle.getModel().getModelName()));
            serviceTable.addCell(new Paragraph("Biển số:"));
            serviceTable.addCell(new Paragraph(vehicle.getLicensePlate()));
            serviceTable.addCell(new Paragraph("Thời gian nhận:"));
            serviceTable.addCell(new Paragraph(booking.getStartDate().format(formatter)));
            serviceTable.addCell(new Paragraph("Thời gian trả:"));
            serviceTable.addCell(new Paragraph(dateTime.format(formatter)));
            document.add(serviceTable);

            document.add(new Paragraph("THÔNG TIN KHÁCH HÀNG").setFont(boldFont).setMarginTop(10));
            Table customerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            customerTable.addCell(new Paragraph("Họ và tên:"));
            customerTable.addCell(new Paragraph(renter.getFullName()));
            customerTable.addCell(new Paragraph("Số điện thoại:"));
            customerTable.addCell(new Paragraph(renter.getPhone()));
            customerTable.addCell(new Paragraph("Email:"));
            customerTable.addCell(new Paragraph(renter.getEmail()));
            document.add(customerTable);

            document.add(new Paragraph("CHI TIẾT CHI PHÍ PHÁT SINH").setFont(boldFont).setMarginTop(15));
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
            detailsTable.addHeaderCell(new Paragraph("Nội dung").setFont(boldFont));
            detailsTable.addHeaderCell(new Paragraph("Số tiền (VNĐ)").setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));

            detailsTable.addCell(new Paragraph("Phí thuê xe cơ bản"));
            detailsTable.addCell(new Paragraph(String.format("%,.0f", billDetails.getBaseRentalFee())).setTextAlignment(TextAlignment.RIGHT));

            if (billDetails.getFeeItems() != null && !billDetails.getFeeItems().isEmpty()) {
                boolean hasPositiveFees = billDetails.getFeeItems().stream().anyMatch(item -> item.getAmount() > 0);
                if (hasPositiveFees) {
                    detailsTable.addCell(new Paragraph("Phụ phí phát sinh:").setFont(boldFont));
                    detailsTable.addCell("");
                    for (BillResponse.FeeItem item : billDetails.getFeeItems()) {
                        if (item.getAmount() > 0) {
                            detailsTable.addCell(new Paragraph("  - " + item.getFeeName()).setPaddingLeft(20));
                            detailsTable.addCell(new Paragraph(String.format("%,.0f", item.getAmount())).setTextAlignment(TextAlignment.RIGHT));
                        }
                    }
                }
            }

            double totalDebit = billDetails.getBaseRentalFee() + billDetails.getTotalPenaltyFee();

            detailsTable.addCell(new Paragraph("TỔNG CHI PHÍ KHÁCH CẦN THANH TOÁN:").setFont(boldFont).setFontSize(14));
            detailsTable.addCell(new Paragraph(String.format("%,.0f", totalDebit)).setTextAlignment(TextAlignment.RIGHT).setFont(boldFont).setFontSize(14));
            document.add(detailsTable);

            document.add(new Paragraph("CHI TIẾT CỌC XE & GIẢM GIÁ").setFont(boldFont).setMarginTop(20));
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
            summaryTable.addHeaderCell(new Paragraph("Nội dung").setFont(boldFont));
            summaryTable.addHeaderCell(new Paragraph("Số tiền (VNĐ)").setFont(boldFont).setTextAlignment(TextAlignment.RIGHT));

            summaryTable.addCell(new Paragraph("Tiền cọc thuê xe khách đã trả (500.000 VNĐ + 2% giá trị xe) :"));
            summaryTable.addCell(new Paragraph(String.format("%,.0f", billDetails.getDownpayPaid())).setTextAlignment(TextAlignment.RIGHT));

            List<BillResponse.FeeItem> negativeFees = billDetails.getFeeItems().stream()
                    .filter(item -> item.getAmount() < 0)
                    .toList();

            if (!negativeFees.isEmpty()) {
                summaryTable.addCell(new Paragraph("Giảm giá / Khuyến mãi:").setFont(boldFont));
                summaryTable.addCell("");
                for (BillResponse.FeeItem item : negativeFees) {
                    summaryTable.addCell(new Paragraph("  - " + item.getFeeName()).setPaddingLeft(20));
                    summaryTable.addCell(new Paragraph(String.format("%,.0f", Math.abs(item.getAmount()))).setTextAlignment(TextAlignment.RIGHT));
                }
            }

            double totalCredit = billDetails.getDownpayPaid() + billDetails.getTotalDiscount();
            summaryTable.addCell(new Paragraph("TỔNG TIỀN ĐÃ CỌC & GIẢM GIÁ (B):").setFont(boldFont).setFontSize(14));
            summaryTable.addCell(new Paragraph(String.format("%,.0f", totalCredit)).setTextAlignment(TextAlignment.RIGHT).setFont(boldFont).setFontSize(14));

            document.add(summaryTable);

            Table finalSummaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                    .useAllAvailableWidth()
                    .setMarginTop(20);
            Cell titleCell = new Cell(1, 2)
                    .add(new Paragraph("QUYẾT TOÁN CUỐI CÙNG"))
                    .setFont(boldFont)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setBorder(null);
            finalSummaryTable.addCell(titleCell);

            if (billDetails.getPaymentDue() > 0) {
                finalSummaryTable.addCell(new Paragraph("KHÁCH HÀNG THANH TOÁN THÊM (A - B):").setFont(boldFont).setFontSize(13));
                finalSummaryTable.addCell(new Paragraph(String.format("%,.0f", billDetails.getPaymentDue())).setTextAlignment(TextAlignment.RIGHT).setFont(boldFont).setFontSize(13));
            } else if (billDetails.getRefundToCustomer() > 0) {
                finalSummaryTable.addCell(new Paragraph("CÔNG TY HOÀN TRẢ KHÁCH HÀNG (B - A):").setFont(boldFont).setFontSize(13));
                finalSummaryTable.addCell(new Paragraph(String.format("%,.0f", billDetails.getRefundToCustomer())).setTextAlignment(TextAlignment.RIGHT).setFont(boldFont).setFontSize(13));
            } else {
                finalSummaryTable.addCell(new Paragraph("QUYẾT TOÁN HOÀN TẤT (KHÔNG PHÁT SINH):").setFont(boldFont).setFontSize(13));
                finalSummaryTable.addCell(new Paragraph("0").setTextAlignment(TextAlignment.RIGHT).setFont(boldFont).setFontSize(13));
            }

            Cell noteCell = new Cell(1, 2)
                    .add(new Paragraph("Lưu ý: Số tiền hoàn trả sẽ được xử lý sau khi khách hàng đã thanh toán đầy đủ các chi phí phát sinh (nếu có)."))
                    .setFont(italicFont)
                    .setFontSize(9)
                    .setPaddingTop(8)
                    .setBorder(null);

            finalSummaryTable.addCell(noteCell);
            finalSummaryTable.setKeepTogether(true);
            document.add(finalSummaryTable);

            if (billDetails.getPaymentDue() > 0 && billDetails.getQrCodeUrl() != null) {
                document.add(new Paragraph("QUÉT MÃ ĐỂ THANH TOÁN CHI PHÍ PHÁT SINH").setFont(boldFont).setTextAlignment(TextAlignment.CENTER).setMarginTop(20));
                String base64Image = billDetails.getQrCodeUrl().split(",")[1];
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);
                Image qrImage = new Image(ImageDataFactory.create(imageBytes));
                qrImage.setWidth(UnitValue.createPercentValue(40));
                qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                document.add(qrImage);
            }

            document.close();
            log.info("Đã tạo thành công hóa đơn PDF tại: {}", filePath);
        } catch (Exception e) {
            log.error("Lỗi khi tạo file PDF hóa đơn", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tạo file PDF hóa đơn.");
        }
    }
}