package com.ieee.pdfchecker.dto;

import java.util.List;

public class SavedReportDto {

    private final Long paperId;
    private final Long versionId;
    private final Integer score;
    private final boolean overallCompliant;
    private final List<SavedRuleResultDto> items;

    public SavedReportDto(Long paperId, Long versionId, Integer score,
                          boolean overallCompliant, List<SavedRuleResultDto> items) {
        this.paperId = paperId;
        this.versionId = versionId;
        this.score = score;
        this.overallCompliant = overallCompliant;
        this.items = items;
    }

    public Long getPaperId() {
        return paperId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public Integer getScore() {
        return score;
    }

    public boolean isOverallCompliant() {
        return overallCompliant;
    }

    public List<SavedRuleResultDto> getItems() {
        return items;
    }
}
