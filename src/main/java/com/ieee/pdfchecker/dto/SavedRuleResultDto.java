package com.ieee.pdfchecker.dto;

public class SavedRuleResultDto {

    private final Long id;
    private final String ruleCode;
    private final String ruleName;
    private final String status;
    private final String message;
    private final String suggestion;
    private final String severity;

    public SavedRuleResultDto(Long id, String ruleCode, String ruleName, String status,
                              String message, String suggestion, String severity) {
        this.id = id;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.status = status;
        this.message = message;
        this.suggestion = suggestion;
        this.severity = severity;
    }

    public Long getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public String getSeverity() {
        return severity;
    }
}
