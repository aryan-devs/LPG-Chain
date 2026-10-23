package com.lpgchain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "KYC compliance summary showing requirement status and remaining uploads")
public class KycSummaryDTO {

    private int totalRequired;
    private int submittedCount;
    private int approvedCount;
    private int rejectedCount;
    private int pendingCount;
    private int remainingRequiredCount;
    private boolean allApproved;
    private String statusMessage;
    private List<CustomerDocumentDTO> documents;

    public KycSummaryDTO() {}

    public int getTotalRequired() { return totalRequired; }
    public void setTotalRequired(int totalRequired) { this.totalRequired = totalRequired; }

    public int getRequiredCount() { return totalRequired; }
    public void setRequiredCount(int requiredCount) { this.totalRequired = requiredCount; }

    public int getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(int submittedCount) { this.submittedCount = submittedCount; }

    public int getApprovedCount() { return approvedCount; }
    public void setApprovedCount(int approvedCount) { this.approvedCount = approvedCount; }

    public int getRejectedCount() { return rejectedCount; }
    public void setRejectedCount(int rejectedCount) { this.rejectedCount = rejectedCount; }

    public int getPendingCount() { return pendingCount; }
    public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }

    public int getPendingReviewCount() { return pendingCount; }
    public void setPendingReviewCount(int pendingReviewCount) { this.pendingCount = pendingReviewCount; }

    public int getRemainingRequiredCount() { return remainingRequiredCount; }
    public void setRemainingRequiredCount(int remainingRequiredCount) { this.remainingRequiredCount = remainingRequiredCount; }

    public boolean isAllApproved() { return allApproved; }
    public void setAllApproved(boolean allApproved) { this.allApproved = allApproved; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public List<CustomerDocumentDTO> getDocuments() { return documents; }
    public void setDocuments(List<CustomerDocumentDTO> documents) { this.documents = documents; }
}
