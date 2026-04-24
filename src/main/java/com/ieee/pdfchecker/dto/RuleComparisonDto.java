package com.ieee.pdfchecker.dto;

public class RuleComparisonDto {

    private final String ruleCode;
    private final String ruleName;
    private final String fromStatus;
    private final String toStatus;

    public RuleComparisonDto(String ruleCode, String ruleName, String fromStatus, String toStatus) {
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }
}
