package com.ieee.pdfchecker.dto;

public class PaperMistakeBreakdownDto {

    private final String ruleCode;
    private final String ruleName;
    private final long failCount;
    private final Integer firstSeenVersion;
    private final Integer lastSeenVersion;

    public PaperMistakeBreakdownDto(String ruleCode, String ruleName, long failCount,
                                    Integer firstSeenVersion, Integer lastSeenVersion) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.failCount = failCount;
        this.firstSeenVersion = firstSeenVersion;
        this.lastSeenVersion = lastSeenVersion;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public long getFailCount() {
        return failCount;
    }

    public Integer getFirstSeenVersion() {
        return firstSeenVersion;
    }

    public Integer getLastSeenVersion() {
        return lastSeenVersion;
    }
}
