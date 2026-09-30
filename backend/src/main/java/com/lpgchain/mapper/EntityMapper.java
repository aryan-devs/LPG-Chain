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

    /**
     * Maps a Driver entity to DriverResponseDTO.
     */
    public com.lpgchain.dto.DriverResponseDTO toDriverResponseDTO(com.lpgchain.entity.Driver driver) {
        if (driver == null) {
            return null;
        }

        Distributor distributor = driver.getDistributor();
        com.lpgchain.dto.DriverResponseDTO dto = new com.lpgchain.dto.DriverResponseDTO();
        dto.setId(driver.getId());
        dto.setName(driver.getName());
        dto.setPhone(driver.getPhone());
        dto.setLicenseNumber(driver.getLicenseNumber());
        dto.setStatus(driver.getStatus());
        dto.setDistributorId(distributor != null ? distributor.getId() : null);
        dto.setDistributorName(distributor != null ? distributor.getAgencyName() : null);
        return dto;
    }

    /**
     * Maps a Vehicle entity to VehicleResponseDTO.
     */
    public com.lpgchain.dto.VehicleResponseDTO toVehicleResponseDTO(com.lpgchain.entity.Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }

        Distributor distributor = vehicle.getDistributor();
        com.lpgchain.dto.VehicleResponseDTO dto = new com.lpgchain.dto.VehicleResponseDTO();
        dto.setId(vehicle.getId());
        dto.setVehicleNumber(vehicle.getVehicleNumber());
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setCapacity(vehicle.getCapacity());
        dto.setStatus(vehicle.getStatus());
        dto.setDistributorId(distributor != null ? distributor.getId() : null);
        dto.setDistributorName(distributor != null ? distributor.getAgencyName() : null);
        dto.setTraccarDeviceId(vehicle.getTraccarDeviceId());
        return dto;
    }

    /**
     * Maps a Delivery entity to DeliveryResponseDTO, flattening associations cleanly.
     */
    public com.lpgchain.dto.DeliveryResponseDTO toDeliveryResponseDTO(com.lpgchain.entity.Delivery delivery) {
        if (delivery == null) {
            return null;
        }

        Booking booking = delivery.getBooking();
        User customer = booking != null ? booking.getCustomer() : null;
        com.lpgchain.entity.Driver driver = delivery.getDriver();
        com.lpgchain.entity.Vehicle vehicle = delivery.getVehicle();
        Distributor distributor = delivery.getDistributor();

        com.lpgchain.dto.DeliveryResponseDTO dto = new com.lpgchain.dto.DeliveryResponseDTO();
        dto.setId(delivery.getId());
        dto.setBookingId(booking != null ? booking.getId() : null);
        dto.setCustomerId(customer != null ? customer.getId() : null);
        dto.setCustomerName(customer != null ? customer.getName() : null);
        dto.setDriverId(driver != null ? driver.getId() : null);
        dto.setDriverName(driver != null ? driver.getName() : null);
        dto.setVehicleId(vehicle != null ? vehicle.getId() : null);
        dto.setVehicleNumber(vehicle != null ? vehicle.getVehicleNumber() : null);
        dto.setDistributorId(distributor != null ? distributor.getId() : null);
        dto.setDistributorName(distributor != null ? distributor.getAgencyName() : null);
        dto.setStatus(delivery.getStatus());
        dto.setAssignedAt(delivery.getAssignedAt());
        dto.setPickedUpAt(delivery.getPickedUpAt());
        dto.setOutForDeliveryAt(delivery.getOutForDeliveryAt());
        dto.setDeliveredAt(delivery.getDeliveredAt());
        return dto;
    }

    /**
     * Maps a VehicleLocation entity to VehicleLocationResponseDTO cleanly without circular references.
     */
    public com.lpgchain.dto.VehicleLocationResponseDTO toVehicleLocationResponseDTO(com.lpgchain.entity.VehicleLocation location) {
        if (location == null) {
            return null;
        }

        com.lpgchain.entity.Vehicle vehicle = location.getVehicle();
        com.lpgchain.dto.VehicleLocationResponseDTO dto = new com.lpgchain.dto.VehicleLocationResponseDTO();
        dto.setId(location.getId());
        dto.setVehicleId(vehicle != null ? vehicle.getId() : null);
        dto.setVehicleNumber(vehicle != null ? vehicle.getVehicleNumber() : null);
        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());
        dto.setSpeed(location.getSpeed());
        dto.setHeading(location.getHeading());
        dto.setRecordedAt(location.getRecordedAt());
        return dto;
    }
}
