package com.ieee.pdfchecker.reports;

public class ComplianceItem {
    public static final String STATUS_PASS = "pass";
    public static final String STATUS_FAIL = "fail";
    public static final String STATUS_WARN = "warn";
    public static final String STATUS_NA = "na";

    private String status;
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

    public boolean isPass() {
        return STATUS_PASS.equalsIgnoreCase(status);
    }

    public boolean isFail() {
        return STATUS_FAIL.equalsIgnoreCase(status);
    }

    public boolean isWarn() {
        return STATUS_WARN.equalsIgnoreCase(status);
    }

    public boolean isNotApplicable() {
        return STATUS_NA.equalsIgnoreCase(status);
    }
}
