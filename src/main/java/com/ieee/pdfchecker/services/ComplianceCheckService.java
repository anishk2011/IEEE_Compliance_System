package com.ieee.pdfchecker.services;

import com.ieee.pdfchecker.entity.Paper;
import com.ieee.pdfchecker.entity.PaperVersion;
import com.ieee.pdfchecker.entity.RuleResult;
import com.ieee.pdfchecker.reports.ComplianceItem;
import com.ieee.pdfchecker.reports.ComplianceReport;
import com.ieee.pdfchecker.repository.PaperRepository;
import com.ieee.pdfchecker.repository.PaperVersionRepository;
import com.ieee.pdfchecker.repository.RuleResultRepository;
import com.ieee.pdfchecker.rules.RuleCode;
import com.ieee.pdfchecker.rules.RuleEngine;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComplianceCheckService {

    private final RuleEngine ruleEngine;
    private final PaperRepository paperRepository;
    private final PaperVersionRepository paperVersionRepository;
    private final RuleResultRepository ruleResultRepository;
    private final SuggestionService suggestionService;

    public ComplianceCheckService(RuleEngine ruleEngine,
                                  PaperRepository paperRepository,
                                  PaperVersionRepository paperVersionRepository,
                                  RuleResultRepository ruleResultRepository,
                                  SuggestionService suggestionService) {
        this.ruleEngine = ruleEngine;
        this.paperRepository = paperRepository;
        this.paperVersionRepository = paperVersionRepository;
        this.ruleResultRepository = ruleResultRepository;
        this.suggestionService = suggestionService;
    }

    @Transactional
    public ComplianceReport analyzeAndSave(File file, String originalFileName, Long paperId) {
        ComplianceReport report = ruleEngine.checkCompliance(file);

        String resolvedFileName = resolveFileName(originalFileName, file);
        Paper paper = resolvePaper(paperId, resolvedFileName);
        int nextVersionNumber = determineNextVersionNumber(paper.getId());
        int score = calculateScore(report.getItems());

        PaperVersion version = new PaperVersion(
                nextVersionNumber,
                resolvedFileName,
                file.getAbsolutePath(),
                report.isCompliant(),
                score,
                LocalDateTime.now()
        );
        version.setPaper(paper);
        PaperVersion savedVersion = paperVersionRepository.save(version);

        List<RuleResult> ruleResults = report.getItems().stream()
                .map(item -> toRuleResult(savedVersion, item))
                .toList();
        ruleResultRepository.saveAll(ruleResults);

        report.setPaperId(paper.getId());
        report.setVersionId(savedVersion.getId());
        report.setScore(score);

        return report;
    }

    private Paper resolvePaper(Long paperId, String displayName) {
        if (paperId == null) {
            Paper paper = new Paper(displayName, LocalDateTime.now());
            return paperRepository.save(paper);
        }

        return paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found for id: " + paperId));
    }

    private int determineNextVersionNumber(Long paperId) {
        return paperVersionRepository.findTopByPaperIdOrderByVersionNumberDesc(paperId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    private int calculateScore(List<ComplianceItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        long passed = items.stream()
                .filter(item -> "pass".equalsIgnoreCase(item.getStatus()))
                .count();

        return (int) Math.round((passed * 100.0) / items.size());
    }

    private RuleResult toRuleResult(PaperVersion version, ComplianceItem item) {
        RuleCode ruleCode = RuleCode.fromCode(item.getRuleCode());
        if (ruleCode == RuleCode.GENERAL) {
            ruleCode = suggestionService.resolveRuleCode(item.getRule(), item.getMessage());
        }

        String suggestion = suggestionService.getSuggestion(ruleCode, item.getStatus());
        item.setSuggestion(suggestion);

        RuleResult result = new RuleResult(
                ruleCode.getCode(),
                ruleCode.getRuleName(),
                item.getStatus().toUpperCase(),
                item.getMessage(),
                suggestion,
                suggestionService.getSeverity(item.getStatus())
        );
        result.setPaperVersion(version);
        return result;
    }

    private String resolveFileName(String originalFileName, File file) {
        if (originalFileName != null && !originalFileName.isBlank()) {
            return originalFileName;
        }
        return file.getName();
    }
}
