package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.entity.Rating;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.RatingRepository;

import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import com.fptu.evstation.rental.evrentalsystem.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
    private final RatingRepository ratingRepository;
    private final StationService stationService;

    @Override
    public Rating saveRating(Long stationId, int stars, String comment, User user) {
        Station station = stationService.getStationById(stationId);

        Rating rating = new Rating();
        rating.setStation(station);
        rating.setStars(stars);
        rating.setComment(comment);
        rating.setUserId(user.getUserId());

        return ratingRepository.save(rating);
    }

    @Override
    public List<Rating> getRatingsByStation(Long stationId) {
        Station station = stationService.getStationById(stationId);
        return ratingRepository.findByStation(station);
    }
    @Override
    public Double getAverageRating(Long stationId) {
        Station station = stationService.getStationById(stationId);
        Double avg = ratingRepository.findAverageStarsByStation(station);
        return avg != null ? avg : 0.0;
    }

}