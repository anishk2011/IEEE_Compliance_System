package com.ieee.pdfchecker.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paper_versions")
public class PaperVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFilePath;

    @Column(nullable = false)
    private boolean overallCompliant;

    @Column(nullable = false)
    private Integer score;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "paperVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleResult> ruleResults = new ArrayList<>();

    protected PaperVersion() {
    }

    public PaperVersion(Integer versionNumber, String originalFileName, String storedFilePath,
                        boolean overallCompliant, Integer score, LocalDateTime createdAt) {
        this.versionNumber = versionNumber;
        this.originalFileName = originalFileName;
        this.storedFilePath = storedFilePath;
        this.overallCompliant = overallCompliant;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Paper getPaper() {
        return paper;
    }

    public void setPaper(Paper paper) {
        this.paper = paper;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFilePath() {
        return storedFilePath;
    }

    public void setStoredFilePath(String storedFilePath) {
        this.storedFilePath = storedFilePath;
    }

    public boolean isOverallCompliant() {
        return overallCompliant;
    }

    public void setOverallCompliant(boolean overallCompliant) {
        this.overallCompliant = overallCompliant;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<RuleResult> getRuleResults() {
        return ruleResults;
    }

    public void addRuleResult(RuleResult ruleResult) {
        ruleResults.add(ruleResult);
        ruleResult.setPaperVersion(this);
    }

    public void removeRuleResult(RuleResult ruleResult) {
        ruleResults.remove(ruleResult);
        ruleResult.setPaperVersion(null);
    }
}
