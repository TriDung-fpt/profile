package com.fptu.evstation.rental.evrentalsystem.service;

import com.fptu.evstation.rental.evrentalsystem.entity.Rating;
import com.fptu.evstation.rental.evrentalsystem.entity.User;

import java.util.List;

public interface RatingService {
    Rating saveRating(Long stationId, int stars, String comment, User user);
    List<Rating> getRatingsByStation(Long stationId);
    Double getAverageRating(Long stationId);

}