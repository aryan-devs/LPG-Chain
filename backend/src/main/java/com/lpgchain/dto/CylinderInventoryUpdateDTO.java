package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to update physical stock counts or record an adjustment")
public class CylinderInventoryUpdateDTO {

    @NotBlank(message = "cylinderCategory is required")
    @Schema(description = "Cylinder category code", example = "DOMESTIC_14_2")
    private String cylinderCategory;

    @NotNull(message = "filledStock cannot be null")
    @Min(value = 0, message = "filledStock must be non-negative")
    @Schema(description = "Current filled physical cylinders count", example = "420")
    private Integer filledStock;

    @NotNull(message = "emptyStock cannot be null")
    @Min(value = 0, message = "emptyStock must be non-negative")
    @Schema(description = "Current empty cylinders count", example = "115")
    private Integer emptyStock;

    @Min(value = 0, message = "damagedStock must be non-negative")
    @Schema(description = "Damaged or quarantined cylinders count", example = "4")
    private Integer damagedStock = 0;

    @Schema(description = "Movement or adjustment reason", example = "Depot weekly physical stock count")
    private String reason;

    @Schema(description = "Reference receipt or invoice number", example = "INV-DEPOT-0912")
    private String referenceNumber;

    public CylinderInventoryUpdateDTO() {}

    public String getCylinderCategory() { return cylinderCategory; }
    public void setCylinderCategory(String cylinderCategory) { this.cylinderCategory = cylinderCategory; }

    public Integer getFilledStock() { return filledStock; }
    public void setFilledStock(Integer filledStock) { this.filledStock = filledStock; }

    public Integer getEmptyStock() { return emptyStock; }
    public void setEmptyStock(Integer emptyStock) { this.emptyStock = emptyStock; }

    public Integer getDamagedStock() { return damagedStock; }
    public void setDamagedStock(Integer damagedStock) { this.damagedStock = damagedStock; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
}
