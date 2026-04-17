package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.ContractSummaryResponse;
import com.fptu.evstation.rental.evrentalsystem.entity.Booking;
import com.fptu.evstation.rental.evrentalsystem.entity.Contract;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

import java.util.List;

public interface ContractService {
    Contract generateAndSaveContract(Booking booking, User staff);
    List<ContractSummaryResponse> getAllContractsByStation(User staff);
}
