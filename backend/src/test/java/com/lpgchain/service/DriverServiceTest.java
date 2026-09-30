package com.lpgchain.service;

import com.lpgchain.dto.DriverRequestDTO;
import com.lpgchain.dto.DriverResponseDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.Driver;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorMembershipRepository;
import com.lpgchain.repository.DistributorRepository;
import com.lpgchain.repository.DriverRepository;
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
class DriverServiceTest {

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DistributorRepository distributorRepository;

    @Mock
    private DistributorMembershipRepository distributorMembershipRepository;

    @Mock
    private DeliveryAuthorizationService deliveryAuthorizationService;

    private final EntityMapper entityMapper = new EntityMapper();

    private DriverService driverService;

    private Distributor distributor;
    private Driver driver;

    @BeforeEach
    void setUp() {
        driverService = new DriverService(driverRepository, distributorRepository, distributorMembershipRepository, entityMapper, deliveryAuthorizationService);
        distributor = new Distributor();
        distributor.setId(1L);
        distributor.setAgencyName("Apex Gas");

        driver = new Driver(10L, "Ramesh", "+919876543210", "LIC-12345", "AVAILABLE", distributor);
    }

    @Test
    @DisplayName("createDriver: succeeds with valid payload and available status")
    void createDriver_Success() {
        // ARRANGE
        DriverRequestDTO dto = new DriverRequestDTO("Ramesh", "+919876543210", "LIC-12345", 1L, "AVAILABLE");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(driverRepository.existsByLicenseNumber("LIC-12345")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        // ACT
        DriverResponseDTO result = driverService.createDriver(dto);

        // ASSERT
        assertNotNull(result);
        assertEquals("Ramesh", result.getName());
        assertEquals("LIC-12345", result.getLicenseNumber());
        assertEquals("AVAILABLE", result.getStatus());

        // VERIFY
        verify(deliveryAuthorizationService).checkCanManageDistributorResource(1L);
        verify(driverRepository).save(any(Driver.class));
    }

    @Test
    @DisplayName("createDriver: throws IllegalArgumentException when distributor does not exist")
    void createDriver_MissingDistributor() {
        // ARRANGE
        DriverRequestDTO dto = new DriverRequestDTO("Ramesh", "+919876543210", "LIC-12345", 99L, "AVAILABLE");
        when(distributorRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> driverService.createDriver(dto));
        assertEquals("Distributor not found with ID: 99", ex.getMessage());

        // VERIFY
        verify(driverRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDriver: throws IllegalArgumentException when license number is duplicated")
    void createDriver_DuplicateLicense() {
        // ARRANGE
        DriverRequestDTO dto = new DriverRequestDTO("Ramesh", "+919876543210", "LIC-12345", 1L, "AVAILABLE");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(driverRepository.existsByLicenseNumber("LIC-12345")).thenReturn(true);

        // ACT & ASSERT
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> driverService.createDriver(dto));
        assertEquals("Driver with license number LIC-12345 already exists", ex.getMessage());

        // VERIFY
        verify(driverRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDriver: throws IllegalArgumentException on invalid status")
    void createDriver_InvalidStatus() {
        // ARRANGE
        DriverRequestDTO dto = new DriverRequestDTO("Ramesh", "+919876543210", "LIC-12345", 1L, "FLYING");
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributor));
        when(driverRepository.existsByLicenseNumber("LIC-12345")).thenReturn(false);

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> driverService.createDriver(dto));
        verify(driverRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteDriver: successfully deletes existing driver")
    void deleteDriver_Success() {
        // ARRANGE
        when(driverRepository.findById(10L)).thenReturn(Optional.of(driver));

        // ACT
        boolean result = driverService.deleteDriver(10L);

        // ASSERT
        assertEquals(true, result);

        // VERIFY
        verify(deliveryAuthorizationService).checkCanManageDistributorResource(1L);
        verify(driverRepository).delete(driver);
    }
}
