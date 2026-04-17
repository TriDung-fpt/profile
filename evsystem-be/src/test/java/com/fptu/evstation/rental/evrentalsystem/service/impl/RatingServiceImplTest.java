package com.fptu.evstation.rental.evrentalsystem.service.impl;

import com.fptu.evstation.rental.evrentalsystem.entity.Rating;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.User;
import com.fptu.evstation.rental.evrentalsystem.repository.RatingRepository;
import com.fptu.evstation.rental.evrentalsystem.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Thêm import cho List và Collections
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private StationService stationService;

    @InjectMocks
    private RatingServiceImpl ratingService;

    private Station mockStation;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockStation = Station.builder().stationId(1L).build();
        mockUser = User.builder().userId(1L).build();

        // Giả lập chung cho các phương thức
        when(stationService.getStationById(1L)).thenReturn(mockStation);
    }

    @Test
    void testSaveRating_Success() {
        // 1. Chuẩn bị
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating rating = invocation.getArgument(0);
            rating.setId(99L); // Giả lập DB gán ID
            return rating;
        });

        // 2. Thực thi
        Rating savedRating = ratingService.saveRating(1L, 5, "Tốt", mockUser);

        // 3. Xác minh
        assertNotNull(savedRating);
        assertEquals(99L, savedRating.getId());
        assertEquals(5, savedRating.getStars());
        assertEquals(mockUser.getUserId(), savedRating.getUserId());
        assertEquals(mockStation, savedRating.getStation());

        verify(stationService, times(1)).getStationById(1L);
        verify(ratingRepository, times(1)).save(any(Rating.class));
    }

    // --- BÀI TEST BỔ SUNG ĐỂ ĐẠT 100% ---
    @Test
    void testGetRatingsByStation_Success() {
        // 1. Chuẩn bị
        Rating mockRating = Rating.builder().id(1L).stars(5).build();
        List<Rating> mockList = Collections.singletonList(mockRating);
        when(ratingRepository.findByStation(mockStation)).thenReturn(mockList);

        // 2. Thực thi
        List<Rating> result = ratingService.getRatingsByStation(1L);

        // 3. Xác minh
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getStars());
        verify(stationService, times(1)).getStationById(1L);
        verify(ratingRepository, times(1)).findByStation(mockStation);
    }
    // --- KẾT THÚC BỔ SUNG ---

    @Test
    void testGetAverageRating_Success_WithRatings() {
        // 1. Chuẩn bị (Test luồng avg != null)
        when(ratingRepository.findAverageStarsByStation(mockStation)).thenReturn(4.5);

        // 2. Thực thi
        Double average = ratingService.getAverageRating(1L);

        // 3. Xác minh
        assertEquals(4.5, average);
        verify(stationService, times(1)).getStationById(1L);
        verify(ratingRepository, times(1)).findAverageStarsByStation(mockStation);
    }

    @Test
    void testGetAverageRating_Success_NoRatings() {
        // 1. Chuẩn bị (Test luồng avg == null)
        when(ratingRepository.findAverageStarsByStation(mockStation)).thenReturn(null);

        // 2. Thực thi
        Double average = ratingService.getAverageRating(1L);

        // 3. Xác minh
        assertEquals(0.0, average); // Phải trả về 0.0
        verify(stationService, times(1)).getStationById(1L);
        verify(ratingRepository, times(1)).findAverageStarsByStation(mockStation);
    }
}