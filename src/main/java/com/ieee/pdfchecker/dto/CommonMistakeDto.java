package com.ieee.pdfchecker.dto;

public class CommonMistakeDto {

    private final String ruleCode;
    private final String ruleName;
    private final long failCount;
    private final long affectedPaperCount;
    private final String latestSuggestion;

    public CommonMistakeDto(String ruleCode, String ruleName, long failCount,
                            long affectedPaperCount, String latestSuggestion) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.failCount = failCount;
        this.affectedPaperCount = affectedPaperCount;
        this.latestSuggestion = latestSuggestion;
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

    public long getAffectedPaperCount() {
        return affectedPaperCount;
    }

    public String getLatestSuggestion() {
        return latestSuggestion;
    }
}
