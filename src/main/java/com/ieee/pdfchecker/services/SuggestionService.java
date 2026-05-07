package com.ieee.pdfchecker.services;

import com.ieee.pdfchecker.rules.RuleCode;
import org.springframework.stereotype.Service;

import java.util.Map;
import static java.util.Map.entry;

@Service
public class SuggestionService {

    private static final Map<RuleCode, String> SUGGESTIONS = Map.ofEntries(
            entry(RuleCode.ABSTRACT_PRESENT, "Add an Abstract heading near the beginning of the paper and include a concise summary."),
            entry(RuleCode.FONT_COMPLIANCE, "Use Times New Roman or an IEEE-accepted equivalent font consistently throughout the document."),
            entry(RuleCode.INTRODUCTION_PRESENT, "Include an Introduction section within the first few pages using a clear section heading."),
            entry(RuleCode.AUTHOR_DETAILS_FORMAT, "Ensure author names, affiliations, and email details are clearly listed near the title."),
            entry(RuleCode.COLUMN_FORMAT, "Format the main content in a clear two-column IEEE-style layout."),
            entry(RuleCode.KEYWORDS_PRESENT, "Add a Keywords or Index Terms section after the abstract."),
            entry(RuleCode.PAGE_SIZE, "Use only A4 or US Letter page size for every page in the PDF."),
            entry(RuleCode.TITLE_PRESENT, "Add a clear paper title at the top of the first page using the IEEE manuscript structure."),
            entry(RuleCode.CONCLUSION_PRESENT, "Add a Conclusion section near the end of the paper to summarize findings and final takeaways."),
            entry(RuleCode.REFERENCES_PRESENT, "Add a References section at the end of the paper using an IEEE-style heading."),
            entry(RuleCode.REFERENCE_ENTRIES_PRESENT, "Include one or more full reference entries in the References section."),
            entry(RuleCode.REFERENCE_NUMBERING_SEQUENCE, "Number the reference list sequentially in IEEE bracketed style, such as [1], [2], and [3]."),
            entry(RuleCode.IN_TEXT_CITATION_PRESENT, "Add in-text citations in IEEE bracketed form to support claims and referenced material."),
            entry(RuleCode.CITATION_REFERENCE_MATCHING, "Make sure every in-text citation matches a reference entry and every listed reference is actually cited."),
            entry(RuleCode.REFERENCE_USAGE, "Use references throughout the paper where prior work, data, methods, or borrowed ideas are discussed."),
            entry(RuleCode.FIGURE_PRESENT, "Add at least one figure if your paper depends on visual results, architecture diagrams, workflows, or plotted outputs."),
            entry(RuleCode.FIGURE_CAPTION_PRESENT, "Add a descriptive caption for each figure so the reader can understand it without guessing."),
            entry(RuleCode.TABLE_PRESENT, "Add a table if your paper presents structured comparisons, datasets, metrics, or summarized results."),
            entry(RuleCode.TABLE_CAPTION_PRESENT, "Add a descriptive caption for each table to explain what the table contains."),
            entry(RuleCode.FIGURE_CAPTION_FORMAT, "Improve figure captions so they follow a consistent IEEE-style format and read clearly."),
            entry(RuleCode.FIGURE_CAPTION_POSITION, "Place figure captions consistently in the expected position for the chosen IEEE template."),
            entry(RuleCode.FIGURE_NUMBERING_SEQUENCE, "Number figures sequentially and avoid skipped, repeated, or out-of-order figure labels."),
            entry(RuleCode.FIGURE_IN_TEXT_MENTION, "Mention each figure in the main text before or near where it appears so readers know when to consult it."),
            entry(RuleCode.TABLE_CAPTION_FORMAT, "Improve table captions so they follow a consistent IEEE-style format and naming convention."),
            entry(RuleCode.TABLE_CAPTION_POSITION, "Place table captions consistently in the expected position for the chosen IEEE template."),
            entry(RuleCode.TABLE_NUMBERING_SEQUENCE, "Number tables sequentially and avoid skipped, repeated, or out-of-order table labels."),
            entry(RuleCode.TABLE_IN_TEXT_MENTION, "Mention each table in the main text so the reader understands where it is being discussed."),
            entry(RuleCode.REFERENCE_FORMAT_QUALITY, "Improve reference formatting so entries consistently follow IEEE punctuation, ordering, and abbreviation style."),
            entry(RuleCode.NON_IEEE_CITATION_STYLE, "Replace non-IEEE citation styles with IEEE bracketed citation formatting throughout the manuscript."),
            entry(RuleCode.EQUATION_DETECTION, "Add displayed equations only where needed and present them clearly as formal mathematical expressions."),
            entry(RuleCode.EQUATION_NUMBERING_FORMAT, "Format equation numbers consistently using the IEEE style expected by your manuscript template."),
            entry(RuleCode.EQUATION_NUMBERING_SEQUENCE, "Number equations sequentially and avoid skipped, repeated, or inconsistent numbering."),
            entry(RuleCode.EQUATION_IN_TEXT_MENTION, "Refer to each numbered equation in the surrounding text so the reader knows why it matters."),
            entry(RuleCode.SECTION_HEADING_FORMAT, "Improve section headings so their style, capitalization, and numbering match the IEEE template consistently."),
            entry(RuleCode.GENERAL, "Review the reported issue and align the document with the IEEE template.")
    );

    public RuleCode resolveRuleCode(String ruleName, String message) {
        return RuleCode.resolve(ruleName, message);
    }

    public String getSuggestion(RuleCode ruleCode, String status) {
        if (!com.ieee.pdfchecker.reports.ComplianceItem.STATUS_FAIL.equalsIgnoreCase(status)) {
            return null;
        }
        return SUGGESTIONS.getOrDefault(ruleCode, SUGGESTIONS.get(RuleCode.GENERAL));
    }

    public String getSeverity(String status) {
        if (com.ieee.pdfchecker.reports.ComplianceItem.STATUS_FAIL.equalsIgnoreCase(status)) {
            return "HIGH";
        }
        if (com.ieee.pdfchecker.reports.ComplianceItem.STATUS_WARN.equalsIgnoreCase(status)) {
            return "LOW";
        }
        return "INFO";
    }
}
