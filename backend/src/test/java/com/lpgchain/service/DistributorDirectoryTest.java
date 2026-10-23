package com.lpgchain.service;

import com.lpgchain.dto.DistributorSummaryDTO;
import com.lpgchain.entity.Distributor;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.DistributorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DistributorDirectoryTest — Unit tests for DistributorService and directory search logic.
 */
@ExtendWith(MockitoExtension.class)
class DistributorDirectoryTest {

    @Mock
    private DistributorRepository distributorRepository;

    @Spy
    private EntityMapper entityMapper = new EntityMapper();

    @InjectMocks
    private DistributorService distributorService;

    private Distributor distributorDomestic;
    private Distributor distributorCommercial;
    private Distributor distributorBoth;

    @BeforeEach
    void setUp() {
        distributorDomestic = new Distributor(
                1L, "Metro Domestic Gas", "LIC-001", "+919876543210",
                "10 MG Road", "ACTIVE", "secretPasskey1",
                LocalDateTime.now().plusDays(2), false,
                "400001", "Mumbai", "Maharashtra",
                18.9220, 72.8347, "DOMESTIC", "09:00 - 18:00"
        );

        distributorCommercial = new Distributor(
                2L, "Apex Commercial Fuels", "LIC-002", "+919876543211",
                "25 Industrial Area", "ACTIVE", "secretPasskey2",
                LocalDateTime.now().plusDays(2), false,
                "400050", "Mumbai", "Maharashtra",
                19.0550, 72.8300, "COMMERCIAL", "08:00 - 20:00"
        );

        distributorBoth = new Distributor(
                3L, "Universal Energy Services", "LIC-003", "+919876543212",
                "50 Ring Road", "ACTIVE", "secretPasskey3",
                LocalDateTime.now().plusDays(2), false,
                "110001", "New Delhi", "Delhi",
                28.6139, 77.2090, "BOTH", "24/7"
        );
    }

    @Test
    @DisplayName("DIR-01: getAllDistributors maps entities to sanitized DTOs and excludes passkey")
    void getAllDistributors_returnsSanitizedDTOsExcludingPasskeys() {
        when(distributorRepository.findAll()).thenReturn(List.of(distributorDomestic, distributorBoth));

        List<DistributorSummaryDTO> summaries = distributorService.getAllDistributors();

        assertThat(summaries).hasSize(2);
        DistributorSummaryDTO first = summaries.get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getAgencyName()).isEqualTo("Metro Domestic Gas");
        assertThat(first.getContactNumber()).isEqualTo("+919876543210");
        assertThat(first.getPinCode()).isEqualTo("400001");
        assertThat(first.getCity()).isEqualTo("Mumbai");
        assertThat(first.getState()).isEqualTo("Maharashtra");
        assertThat(first.getLatitude()).isEqualTo(18.9220);
        assertThat(first.getLongitude()).isEqualTo(72.8347);
        assertThat(first.getSupportedServices()).isEqualTo("DOMESTIC");
        assertThat(first.getOperatingHours()).isEqualTo("09:00 - 18:00");
        assertThat(first.getNetworkType()).isEqualTo("LPG_CHAIN_PARTNER");
        assertThat(first.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("DIR-02: getDistributorById returns sanitized DTO without sensitive credentials")
    void getDistributorById_whenFound_returnsSanitizedDTO() {
        when(distributorRepository.findById(1L)).thenReturn(Optional.of(distributorDomestic));

        Optional<DistributorSummaryDTO> result = distributorService.getDistributorById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getAgencyName()).isEqualTo("Metro Domestic Gas");
        assertThat(result.get().getNetworkType()).isEqualTo("LPG_CHAIN_PARTNER");
    }

    @Test
    @DisplayName("DIR-03: getDistributorById returns empty when distributor does not exist")
    void getDistributorById_whenNotFound_returnsEmpty() {
        when(distributorRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<DistributorSummaryDTO> result = distributorService.getDistributorById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("DIR-04: searchDistributors delegates to repository specification and maps sanitized results")
    void searchDistributors_validQuery_delegatesToRepository() {
        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distributorDomestic));

        List<DistributorSummaryDTO> results = distributorService.searchDistributors("400001", "Mumbai", "DOMESTIC");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAgencyName()).isEqualTo("Metro Domestic Gas");
        assertThat(results.get(0).getPinCode()).isEqualTo("400001");
        assertThat(results.get(0).getCity()).isEqualTo("Mumbai");
    }

    @Test
    @DisplayName("DIR-05: searchDistributors rejects invalid service type with IllegalArgumentException")
    void searchDistributors_invalidServiceType_throwsException() {
        assertThatThrownBy(() -> distributorService.searchDistributors("400001", "Mumbai", "INDUSTRIAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid service type: 'INDUSTRIAL'");

        assertThatThrownBy(() -> distributorService.searchDistributors(null, null, "UNKNOWN_SERVICE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supported values are DOMESTIC, COMMERCIAL, or BOTH");
    }

    @Test
    @DisplayName("DIR-06: searchDistributors accepts case-insensitive valid service types (domestic, commercial, both)")
    void searchDistributors_caseInsensitiveServiceType_isAccepted() {
        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distributorDomestic));

        List<DistributorSummaryDTO> resultsDomestic = distributorService.searchDistributors(null, null, "domestic");
        assertThat(resultsDomestic).hasSize(1);

        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distributorCommercial));

        List<DistributorSummaryDTO> resultsCommercial = distributorService.searchDistributors(null, null, "Commercial");
        assertThat(resultsCommercial).hasSize(1);

        when(distributorRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(distributorBoth));

        List<DistributorSummaryDTO> resultsBoth = distributorService.searchDistributors(null, null, "BOTH");
        assertThat(resultsBoth).hasSize(1);
    }

    @Test
    @DisplayName("DIR-07: searchDistributors returns empty list when repository returns no matches")
    void searchDistributors_noMatches_returnsEmptyList() {
        when(distributorRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<DistributorSummaryDTO> results = distributorService.searchDistributors("999999", "NonExistent", "BOTH");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("DIR-08: saveDistributor persists entity and returns sanitized summary DTO")
    void saveDistributor_persistsAndReturnsSummaryDTO() {
        Distributor toSave = new Distributor(
                null, "New Agency", "LIC-NEW", "1234567890",
                "New Address", "ACTIVE", "tempPasskey",
                LocalDateTime.now().plusDays(1), false,
                "500001", "Hyderabad", "Telangana",
                17.3850, 78.4867, "BOTH", "09:00 - 18:00"
        );
        Distributor saved = new Distributor(
                10L, "New Agency", "LIC-NEW", "1234567890",
                "New Address", "ACTIVE", "tempPasskey",
                LocalDateTime.now().plusDays(1), false,
                "500001", "Hyderabad", "Telangana",
                17.3850, 78.4867, "BOTH", "09:00 - 18:00"
        );
        when(distributorRepository.save(toSave)).thenReturn(saved);

        DistributorSummaryDTO result = distributorService.saveDistributor(toSave);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getAgencyName()).isEqualTo("New Agency");
        assertThat(result.getCity()).isEqualTo("Hyderabad");
        assertThat(result.getNetworkType()).isEqualTo("LPG_CHAIN_PARTNER");
    }

    @Test
    @DisplayName("DIR-09: deleteDistributor invokes repository deleteById")
    void deleteDistributor_delegatesToRepository() {
        distributorService.deleteDistributor(5L);

        verify(distributorRepository).deleteById(5L);
    }
}
