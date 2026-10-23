package com.lpgchain.service;

import com.lpgchain.dto.CustomerProfileDTO;
import com.lpgchain.dto.SavedAddressDTO;
import com.lpgchain.entity.Role;
import com.lpgchain.entity.SavedAddress;
import com.lpgchain.entity.User;
import com.lpgchain.entity.UserRole;
import com.lpgchain.mapper.EntityMapper;
import com.lpgchain.repository.CustomerDocumentRepository;
import com.lpgchain.repository.SavedAddressRepository;
import com.lpgchain.repository.UserRepository;
import com.lpgchain.repository.UserRoleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class CustomerProfileService {

    private final UserRepository userRepository;
    private final SavedAddressRepository savedAddressRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomerDocumentRepository customerDocumentRepository;
    private final EntityMapper entityMapper;

    public CustomerProfileService(UserRepository userRepository,
                                  SavedAddressRepository savedAddressRepository,
                                  UserRoleRepository userRoleRepository,
                                  CustomerDocumentRepository customerDocumentRepository,
                                  EntityMapper entityMapper) {
        this.userRepository = userRepository;
        this.savedAddressRepository = savedAddressRepository;
        this.userRoleRepository = userRoleRepository;
        this.customerDocumentRepository = customerDocumentRepository;
        this.entityMapper = entityMapper;
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized: Authentication required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    @Transactional(readOnly = true)
    public CustomerProfileDTO getProfile() {
        User user = getAuthenticatedUser();
        return toProfileDTO(user);
    }

    public CustomerProfileDTO updateProfile(CustomerProfileDTO dto) {
        User user = getAuthenticatedUser();
        // Prevent editing sensitive identity fields or roles
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            user.setName(dto.getName().trim());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone().trim());
        }
        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress().trim());
        }
        if (dto.getCity() != null) {
            user.setCity(dto.getCity().trim());
        }
        if (dto.getState() != null) {
            user.setState(dto.getState().trim());
        }
        if (dto.getPincode() != null) {
            user.setPincode(dto.getPincode().trim());
        }
        User saved = userRepository.save(user);
        return toProfileDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedAddressDTO> getSavedAddresses() {
        User user = getAuthenticatedUser();
        return savedAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
                .map(entityMapper::toSavedAddressDTO)
                .toList();
    }

    public SavedAddressDTO addSavedAddress(SavedAddressDTO dto) {
        User user = getAuthenticatedUser();

        // If this is set as default or is first address, adjust existing defaults
        if (Boolean.TRUE.equals(dto.getIsDefault()) || savedAddressRepository.findByUserId(user.getId()).isEmpty()) {
            clearExistingDefault(user.getId());
            dto.setIsDefault(true);
        }

        SavedAddress address = new SavedAddress();
        address.setUser(user);
        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setLandmark(dto.getLandmark());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        address.setAddressType(dto.getAddressType() != null ? dto.getAddressType() : "HOME");
        address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);

        SavedAddress saved = savedAddressRepository.save(address);
        return entityMapper.toSavedAddressDTO(saved);
    }

    public SavedAddressDTO updateSavedAddress(Long id, SavedAddressDTO dto) {
        User user = getAuthenticatedUser();
        SavedAddress address = savedAddressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Address not found or not owned by authenticated user"));

        if (Boolean.TRUE.equals(dto.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            clearExistingDefault(user.getId());
            address.setIsDefault(true);
        }

        address.setAddressLine1(dto.getAddressLine1());
        address.setAddressLine2(dto.getAddressLine2());
        address.setLandmark(dto.getLandmark());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setPincode(dto.getPincode());
        if (dto.getAddressType() != null) {
            address.setAddressType(dto.getAddressType());
        }

        SavedAddress saved = savedAddressRepository.save(address);
        return entityMapper.toSavedAddressDTO(saved);
    }

    public void setDefaultAddress(Long id) {
        User user = getAuthenticatedUser();
        SavedAddress address = savedAddressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Address not found or not owned by authenticated user"));

        clearExistingDefault(user.getId());
        address.setIsDefault(true);
        savedAddressRepository.save(address);
    }

    public void deleteSavedAddress(Long id) {
        User user = getAuthenticatedUser();
        SavedAddress address = savedAddressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new AccessDeniedException("Address not found or not owned by authenticated user"));
        savedAddressRepository.delete(address);
    }

    private void clearExistingDefault(Long userId) {
        savedAddressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(existing -> {
                    existing.setIsDefault(false);
                    savedAddressRepository.save(existing);
                });
    }

    private CustomerProfileDTO toProfileDTO(User user) {
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setConsumerNumber(user.getConsumerNumber());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setPincode(user.getPincode());

        List<UserRole> userRoles = userRoleRepository.findByUser(user);
        List<String> roles = userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .map(Role::getName)
                .toList();
        dto.setRoles(roles);

        // Check if all 4 KYC documents are approved
        long approvedDocs = customerDocumentRepository.countByCustomerIdAndStatus(user.getId(), "APPROVED");
        dto.setKycVerified(approvedDocs >= 4);

        return dto;
    }
}
