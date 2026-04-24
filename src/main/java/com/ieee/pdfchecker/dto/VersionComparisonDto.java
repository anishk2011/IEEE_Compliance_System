package com.ieee.pdfchecker.dto;

import java.util.List;

public class VersionComparisonDto {

    private final Long paperId;
    private final Integer fromVersion;
    private final Integer toVersion;
    private final List<RuleComparisonDto> improved;
    private final List<RuleComparisonDto> worsened;
    private final List<RuleComparisonDto> unchanged;

    public VersionComparisonDto(Long paperId, Integer fromVersion, Integer toVersion,
                                List<RuleComparisonDto> improved,
                                List<RuleComparisonDto> worsened,
                                List<RuleComparisonDto> unchanged) {
        this.paperId = paperId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.improved = improved;
        this.worsened = worsened;
        this.unchanged = unchanged;
    }

    public Long getPaperId() {
        return paperId;
    }

    public Integer getFromVersion() {
        return fromVersion;
    }

    public Integer getToVersion() {
        return toVersion;
    }

    public List<RuleComparisonDto> getImproved() {
        return improved;
    }

    public List<RuleComparisonDto> getWorsened() {
        return worsened;
    }

    public List<RuleComparisonDto> getUnchanged() {
        return unchanged;
    }
}
