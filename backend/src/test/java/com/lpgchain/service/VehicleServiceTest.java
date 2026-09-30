package com.lpgchain.service;

import com.lpgchain.dto.VehicleRequestDTO;
import com.lpgchain.dto.VehicleResponseDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Vehicle;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private DistributorMembershipRepository distributorMembershipRepository;

    @Mock
    private DeliveryAuthorizationService deliveryAuthorizationService;

    private final EntityMapper entityMapper = new EntityMapper();
    private VehicleService vehicleService;

    private Distributor distributor;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository, distributorRepository, distributorMembershipRepository, entityMapper, deliveryAuthorizationService);
        distributor = new Distributor();
        distributor.setId(1L);
        distributor.setAgencyName("Apex Gas");

        vehicle = new Vehicle(5L, "MH-01-AB-1234", "LPG_DELIVERY", 30, "AVAILABLE", distributor);
    }

    @Test
    @DisplayName("createVehicle: succeeds with valid payload and available status")
    void createVehicle_Success() {
        // ARRANGE
        VehicleRequestDTO dto = new VehicleRequestDTO("MH-01-AB-1234", "LPG_DELIVERY", 30, 1L, "AVAILABLE");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(vehicleRepository.existsByVehicleNumber("MH-01-AB-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);

        // ACT
        VehicleResponseDTO result = vehicleService.createVehicle(dto);

        // ASSERT
        assertNotNull(result);
        assertEquals("MH-01-AB-1234", result.getVehicleNumber());
        assertEquals("LPG_DELIVERY", result.getVehicleType());
        assertEquals(30, result.getCapacity());
        assertEquals("AVAILABLE", result.getStatus());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanManageDistributorResource(1L);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    @DisplayName("createVehicle: throws IllegalArgumentException when capacity <= 0")
    void createVehicle_InvalidCapacity() {
        // ARRANGE
        VehicleRequestDTO dto = new VehicleRequestDTO("MH-01-AB-1234", "LPG_DELIVERY", 0, 1L, "AVAILABLE");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(vehicleRepository.existsByVehicleNumber("MH-01-AB-1234")).thenReturn(false);

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> vehicleService.createVehicle(dto));
        assertEquals("Capacity must be greater than 0", ex.getMessage());

        // VERIFY
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createVehicle: throws IllegalArgumentException when vehicle number is duplicated")
    void createVehicle_DuplicateVehicleNumber() {
        // ARRANGE
        VehicleRequestDTO dto = new VehicleRequestDTO("MH-01-AB-1234", "LPG_DELIVERY", 30, 1L, "AVAILABLE");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(vehicleRepository.existsByVehicleNumber("MH-01-AB-1234")).thenReturn(true);

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> vehicleService.createVehicle(dto));
        assertEquals("Vehicle with number MH-01-AB-1234 already exists", ex.getMessage());

        // VERIFY
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("createVehicle: throws IllegalArgumentException on invalid status")
    void createVehicle_InvalidStatus() {
        // ARRANGE
        VehicleRequestDTO dto = new VehicleRequestDTO("MH-01-AB-1234", "LPG_DELIVERY", 30, 1L, "DESTROYED");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(vehicleRepository.existsByVehicleNumber("MH-01-AB-1234")).thenReturn(false);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> vehicleService.createVehicle(dto));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteVehicle: successfully deletes existing vehicle")
    void deleteVehicle_Success() {
        // ARRANGE
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(vehicle));

        // ACT
        boolean result = vehicleService.deleteVehicle(5L);

        // ASSERT
        assertEquals(true, result);

        // VERIFY
        verify(deliveryAuthorizationService).checkCanManageDistributorResource(1L);
        verify(vehicleRepository).delete(vehicle);
    }
}
