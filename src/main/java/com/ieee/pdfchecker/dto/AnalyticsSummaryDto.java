package com.ieee.pdfchecker.dto;

public class AnalyticsSummaryDto {

    private final long totalPapers;
    private final long totalVersions;
    private final long totalChecks;
    private final double averageScore;
    private final double overallComplianceRate;
    private final long totalFailedRuleResults;
    private final long totalPassedRuleResults;
    private final String mostCommonFailedRuleCode;
    private final String mostCommonFailedRuleName;

    public AnalyticsSummaryDto(long totalPapers, long totalVersions, long totalChecks,
                               double averageScore, double overallComplianceRate,
                               long totalFailedRuleResults, long totalPassedRuleResults,
                               String mostCommonFailedRuleCode, String mostCommonFailedRuleName) {
        this.totalPapers = totalPapers;
        this.totalVersions = totalVersions;
        this.totalChecks = totalChecks;
        this.averageScore = averageScore;
        this.overallComplianceRate = overallComplianceRate;
        this.totalFailedRuleResults = totalFailedRuleResults;
        this.totalPassedRuleResults = totalPassedRuleResults;
        this.mostCommonFailedRuleCode = mostCommonFailedRuleCode;
        this.mostCommonFailedRuleName = mostCommonFailedRuleName;
    }

    public long getTotalPapers() {
        return totalPapers;
    }

    public long getTotalVersions() {
        return totalVersions;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getOverallComplianceRate() {
        return overallComplianceRate;
    }

    public long getTotalFailedRuleResults() {
        return totalFailedRuleResults;
    }

    public long getTotalPassedRuleResults() {
        return totalPassedRuleResults;
    }

    public String getMostCommonFailedRuleCode() {
        return mostCommonFailedRuleCode;
    }

    public String getMostCommonFailedRuleName() {
        return mostCommonFailedRuleName;
    }
}
