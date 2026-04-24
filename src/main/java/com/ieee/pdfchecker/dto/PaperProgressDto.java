package com.ieee.pdfchecker.dto;

import java.time.LocalDateTime;

public class PaperProgressDto {

    private final Integer versionNumber;
    private final Integer score;
    private final LocalDateTime createdAt;

    public PaperProgressDto(Integer versionNumber, Integer score, LocalDateTime createdAt) {
        this.versionNumber = versionNumber;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public Integer getScore() {
        return score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
