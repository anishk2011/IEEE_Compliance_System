package com.ieee.pdfchecker.rules;

import java.util.Arrays;
import java.util.Locale;

public enum RuleCode {
    ABSTRACT_PRESENT("ABSTRACT_PRESENT", "Abstract"),
    FONT_COMPLIANCE("FONT_COMPLIANCE", "Font"),
    INTRODUCTION_PRESENT("INTRODUCTION_PRESENT", "Introduction"),
    AUTHOR_DETAILS_FORMAT("AUTHOR_DETAILS_FORMAT", "Author"),
    COLUMN_FORMAT("COLUMN_FORMAT", "Column Format"),
    KEYWORDS_PRESENT("KEYWORDS_PRESENT", "Keywords"),
    PAGE_SIZE("PAGE_SIZE", "Page Size"),
    TITLE_PRESENT("TITLE_PRESENT", "Title"),
    CONCLUSION_PRESENT("CONCLUSION_PRESENT", "Conclusion"),
    REFERENCES_PRESENT("REFERENCES_PRESENT", "References Section"),
    REFERENCE_ENTRIES_PRESENT("REFERENCE_ENTRIES_PRESENT", "Reference Entries"),
    REFERENCE_NUMBERING_SEQUENCE("REFERENCE_NUMBERING_SEQUENCE", "Reference Numbering"),
    IN_TEXT_CITATION_PRESENT("IN_TEXT_CITATION_PRESENT", "In-Text Citations"),
    CITATION_REFERENCE_MATCHING("CITATION_REFERENCE_MATCHING", "Citation-Reference Matching"),
    REFERENCE_USAGE("REFERENCE_USAGE", "Reference Usage"),
    FIGURE_PRESENT("FIGURE_PRESENT", "Figures"),
    FIGURE_CAPTION_PRESENT("FIGURE_CAPTION_PRESENT", "Figure Captions"),
    TABLE_PRESENT("TABLE_PRESENT", "Tables"),
    TABLE_CAPTION_PRESENT("TABLE_CAPTION_PRESENT", "Table Captions"),
    FIGURE_CAPTION_FORMAT("FIGURE_CAPTION_FORMAT", "Figure Caption Format"),
    FIGURE_CAPTION_POSITION("FIGURE_CAPTION_POSITION", "Figure Caption Position"),
    FIGURE_NUMBERING_SEQUENCE("FIGURE_NUMBERING_SEQUENCE", "Figure Numbering"),
    FIGURE_IN_TEXT_MENTION("FIGURE_IN_TEXT_MENTION", "Figure In-Text Mention"),
    TABLE_CAPTION_FORMAT("TABLE_CAPTION_FORMAT", "Table Caption Format"),
    TABLE_CAPTION_POSITION("TABLE_CAPTION_POSITION", "Table Caption Position"),
    TABLE_NUMBERING_SEQUENCE("TABLE_NUMBERING_SEQUENCE", "Table Numbering"),
    TABLE_IN_TEXT_MENTION("TABLE_IN_TEXT_MENTION", "Table In-Text Mention"),
    REFERENCE_FORMAT_QUALITY("REFERENCE_FORMAT_QUALITY", "Reference Format Quality"),
    NON_IEEE_CITATION_STYLE("NON_IEEE_CITATION_STYLE", "Non-IEEE Citation Style"),
    EQUATION_DETECTION("EQUATION_DETECTION", "Equations"),
    EQUATION_NUMBERING_FORMAT("EQUATION_NUMBERING_FORMAT", "Equation Numbering Format"),
    EQUATION_NUMBERING_SEQUENCE("EQUATION_NUMBERING_SEQUENCE", "Equation Numbering Sequence"),
    EQUATION_IN_TEXT_MENTION("EQUATION_IN_TEXT_MENTION", "Equation In-Text Mention"),
    SECTION_HEADING_FORMAT("SECTION_HEADING_FORMAT", "Section Heading Format"),
    GENERAL("GENERAL", "General");

