package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * UserRequestDTO — Data transfer object for creating a new User.
 */
@Data
@Schema(description = "Request payload for creating a new customer account")
public class UserRequestDTO {

    @NotBlank(message = "Name is required")
    @Schema(description = "Full name of the user", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address for login", example = "john.doe@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(description = "Account password (write-only, never returned in responses)", example = "SecretPass@123", accessMode = Schema.AccessMode.WRITE_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Contact phone number", example = "+91 9876543210")
    private String phone;

    @Schema(description = "Default delivery address", example = "42 Park Street, Kolkata")
    private String address;

    @Schema(description = "Optional role request (Note: privileged roles like ADMIN/DISTRIBUTOR are forbidden during public registration)", example = "CUSTOMER")
    private String role;

    public UserRequestDTO() {
    }

    public UserRequestDTO(String name, String email, String password, String phone, String address) {
        this(name, email, password, phone, address, null);
    }

    public UserRequestDTO(String name, String email, String password, String phone, String address, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = role;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
