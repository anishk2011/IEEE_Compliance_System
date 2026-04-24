package com.ieee.pdfchecker.services;

import com.ieee.pdfchecker.dto.AnalyticsSummaryDto;
import com.ieee.pdfchecker.dto.CommonMistakeDto;
import com.ieee.pdfchecker.dto.ImprovementTrendsDto;
import com.ieee.pdfchecker.dto.PaperMistakeBreakdownDto;
import com.ieee.pdfchecker.entity.Paper;
import com.ieee.pdfchecker.entity.PaperVersion;
import com.ieee.pdfchecker.entity.RuleResult;
import com.ieee.pdfchecker.repository.PaperRepository;
import com.ieee.pdfchecker.repository.PaperVersionRepository;
import com.ieee.pdfchecker.repository.RuleResultRepository;
import com.ieee.pdfchecker.rules.RuleCode;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final PaperRepository paperRepository;
    private final PaperVersionRepository paperVersionRepository;
    private final RuleResultRepository ruleResultRepository;

    public AnalyticsService(PaperRepository paperRepository,
                            PaperVersionRepository paperVersionRepository,
                            RuleResultRepository ruleResultRepository) {
        this.paperRepository = paperRepository;
        this.paperVersionRepository = paperVersionRepository;
        this.ruleResultRepository = ruleResultRepository;
    }

    @Transactional
    public List<CommonMistakeDto> getCommonMistakes() {
        List<PaperVersion> versions = paperVersionRepository.findAll();
        Map<Long, Long> versionToPaper = versions.stream()
                .collect(Collectors.toMap(PaperVersion::getId, version -> version.getPaper().getId()));

        Map<String, MistakeAggregate> aggregates = new LinkedHashMap<>();

        for (RuleResult result : ruleResultRepository.findAll()) {
            if (!isFail(result.getStatus())) {
                continue;
            }

            MistakeAggregate aggregate = aggregates.computeIfAbsent(
                    result.getRuleCode(),
                    ruleCode -> new MistakeAggregate(ruleCode, resolveRuleName(result))
            );

            aggregate.failCount++;
            Long paperId = versionToPaper.get(result.getPaperVersion().getId());
            if (paperId != null) {
                aggregate.affectedPaperIds.add(paperId);
            }
            if (result.getSuggestion() != null && !result.getSuggestion().isBlank()) {
                aggregate.latestSuggestion = result.getSuggestion();
            }
            if (aggregate.ruleName == null || aggregate.ruleName.isBlank()) {
                aggregate.ruleName = result.getRuleName();
            }
        }

        return aggregates.values().stream()
                .sorted(Comparator.comparingLong(MistakeAggregate::getFailCount).reversed()
                        .thenComparing(MistakeAggregate::getRuleCode))
                .map(aggregate -> new CommonMistakeDto(
                        aggregate.ruleCode,
                        aggregate.ruleName,
                        aggregate.failCount,
                        aggregate.affectedPaperIds.size(),
                        aggregate.latestSuggestion
                ))
                .toList();
    }

    @Transactional
    public AnalyticsSummaryDto getSummary() {
        long totalPapers = paperRepository.count();
        List<PaperVersion> versions = paperVersionRepository.findAll();
        List<RuleResult> ruleResults = ruleResultRepository.findAll();

        long totalVersions = versions.size();
        long totalChecks = totalVersions;
        double averageScore = totalVersions == 0
                ? 0.0
                : versions.stream().mapToInt(PaperVersion::getScore).average().orElse(0.0);
        double overallComplianceRate = totalVersions == 0
                ? 0.0
                : (versions.stream().filter(PaperVersion::isOverallCompliant).count() * 100.0) / totalVersions;

        long totalFailedRuleResults = ruleResults.stream().filter(result -> isFail(result.getStatus())).count();
        long totalPassedRuleResults = ruleResults.stream().filter(result -> isPass(result.getStatus())).count();

        CommonMistakeDto mostCommon = getCommonMistakes().stream().findFirst().orElse(null);

        return new AnalyticsSummaryDto(
                totalPapers,
                totalVersions,
                totalChecks,
                averageScore,
                overallComplianceRate,
                totalFailedRuleResults,
                totalPassedRuleResults,
                mostCommon != null ? mostCommon.getRuleCode() : null,
                mostCommon != null ? mostCommon.getRuleName() : null
        );
    }

    @Transactional
    public ImprovementTrendsDto getImprovementTrends() {
        List<Paper> papers = paperRepository.findAll();
        long totalPapersCompared = 0;
        long papersImproved = 0;
        long papersWorsened = 0;
        long papersUnchanged = 0;
        double totalDelta = 0.0;

        for (Paper paper : papers) {
            List<PaperVersion> versions = paperVersionRepository.findByPaperIdOrderByVersionNumberAsc(paper.getId());
            if (versions.size() < 2) {
                continue;
            }

            PaperVersion first = versions.get(0);
            PaperVersion latest = versions.get(versions.size() - 1);
            double delta = latest.getScore() - first.getScore();

            totalPapersCompared++;
            totalDelta += delta;

            if (delta > 0) {
                papersImproved++;
            } else if (delta < 0) {
                papersWorsened++;
            } else {
                papersUnchanged++;
            }
        }

        double averageScoreDelta = totalPapersCompared == 0 ? 0.0 : totalDelta / totalPapersCompared;

        return new ImprovementTrendsDto(
                totalPapersCompared,
                papersImproved,
                papersWorsened,
                papersUnchanged,
                averageScoreDelta
        );
    }

    @Transactional
    public List<PaperMistakeBreakdownDto> getPaperMistakeBreakdown(Long paperId) {
        if (!paperRepository.existsById(paperId)) {
            throw new IllegalArgumentException("Paper not found for id: " + paperId);
        }

        List<PaperVersion> versions = paperVersionRepository.findByPaperIdOrderByVersionNumberAsc(paperId);
        Map<String, BreakdownAggregate> aggregates = new LinkedHashMap<>();

        for (PaperVersion version : versions) {
            for (RuleResult result : ruleResultRepository.findByPaperVersionIdOrderByIdAsc(version.getId())) {
                if (!isFail(result.getStatus())) {
                    continue;
                }

                BreakdownAggregate aggregate = aggregates.computeIfAbsent(
                        result.getRuleCode(),
                        ruleCode -> new BreakdownAggregate(ruleCode, resolveRuleName(result))
                );

                aggregate.failCount++;
                aggregate.firstSeenVersion = aggregate.firstSeenVersion == null
                        ? version.getVersionNumber()
                        : Math.min(aggregate.firstSeenVersion, version.getVersionNumber());
                aggregate.lastSeenVersion = aggregate.lastSeenVersion == null
                        ? version.getVersionNumber()
                        : Math.max(aggregate.lastSeenVersion, version.getVersionNumber());

                if (aggregate.ruleName == null || aggregate.ruleName.isBlank()) {
                    aggregate.ruleName = result.getRuleName();
                }
            }
        }

        return aggregates.values().stream()
                .sorted(Comparator.comparingLong(BreakdownAggregate::getFailCount).reversed()
                        .thenComparing(BreakdownAggregate::getRuleCode))
                .map(aggregate -> new PaperMistakeBreakdownDto(
                        aggregate.ruleCode,
                        aggregate.ruleName,
                        aggregate.failCount,
                        aggregate.firstSeenVersion,
                        aggregate.lastSeenVersion
                ))
                .toList();
    }

    private boolean isFail(String status) {
        return "FAIL".equalsIgnoreCase(status);
    }

    private boolean isPass(String status) {
        return "PASS".equalsIgnoreCase(status);
    }

    private String resolveRuleName(RuleResult result) {
        if (result.getRuleName() != null && !result.getRuleName().isBlank()) {
            return result.getRuleName();
        }
        return RuleCode.fromCode(result.getRuleCode()).getRuleName();
    }

    private static final class MistakeAggregate {
        private final String ruleCode;
        private String ruleName;
        private long failCount;
        private final Set<Long> affectedPaperIds = new java.util.HashSet<>();
        private String latestSuggestion;

        private MistakeAggregate(String ruleCode, String ruleName) {
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
        }

        private long getFailCount() {
            return failCount;
        }

        private String getRuleCode() {
            return ruleCode;
        }
    }

    private static final class BreakdownAggregate {
        private final String ruleCode;
        private String ruleName;
        private long failCount;
        private Integer firstSeenVersion;
        private Integer lastSeenVersion;

        private BreakdownAggregate(String ruleCode, String ruleName) {
            this.ruleCode = ruleCode;
            this.ruleName = ruleName;
        }

        private long getFailCount() {
            return failCount;
        }

        private String getRuleCode() {
            return ruleCode;
        }
    }
}
