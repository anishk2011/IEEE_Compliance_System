package com.ieee.pdfchecker.dto;

import java.time.LocalDateTime;

public class PaperSummaryDto {

    private final Long paperId;
    private final String displayName;
    private final LocalDateTime createdAt;
    private final Integer latestVersionNumber;
    private final Integer latestScore;
    private final int totalVersions;

    public PaperSummaryDto(Long paperId, String displayName, LocalDateTime createdAt,
                           Integer latestVersionNumber, Integer latestScore, int totalVersions) {
        this.paperId = paperId;
        this.displayName = displayName;
        this.createdAt = createdAt;
        this.latestVersionNumber = latestVersionNumber;
        this.latestScore = latestScore;
        this.totalVersions = totalVersions;
    }

    public Long getPaperId() {
        return paperId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getLatestVersionNumber() {
        return latestVersionNumber;
    }

    public Integer getLatestScore() {
        return latestScore;
    }

    public int getTotalVersions() {
        return totalVersions;
    }
}
