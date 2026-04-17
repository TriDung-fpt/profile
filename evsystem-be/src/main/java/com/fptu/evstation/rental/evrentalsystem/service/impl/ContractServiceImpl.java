package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.dto.ContractSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.*;
import com.fptu.evstation.rental.evrentalsystem.repository.ContractRepository;
import com.fptu.evstation.rental.evrentalsystem.repository.TransactionRepository;
import com.fptu.evstation.rental.evrentalsystem.service.ContractService;
import com.fptu.evstation.rental.evrentalsystem.service.util.PdfGenerationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final TransactionRepository transactionRepository;
    private final PdfGenerationService pdfGenerationService;
    private final Path contractBaseDir = Paths.get(System.getProperty("user.dir"), "uploads", "contracts");

    @Override
    @Transactional
    public Contract generateAndSaveContract(Booking booking, User staff) {
        User user = booking.getUser();
        String renterNameNormalized = Normalizer.normalize(user.getFullName(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-zA-Z0-9]", "_");
        String fileName = String.format("HopDong_%s_Booking_%d.pdf", renterNameNormalized, booking.getBookingId());
        Path userDir = contractBaseDir.resolve("user_" + user.getUserId());
        Path filePath = userDir.resolve(fileName);

        try {
            Files.createDirectories(userDir);

            pdfGenerationService.generateContractPdf(filePath, booking, staff);

            String relativePath = "/uploads/contracts/user_" + user.getUserId() + "/" + fileName;
            Contract contract = Contract.builder()
                    .booking(booking)
                    .contractPdfPath(relativePath)
                    .signedDate(LocalDateTime.now())
                    .termsSnapshot("Điều khoản dịch vụ phiên bản 1.4 được áp dụng.")
                    .build();
            return contractRepository.save(contract);

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống khi tạo Hợp đồng PDF.");
        }
    }

    @Override
    public List<ContractSummaryResponse> getAllContractsByStation(User staff) {
        if (staff.getStation() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nhân viên chưa được gán cho trạm nào.");
        }

        List<Contract> contracts = contractRepository.findByBooking_Station(staff.getStation(), Sort.by(Sort.Direction.DESC, "signedDate"));

        return contracts.stream().map(contract -> {
            Booking booking = contract.getBooking();
            String staffName = transactionRepository.findByBooking(booking).stream()
                    .filter(t -> t.getStaffNote() != null && t.getStaffNote().contains("Thu cọc thuê xe"))
                    .map(t -> t.getStaff().getFullName())
                    .findFirst()
                    .orElse("N/A");

            return ContractSummaryResponse.builder()
                    .contractId(contract.getContractId())
                    .bookingId(booking.getBookingId())
                    .renterName(booking.getUser().getFullName())
                    .staffName(staffName)
                    .vehicleLicensePlate(booking.getVehicle().getLicensePlate())
                    .signedDate(contract.getSignedDate())
                    .contractPdfPath(contract.getContractPdfPath())
                    .build();
        }).toList();
    }
}
