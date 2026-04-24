package com.ieee.pdfchecker.dto;

import java.time.LocalDateTime;

public class PaperVersionDto {

    private final Long versionId;
    private final Integer versionNumber;
    private final String originalFileName;
    private final LocalDateTime createdAt;
    private final boolean overallCompliant;
    private final Integer score;

    public PaperVersionDto(Long versionId, Integer versionNumber, String originalFileName,
                           LocalDateTime createdAt, boolean overallCompliant, Integer score) {
        this.versionId = versionId;
        this.versionNumber = versionNumber;
        this.originalFileName = originalFileName;
        this.createdAt = createdAt;
        this.overallCompliant = overallCompliant;
        this.score = score;
    }

    public Long getVersionId() {
        return versionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isOverallCompliant() {
        return overallCompliant;
    }

    public Integer getScore() {
        return score;
    }
}