    private final String code;
    private final String ruleName;

    RuleCode(String code, String ruleName) {
        this.code = code;
        this.ruleName = ruleName;
    }

    public String getCode() {
        return code;
    }

    public String getRuleName() {
        return ruleName;
    }

    public static RuleCode resolve(String ruleName, String message) {
        RuleCode byRuleName = fromRuleName(ruleName);
        if (byRuleName != GENERAL) {
            return byRuleName;
        }
        return fromMessage(message);
    }

    public static RuleCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return GENERAL;
        }

        return Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(GENERAL);
    }

    public static RuleCode fromRuleName(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) {
            return GENERAL;
        }

        return Arrays.stream(values())
                .filter(value -> value.ruleName.equalsIgnoreCase(ruleName))
                .findFirst()
                .orElse(GENERAL);
    }

    private static RuleCode fromMessage(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (normalized.contains("abstract")) return ABSTRACT_PRESENT;
        if (normalized.contains("font") || normalized.contains("typeface")) return FONT_COMPLIANCE;
        if (normalized.contains("introduction")) return INTRODUCTION_PRESENT;
        if (normalized.contains("author")) return AUTHOR_DETAILS_FORMAT;
        if (normalized.contains("column")) return COLUMN_FORMAT;
        if (normalized.contains("keyword") || normalized.contains("index terms")) return KEYWORDS_PRESENT;
        if (normalized.contains("page size") || normalized.contains("incorrect size")) return PAGE_SIZE;
        if (normalized.contains("title")) return TITLE_PRESENT;
        if (normalized.contains("conclusion")) return CONCLUSION_PRESENT;
        if (normalized.contains("references")) return REFERENCES_PRESENT;
        if (normalized.contains("reference entries")) return REFERENCE_ENTRIES_PRESENT;
        if (normalized.contains("reference numbering")) return REFERENCE_NUMBERING_SEQUENCE;
        if (normalized.contains("in-text citation") || normalized.contains("citation")) return IN_TEXT_CITATION_PRESENT;
        if (normalized.contains("citation-reference")) return CITATION_REFERENCE_MATCHING;
        if (normalized.contains("reference usage")) return REFERENCE_USAGE;
        if (normalized.contains("figure caption format")) return FIGURE_CAPTION_FORMAT;
        if (normalized.contains("figure caption position")) return FIGURE_CAPTION_POSITION;
        if (normalized.contains("figure numbering")) return FIGURE_NUMBERING_SEQUENCE;
        if (normalized.contains("figure") && normalized.contains("mention")) return FIGURE_IN_TEXT_MENTION;
        if (normalized.contains("figure caption")) return FIGURE_CAPTION_PRESENT;
        if (normalized.contains("figure")) return FIGURE_PRESENT;
        if (normalized.contains("table caption format")) return TABLE_CAPTION_FORMAT;
        if (normalized.contains("table caption position")) return TABLE_CAPTION_POSITION;
        if (normalized.contains("table numbering")) return TABLE_NUMBERING_SEQUENCE;
        if (normalized.contains("table") && normalized.contains("mention")) return TABLE_IN_TEXT_MENTION;
        if (normalized.contains("table caption")) return TABLE_CAPTION_PRESENT;
        if (normalized.contains("table")) return TABLE_PRESENT;
        if (normalized.contains("reference format")) return REFERENCE_FORMAT_QUALITY;
        if (normalized.contains("non-ieee citation style")) return NON_IEEE_CITATION_STYLE;
        if (normalized.contains("equation numbering format")) return EQUATION_NUMBERING_FORMAT;
        if (normalized.contains("equation numbering")) return EQUATION_NUMBERING_SEQUENCE;
        if (normalized.contains("equation") && normalized.contains("mention")) return EQUATION_IN_TEXT_MENTION;
        if (normalized.contains("equation")) return EQUATION_DETECTION;
        if (normalized.contains("section heading")) return SECTION_HEADING_FORMAT;
        return GENERAL;
    }
}
