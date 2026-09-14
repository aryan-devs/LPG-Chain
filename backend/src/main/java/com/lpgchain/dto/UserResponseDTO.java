package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * UserResponseDTO — Safe data transfer object for User API responses.
 *
 * Excludes password, hash salts, and internal security credentials.
 */
@Data
@Schema(description = "Safe public profile information for a User")
public class UserResponseDTO {

    @Schema(description = "Unique user ID", example = "1")
    private Long id;

    @Schema(description = "User's full name", example = "John Doe")
    private String name;

    @Schema(description = "User's email address", example = "john.doe@gmail.com")
    private String email;

    @Schema(description = "User's phone number", example = "+91 9876543210")
    private String phone;

    @Schema(description = "User's primary delivery address", example = "42 Park Street, Kolkata")
    private String address;

    public UserResponseDTO() {
    }

    public UserResponseDTO(Long id, String name, String email, String phone, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
