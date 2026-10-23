package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to confirm cash collection for Cash on Delivery booking")
public class ConfirmCodRequestDTO {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    public ConfirmCodRequestDTO() {}

    public ConfirmCodRequestDTO(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
}
