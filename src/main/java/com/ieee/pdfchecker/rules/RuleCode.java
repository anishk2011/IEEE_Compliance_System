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
        return GENERAL;
    }
}
