package com.ieee.pdfchecker.services;

import com.ieee.pdfchecker.dto.PaperProgressDto;
import com.ieee.pdfchecker.dto.PaperSummaryDto;
import com.ieee.pdfchecker.dto.PaperVersionDto;
import com.ieee.pdfchecker.dto.RuleComparisonDto;
import com.ieee.pdfchecker.dto.SavedReportDto;
import com.ieee.pdfchecker.dto.SavedRuleResultDto;
import com.ieee.pdfchecker.dto.VersionComparisonDto;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class PaperQueryService {

    private final PaperRepository paperRepository;
    private final PaperVersionRepository paperVersionRepository;
    private final RuleResultRepository ruleResultRepository;

    public PaperQueryService(PaperRepository paperRepository,
                             PaperVersionRepository paperVersionRepository,
                             RuleResultRepository ruleResultRepository) {
        this.paperRepository = paperRepository;
        this.paperVersionRepository = paperVersionRepository;
        this.ruleResultRepository = ruleResultRepository;
    }

    @Transactional
    public List<PaperSummaryDto> getAllPapers() {
        return paperRepository.findAll().stream()
                .map(this::toPaperSummary)
                .toList();
    }

    @Transactional
    public List<PaperVersionDto> getVersions(Long paperId) {
        ensurePaperExists(paperId);
        return paperVersionRepository.findByPaperIdOrderByVersionNumberDesc(paperId).stream()
                .map(this::toPaperVersionDto)
                .toList();
    }

    @Transactional
    public SavedReportDto getSavedReport(Long paperId, Long versionId) {
        PaperVersion version = getPaperVersion(paperId, versionId);
        List<SavedRuleResultDto> items = ruleResultRepository.findByPaperVersionIdOrderByIdAsc(version.getId()).stream()
                .map(this::toSavedRuleResultDto)
                .toList();

        return new SavedReportDto(
                paperId,
                version.getId(),
                version.getScore(),
                version.isOverallCompliant(),
                items
        );
    }

    @Transactional
    public List<PaperProgressDto> getProgress(Long paperId) {
        ensurePaperExists(paperId);
        return paperVersionRepository.findByPaperIdOrderByVersionNumberAsc(paperId).stream()
                .map(version -> new PaperProgressDto(
                        version.getVersionNumber(),
                        version.getScore(),
                        version.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public VersionComparisonDto compareVersions(Long paperId, Integer fromVersion, Integer toVersion) {
        ensurePaperExists(paperId);

        PaperVersion sourceVersion = paperVersionRepository.findByPaperIdAndVersionNumber(paperId, fromVersion)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + fromVersion));
        PaperVersion targetVersion = paperVersionRepository.findByPaperIdAndVersionNumber(paperId, toVersion)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + toVersion));

        Map<String, RuleResult> fromRules = indexByRuleCode(ruleResultRepository.findByPaperVersionIdOrderByIdAsc(sourceVersion.getId()));
        Map<String, RuleResult> toRules = indexByRuleCode(ruleResultRepository.findByPaperVersionIdOrderByIdAsc(targetVersion.getId()));

        Set<String> allRuleCodes = new TreeSet<>();
        allRuleCodes.addAll(fromRules.keySet());
        allRuleCodes.addAll(toRules.keySet());

        List<RuleComparisonDto> improved = new ArrayList<>();
        List<RuleComparisonDto> worsened = new ArrayList<>();
        List<RuleComparisonDto> unchanged = new ArrayList<>();

        for (String ruleCode : allRuleCodes) {
            RuleResult fromResult = fromRules.get(ruleCode);
            RuleResult toResult = toRules.get(ruleCode);

            RuleComparisonDto comparison = new RuleComparisonDto(
                    ruleCode,
                    resolveRuleName(fromResult, toResult),
                    statusOf(fromResult),
                    statusOf(toResult)
            );

            if (fromResult == null || toResult == null) {
                unchanged.add(comparison);
                continue;
            }

            int fromRank = statusRank(fromResult.getStatus());
            int toRank = statusRank(toResult.getStatus());

            if (toRank > fromRank) {
                improved.add(comparison);
            } else if (toRank < fromRank) {
                worsened.add(comparison);
            } else {
                unchanged.add(comparison);
            }
        }

        return new VersionComparisonDto(paperId, fromVersion, toVersion, improved, worsened, unchanged);
    }

    private PaperSummaryDto toPaperSummary(Paper paper) {
        List<PaperVersion> versions = paperVersionRepository.findByPaperIdOrderByVersionNumberDesc(paper.getId());
        PaperVersion latest = versions.isEmpty() ? null : versions.get(0);

        return new PaperSummaryDto(
                paper.getId(),
                paper.getDisplayName(),
                paper.getCreatedAt(),
                latest != null ? latest.getVersionNumber() : null,
                latest != null ? latest.getScore() : null,
                versions.size()
        );
    }

    private PaperVersionDto toPaperVersionDto(PaperVersion version) {
        return new PaperVersionDto(
                version.getId(),
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getCreatedAt(),
                version.isOverallCompliant(),
                version.getScore()
        );
    }

    private SavedRuleResultDto toSavedRuleResultDto(RuleResult result) {
        return new SavedRuleResultDto(
                result.getId(),
                result.getRuleCode(),
                result.getRuleName(),
                result.getStatus(),
                result.getMessage(),
                result.getSuggestion(),
                result.getSeverity()
        );
    }

    private PaperVersion getPaperVersion(Long paperId, Long versionId) {
        ensurePaperExists(paperId);
        return paperVersionRepository.findByPaperIdAndId(paperId, versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found for paper: " + paperId));
    }

    private void ensurePaperExists(Long paperId) {
        if (!paperRepository.existsById(paperId)) {
            throw new IllegalArgumentException("Paper not found for id: " + paperId);
        }
    }

    private Map<String, RuleResult> indexByRuleCode(List<RuleResult> results) {
        Map<String, RuleResult> indexed = new LinkedHashMap<>();
        for (RuleResult result : results) {
            indexed.putIfAbsent(result.getRuleCode(), result);
        }
        return indexed;
    }

    private String resolveRuleName(RuleResult fromResult, RuleResult toResult) {
        if (toResult != null && toResult.getRuleName() != null) {
            return toResult.getRuleName();
        }
        if (fromResult != null) {
            return fromResult.getRuleName();
        }
        if (toResult != null) {
            return RuleCode.fromCode(toResult.getRuleCode()).getRuleName();
        }
        if (fromResult != null) {
            return RuleCode.fromCode(fromResult.getRuleCode()).getRuleName();
        }
        return RuleCode.GENERAL.getRuleName();
    }

    private String statusOf(RuleResult result) {
        return result != null ? result.getStatus() : "MISSING";
    }

    private int statusRank(String status) {
        if ("PASS".equalsIgnoreCase(status)) {
            return 1;
        }
        if ("FAIL".equalsIgnoreCase(status)) {
            return 0;
        }
        return -1;
    }
}
