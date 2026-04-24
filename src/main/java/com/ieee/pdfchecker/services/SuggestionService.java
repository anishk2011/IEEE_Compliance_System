package com.ieee.pdfchecker.services;

import com.ieee.pdfchecker.rules.RuleCode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SuggestionService {

    private static final Map<RuleCode, String> SUGGESTIONS = Map.of(
            RuleCode.ABSTRACT_PRESENT, "Add an Abstract heading near the beginning of the paper and include a concise summary.",
            RuleCode.FONT_COMPLIANCE, "Use Times New Roman or an IEEE-accepted equivalent font consistently throughout the document.",
            RuleCode.INTRODUCTION_PRESENT, "Include an Introduction section within the first few pages using a clear section heading.",
            RuleCode.AUTHOR_DETAILS_FORMAT, "Ensure author names, affiliations, and email details are clearly listed near the title.",
            RuleCode.COLUMN_FORMAT, "Format the main content in a clear two-column IEEE-style layout.",
            RuleCode.KEYWORDS_PRESENT, "Add a Keywords or Index Terms section after the abstract.",
            RuleCode.PAGE_SIZE, "Use only A4 or US Letter page size for every page in the PDF.",
            RuleCode.GENERAL, "Review the reported issue and align the document with the IEEE template."
    );

    public RuleCode resolveRuleCode(String ruleName, String message) {
        return RuleCode.resolve(ruleName, message);
    }

    public String getSuggestion(RuleCode ruleCode, String status) {
        if (!"fail".equalsIgnoreCase(status)) {
            return null;
        }
        return SUGGESTIONS.getOrDefault(ruleCode, SUGGESTIONS.get(RuleCode.GENERAL));
    }

    public String getSeverity(String status) {
        return "fail".equalsIgnoreCase(status) ? "HIGH" : "INFO";
    }
}
