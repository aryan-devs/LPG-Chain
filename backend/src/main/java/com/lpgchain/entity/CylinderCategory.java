package com.lpgchain.entity;

/**
 * CylinderCategory — Canonical cylinder specification standards supported by LPG-Chain.
 * Categorizes cylinders by consumer market segment (Domestic vs Commercial) and nominal capacity in kg.
 */
public enum CylinderCategory {
    DOMESTIC_14_2("DOMESTIC_14_2", "Domestic 14.2 kg", 14.2, "DOMESTIC"),
    DOMESTIC_5_0("DOMESTIC_5_0", "Domestic 5 kg", 5.0, "DOMESTIC"),
    COMMERCIAL_19_0("COMMERCIAL_19_0", "Commercial 19 kg", 19.0, "COMMERCIAL");

    private final String code;
    private final String displayName;
    private final Double capacityKg;
    private final String serviceType;

    CylinderCategory(String code, String displayName, Double capacityKg, String serviceType) {
        this.code = code;
        this.displayName = displayName;
        this.capacityKg = capacityKg;
        this.serviceType = serviceType;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Double getCapacityKg() {
        return capacityKg;
    }

    public String getServiceType() {
        return serviceType;
    }

    /**
     * Resolves a category string or code to the canonical CylinderCategory.
     * Defaults to DOMESTIC_14_2 if null or empty.
     */
    public static CylinderCategory fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DOMESTIC_14_2;
        }

        String normalized = code.trim().toUpperCase().replace(" ", "_").replace(".", "_");
        for (CylinderCategory cat : values()) {
            if (cat.code.equalsIgnoreCase(normalized) || cat.name().equalsIgnoreCase(normalized)) {
                return cat;
            }
        }

        // Fuzzy resolution for capacities e.g. "14.2", "5", "19"
        if (normalized.contains("14_2") || normalized.contains("14")) {
            return DOMESTIC_14_2;
        }
        if (normalized.contains("5_0") || normalized.contains("5")) {
            return DOMESTIC_5_0;
        }
        if (normalized.contains("19_0") || normalized.contains("19")) {
            return COMMERCIAL_19_0;
        }

        throw new IllegalArgumentException("Unknown cylinder category: '" + code + "'. Supported categories: DOMESTIC_14_2, DOMESTIC_5_0, COMMERCIAL_19_0");
    }
}
