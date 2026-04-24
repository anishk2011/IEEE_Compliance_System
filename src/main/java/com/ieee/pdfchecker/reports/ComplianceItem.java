package com.ieee.pdfchecker.reports;

public class ComplianceItem {
    private String status;  // "pass", "fail"
    private String rule;
    private String ruleCode;
    private String message;
    private String suggestion;

    public ComplianceItem(String status, String rule, String ruleCode, String message) {
        this.status = status;
        this.rule = rule;
        this.ruleCode = ruleCode;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getRule() {
        return rule;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
