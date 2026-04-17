package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.DashboardSummaryDto;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

public interface DashboardService {
    DashboardSummaryDto getSummaryForStaff(User staff);
}
