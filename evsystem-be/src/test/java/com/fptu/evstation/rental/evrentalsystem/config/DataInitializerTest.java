package com.fptu.evstation.rental.evrentalsystem.config;

import com.fptu.evstation.rental.evrentalsystem.entity.Model;
import com.fptu.evstation.rental.evrentalsystem.entity.PenaltyFee;
import com.fptu.evstation.rental.evrentalsystem.entity.Role;
import com.fptu.evstation.rental.evrentalsystem.entity.Station;
import com.fptu.evstation.rental.evrentalsystem.entity.Vehicle;
import com.fptu.evstation.rental.evrentalsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepo;
    @Mock
    private PenaltyFeeRepository feeRepo;
    @Mock
    private ModelRepository modelRepository;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        lenient().when(roleRepo.findByRoleName(anyString())).thenReturn(Optional.empty());
        lenient().when(feeRepo.existsByFeeName(anyString())).thenReturn(false);
        lenient().when(stationRepository.existsByName(anyString())).thenReturn(false);
        lenient().when(modelRepository.existsByModelName(anyString())).thenReturn(false);
        lenient().when(vehicleRepository.existsByLicensePlate(anyString())).thenReturn(false);
    }

    @Test
    void testRun_AllDataNeedsInitialization() throws Exception {
        // Arrange
        // Provide mock data for findAll to allow vehicle initialization
        when(modelRepository.findAll()).thenReturn(List.of(Model.builder().modelId(1L).build()));
        when(stationRepository.findAll()).thenReturn(List.of(Station.builder().stationId(1L).build()));

        // Act
        dataInitializer.run();

        // Assert
        verify(roleRepo, times(3)).save(any(Role.class));
        verify(feeRepo, atLeast(6)).save(any(PenaltyFee.class));
        verify(stationRepository, atLeast(1)).save(any(Station.class));
        verify(modelRepository, atLeast(1)).save(any(Model.class));
        verify(vehicleRepository, atLeast(1)).save(any(Vehicle.class));
    }

    @Test
    void testRun_RolesAlreadyExist() throws Exception {
        when(roleRepo.findByRoleName(anyString())).thenReturn(Optional.of(new Role()));
        dataInitializer.run();
        verify(roleRepo, never()).save(any(Role.class));
    }

    @Test
    void testRun_PenaltyFeesAlreadyExist() throws Exception {
        when(feeRepo.existsByFeeName(anyString())).thenReturn(true);
        dataInitializer.run();
        verify(feeRepo, never()).save(any(PenaltyFee.class));
    }

    @Test
    void testRun_StationsAlreadyExist() throws Exception {
        when(stationRepository.existsByName(anyString())).thenReturn(true);
        dataInitializer.run();
        verify(stationRepository, never()).save(any(Station.class));
    }

    @Test
    void testRun_ModelsAlreadyExist() throws Exception {
        when(modelRepository.existsByModelName(anyString())).thenReturn(true);
        dataInitializer.run();
        verify(modelRepository, never()).save(any(Model.class));
    }

    @Test
    void testRun_VehiclesAlreadyExist() throws Exception {
        when(vehicleRepository.existsByLicensePlate(anyString())).thenReturn(true);
        when(modelRepository.findAll()).thenReturn(List.of(Model.builder().modelId(1L).build()));
        when(stationRepository.findAll()).thenReturn(List.of(Station.builder().stationId(1L).build()));
        dataInitializer.run();
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testInitVehicles_ModelNotFound() throws Exception {
        when(stationRepository.findAll()).thenReturn(List.of(Station.builder().stationId(1L).build()));
        when(modelRepository.findAll()).thenReturn(Collections.emptyList());
        dataInitializer.run();
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testInitVehicles_StationNotFound() throws Exception {
        when(modelRepository.findAll()).thenReturn(List.of(Model.builder().modelId(1L).build()));
        when(stationRepository.findAll()).thenReturn(Collections.emptyList());
        dataInitializer.run();
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void testGetDynamicImagePaths_DirectoryNotFound() throws Exception {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(false);
            dataInitializer.run();
            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelRepository, atLeastOnce()).save(modelCaptor.capture());
            assertTrue(modelCaptor.getAllValues().stream().anyMatch(m -> m.getImagePaths() == null));
        }
    }

    @Test
    void testGetDynamicImagePaths_IoException() throws Exception {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.isDirectory(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.list(any(Path.class))).thenThrow(new IOException("Disk read error"));

            dataInitializer.run();

            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelRepository, atLeastOnce()).save(modelCaptor.capture());
            assertTrue(modelCaptor.getAllValues().stream().anyMatch(m -> m.getImagePaths() == null));
        }
    }

    @Test
    void testGetDynamicImagePaths_NoImagesInDirectory() throws Exception {
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mockedFiles.when(() -> Files.isDirectory(any(Path.class))).thenReturn(true);
            // Return a new empty stream every time it's called to avoid IllegalStateException
            mockedFiles.when(() -> Files.list(any(Path.class))).thenAnswer(invocation -> Stream.empty());

            dataInitializer.run();

            ArgumentCaptor<Model> modelCaptor = ArgumentCaptor.forClass(Model.class);
            verify(modelRepository, atLeastOnce()).save(modelCaptor.capture());
            assertTrue(modelCaptor.getAllValues().stream().anyMatch(m -> m.getImagePaths() == null));
        }
    }
}
