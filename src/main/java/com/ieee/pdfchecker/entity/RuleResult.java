package com.ieee.pdfchecker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rule_results")
public class RuleResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_version_id", nullable = false)
    private PaperVersion paperVersion;

    @Column(nullable = false, length = 100)
    private String ruleCode;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false, length = 20)
    private String status;

    @Lob
    @Column(nullable = false)
    private String message;

    @Lob
    private String suggestion;

    @Column(length = 20)
    private String severity;

    protected RuleResult() {
    }

    public RuleResult(String ruleCode, String ruleName, String status, String message,
                      String suggestion, String severity) {
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

    public PaperVersion getPaperVersion() {
        return paperVersion;
    }

    public void setPaperVersion(PaperVersion paperVersion) {
        this.paperVersion = paperVersion;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
