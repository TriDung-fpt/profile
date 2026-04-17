package com.fptu.evstation.rental.evrentalsystem.repository;

import com.fptu.evstation.rental.evrentalsystem.entity.Rating;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByStation(Station station);
    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.station = :station")
    Double findAverageStarsByStation(@Param("station") Station station);

}