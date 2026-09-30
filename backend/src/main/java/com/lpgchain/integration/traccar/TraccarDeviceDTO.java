package com.lpgchain.integration.traccar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TraccarDeviceDTO {

    private Long id;
    private String name;
    private String uniqueId;
    private String status;
    private String lastUpdate;
    private Long positionId;

    public TraccarDeviceDTO() {
    }

    public TraccarDeviceDTO(Long id, String name, String uniqueId, String status, Long positionId) {
        this.id = id;
        this.name = name;
        this.uniqueId = uniqueId;
        this.status = status;
        this.positionId = positionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }
}
