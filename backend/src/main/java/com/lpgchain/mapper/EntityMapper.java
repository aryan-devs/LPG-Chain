package com.lpgchain.mapper;

import com.lpgchain.dto.BookingResponseDTO;
import com.lpgchain.dto.UserRequestDTO;
import com.lpgchain.dto.UserResponseDTO;
import com.lpgchain.entity.Booking;
import com.lpgchain.entity.Distributor;
import com.lpgchain.entity.User;
import org.springframework.stereotype.Component;

/**
 * EntityMapper — Centralized mapper for converting JPA entities to response DTOs
 * and request DTOs to entities.
 *
 * Keeps mapping logic decoupled from controllers and entity definitions.
 */
@Component
public class EntityMapper {

    /**
     * Maps a User entity to UserResponseDTO, excluding sensitive fields.
     */
    public UserResponseDTO toUserResponseDTO(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        return dto;
    }

    /**
     * Maps a UserRequestDTO to a new User entity.
     */
    public User toUserEntity(UserRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        return user;
    }

    /**
     * Maps a Booking entity to BookingResponseDTO, flattening customer and distributor associations.
     */
    public BookingResponseDTO toBookingResponseDTO(Booking booking) {
        if (booking == null) {
            return null;
        }

        User customer = booking.getCustomer();
        Distributor distributor = booking.getDistributor();

        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setId(booking.getId());
        dto.setCustomerId(customer != null ? customer.getId() : null);
        dto.setCustomerName(customer != null ? customer.getName() : null);
        dto.setDistributorId(distributor != null ? distributor.getId() : null);
        dto.setDistributorName(distributor != null ? distributor.getAgencyName() : null);
        dto.setDeliveryAddress(booking.getDeliveryAddress());
        dto.setBookingDate(booking.getBookingDate());
        dto.setQuantity(booking.getQuantity());
        dto.setStatus(booking.getStatus());
        return dto;
    }

    /**
     * Maps a DistributorMembership entity to DistributorMembershipResponseDTO.
     */
    public com.lpgchain.dto.DistributorMembershipResponseDTO toDistributorMembershipResponseDTO(com.lpgchain.entity.DistributorMembership membership) {
        if (membership == null) {
            return null;
        }

        User user = membership.getUser();
        Distributor distributor = membership.getDistributor();

        com.lpgchain.dto.DistributorMembershipResponseDTO dto = new com.lpgchain.dto.DistributorMembershipResponseDTO();
        dto.setId(membership.getId());
        dto.setUserId(user != null ? user.getId() : null);
        dto.setUserName(user != null ? user.getName() : null);
        dto.setUserEmail(user != null ? user.getEmail() : null);
        dto.setDistributorId(distributor != null ? distributor.getId() : null);
        dto.setDistributorName(distributor != null ? distributor.getAgencyName() : null);
        dto.setMembershipRole(membership.getMembershipRole());
        dto.setStatus(membership.getStatus());
        return dto;
    }
}
