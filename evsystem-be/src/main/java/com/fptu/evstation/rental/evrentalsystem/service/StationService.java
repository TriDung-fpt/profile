package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.dto.StationRequest;
import com.fptu.evstation.rental.evrentalsystem.dto.UpdateStationRequest;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;

import java.util.List;
import java.util.Map;

public interface StationService {
    Station addStation(StationRequest request);
    List<Station> getAllStations();
    Station updateStation(Long id, UpdateStationRequest request);
    void deleteStation(Long id);
    Station getStationById(Long stationId);
    Map<String, Object> getVehicleStatsByStation(Long stationId);
    List<Map<String, Object>> getAllStationReports();
}