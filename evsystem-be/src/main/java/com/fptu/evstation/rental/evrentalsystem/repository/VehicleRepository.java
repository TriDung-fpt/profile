package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.Model;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.Vehicle;
import com.fptu.evstation.rental.evrentalsystem.entity.VehicleStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {
    boolean existsByLicensePlate(String licensePlate);

    long countByStation(Station station);

    long countByModelAndStation(Model model, Station station);

    List<Vehicle> findByStation(Station station, Sort sort);

    List<Vehicle> findByStationAndModelAndStatusNotIn(Station station, Model model, List<VehicleStatus> statuses);

    @Query("SELECT v.status, COUNT(v) FROM Vehicle v WHERE v.station = :station GROUP BY v.status")
    List<Object[]> countVehiclesByStatus(@Param("station") Station station);


    @Query("""
        SELECT v.station.stationId, v.station.name, v.status, COUNT(v)
        FROM Vehicle v
        GROUP BY v.station.stationId, v.station.name, v.status""")
    List<Object[]> getVehicleStatsGroupedByStation();

    @Query("SELECT v.model.id FROM Vehicle v WHERE v.station = :station GROUP BY v.model.id")
    List<Long> findDistinctModelIdsByStation(@Param("station") Station station);

    @Query("SELECT v FROM Vehicle v " +
            "LEFT JOIN FETCH v.station " +
            "LEFT JOIN FETCH v.model " +
            "WHERE v.station = :station")
    List<Vehicle> findByStationWithDetails(@Param("station") Station station, Sort sort);
}
