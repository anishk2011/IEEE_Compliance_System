package com.ieee.pdfchecker.rules;

import com.ieee.pdfchecker.reports.ComplianceReport;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleEngine {

    public ComplianceReport checkCompliance(File file) {
        ComplianceReport report = new ComplianceReport(file.getName());

        try (PDDocument document = PDDocument.load(file)) {
            checkFont(document, report);
            checkColumnFormat(document, report);
            checkPageSize(document, report);
            checkAbstractPresence(document, report);
            checkAuthorDetailsFormat(document, report);
            checkKeywordsFormat(document, report);
            checkIntroPresence(document, report);
            checkTitlePresence(document, report);

            String fullText = extractFullText(document);
            checkConclusionPresence(fullText, report);
            checkReferenceAndCitationRules(fullText, report);
            checkFigureRules(document, fullText, report);
            checkTableRules(fullText, report);
            checkEquationRules(fullText, report);
        } catch (IOException e) {
            report.addError("Error reading PDF: " + e.getMessage());
        }
        return report;
    }

    private void checkPageSize(PDDocument document, ComplianceReport report) {
        PDPageTree pages = document.getDocumentCatalog().getPages();
        boolean allCompliant = true;

        for (PDPage page : pages) {
            Rectangle2D pageSize = new Rectangle2D.Float(
                    page.getMediaBox().getLowerLeftX(),
                    page.getMediaBox().getLowerLeftY(),
                    page.getMediaBox().getWidth(),
                    page.getMediaBox().getHeight()
            );

            boolean isA4 = (pageSize.getWidth() == 595 && pageSize.getHeight() == 842);
            boolean isLetter = (pageSize.getWidth() == 612 && pageSize.getHeight() == 792);

            if (!isA4 && !isLetter) {
                report.addError("Page size is incorrect. Use A4 (595x842) or US Letter (612x792).");
                allCompliant = false;
                break;
            }
        }

        if (allCompliant) {
            report.addInfo("Page size is compliant on all pages.");
        } else {
            report.addError("One or more pages have an incorrect size. Use A4 (595x842) or US Letter (612x792).");
        }

        report.setPageSizeCompliant(allCompliant);
    }

    private void checkColumnFormat(PDDocument document, ComplianceReport report) throws IOException {
        int numberOfPages = document.getNumberOfPages();
        boolean overallCompliant = true;
        float minCentroidSeparation = 50.0f;

        for (int page = 1; page <= numberOfPages; page++) {
            List<Float> firstWordPositions = new ArrayList<>();

            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                    if (textPositions != null && !textPositions.isEmpty()) {
                        firstWordPositions.add(textPositions.get(0).getXDirAdj());
                    }
                }
            };

            stripper.setStartPage(page);
            stripper.setEndPage(page);
            stripper.getText(document);

            if (firstWordPositions.isEmpty()) {
                report.addError("Page " + page + ": No text found.");
                overallCompliant = false;
                continue;
            }

            float centroid1 = Collections.min(firstWordPositions);
            float centroid2 = Collections.max(firstWordPositions);

            if (centroid1 == centroid2) {
                report.addError("Page " + page + ": Column format not compliant, detected 1 column.");
                overallCompliant = false;
                continue;
            }

            List<Float> cluster1 = new ArrayList<>();
            List<Float> cluster2 = new ArrayList<>();
            for (int iter = 0; iter < 100; iter++) {
                cluster1.clear();
                cluster2.clear();
                for (float pos : firstWordPositions) {
                    if (Math.abs(pos - centroid1) <= Math.abs(pos - centroid2)) {
                        cluster1.add(pos);
                    } else {
                        cluster2.add(pos);
                    }
                }
                float newCentroid1 = cluster1.isEmpty() ? centroid1 : average(cluster1);
                float newCentroid2 = cluster2.isEmpty() ? centroid2 : average(cluster2);
                if (Math.abs(newCentroid1 - centroid1) < 0.01f && Math.abs(newCentroid2 - centroid2) < 0.01f) {
                    centroid1 = newCentroid1;
                    centroid2 = newCentroid2;
                    break;
                }
                centroid1 = newCentroid1;
                centroid2 = newCentroid2;
            }

            if (Math.abs(centroid1 - centroid2) < minCentroidSeparation) {
                overallCompliant = false;
            }
        }

        if (overallCompliant) {
            report.addInfo("Column format is compliant across the document.");
            report.setColumnFormatCompliant(true);
        } else {
            report.addError("Column format is not compliant across the document.");
            report.setColumnFormatCompliant(false);
        }
    }

    private float average(List<Float> list) {
        float sum = 0;
        for (float value : list) {
            sum += value;
        }
        return sum / list.size();
    }

    private void checkAbstractPresence(PDDocument document, ComplianceReport report) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(1);
        textStripper.setEndPage(Math.min(2, document.getNumberOfPages()));

        String text = textStripper.getText(document);
        if (!text.toUpperCase().contains("ABSTRACT")) {
            report.addError("Abstract section is missing");
            report.setAbstractPresent(false);
        } else {
            report.addInfo("Abstract section is present");
            report.setAbstractPresent(true);
        }
    }

    private void checkFont(PDDocument document, ComplianceReport report) {
        Set<String> detectedFonts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> normalizedFonts = new HashSet<>();

        try {
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                    super.writeString(text, textPositions);
                    if (textPositions == null) {
                        return;
                    }

                    for (TextPosition textPosition : textPositions) {
                        collectFontNames(textPosition.getFont(), detectedFonts, normalizedFonts);
                    }
                }
            };

            stripper.getText(document);
        } catch (IOException e) {
            report.addError("Error checking font using PDFBox: " + e.getMessage());
            report.setFontCompliant(false);
            return;
        }

        if (containsCompatibleSerifFont(normalizedFonts)) {
            report.addInfo("Font is compliant. An IEEE-compatible serif font was detected.");
            report.setFontCompliant(true);
            return;
        }

        Set<String> incompatibleRealFonts = findIncompatibleRealFonts(detectedFonts);
        if (incompatibleRealFonts.isEmpty() && (detectedFonts.isEmpty() || containsOnlyFontAliases(detectedFonts))) {
            report.addPass(RuleCode.FONT_COMPLIANCE,
                    "Embedded CID fonts were detected and accepted as IEEE-compatible for this PDF because no incompatible real font family was found.");
            report.setFontCompliant(true);
            return;
        }

        String fontsList = incompatibleRealFonts.isEmpty()
                ? String.join(", ", detectedFonts)
                : String.join(", ", incompatibleRealFonts);
        report.addFail(RuleCode.FONT_COMPLIANCE,
                "Times New Roman or an IEEE-compatible serif font was not detected. Detected fonts: " + fontsList);
        report.setFontCompliant(false);
    }

    private void checkAuthorDetailsFormat(PDDocument document, ComplianceReport report) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(1);
        textStripper.setEndPage(1);

        String text = textStripper.getText(document);

        boolean hasSimpleName = Pattern.compile("\\b[A-Z][a-z]+ [A-Z][a-z]+\\b").matcher(text).find();
        boolean hasAffiliation = text.toLowerCase().contains("department") || text.toLowerCase().contains("university");
        boolean hasEmail = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+").matcher(text).find();

        if (hasSimpleName && hasAffiliation) {
            report.addInfo("Author details are properly formatted.");
            report.setAuthorDetailsCompliant(true);
        } else {
            report.addError("Author details are missing or incorrectly formatted. Check the name, affiliation, and structure.");
            report.setAuthorDetailsCompliant(false);
        }
    }

    private void checkKeywordsFormat(PDDocument document, ComplianceReport report) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(1);
        textStripper.setEndPage(Math.min(2, document.getNumberOfPages()));

        String text = textStripper.getText(document).toLowerCase();
        if (text.contains("keywords") || text.contains("index terms")) {
            report.addInfo("Keywords section is present.");
            report.setKeywordsPresent(true);
        } else {
            report.addError("Keywords section is missing");
            report.setKeywordsPresent(false);
        }
    }

    private void checkIntroPresence(PDDocument document, ComplianceReport report) {
        AtomicBoolean foundIntroduction = new AtomicBoolean(false);

        try {
            PDFTextStripper textStripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                    super.writeString(text, textPositions);
                    processText(text, foundIntroduction);
                }
            };

            textStripper.setStartPage(1);
            textStripper.setEndPage(Math.min(3, document.getNumberOfPages()));
            textStripper.getText(document);

            if (foundIntroduction.get()) {
                report.addInfo("Introduction section is present");
            } else {
                report.addError("Introduction section is missing");
            }
        } catch (IOException e) {
            report.addError("Error checking Introduction section: " + e.getMessage());
        }
    }

    private void processText(String text, AtomicBoolean foundIntroduction) {
        String normalizedText = text.replaceAll("\\s+", " ").trim().toLowerCase();

        if (!foundIntroduction.get() && normalizedText.contains("introduction")) {
            foundIntroduction.set(true);
        }
    }

    private void checkTitlePresence(PDDocument document, ComplianceReport report) throws IOException {
        String firstPageText = extractFirstPageText(document);
        List<String> meaningfulLines = getMeaningfulLines(firstPageText);

        for (int i = 0; i < Math.min(5, meaningfulLines.size()); i++) {
            String line = meaningfulLines.get(i);
            if (isLikelyTitleLine(line)) {
                report.addPass(RuleCode.TITLE_PRESENT, "A likely paper title was detected near the beginning of the first page.");
                return;
            }
        }

        report.addFail(RuleCode.TITLE_PRESENT, "A clear paper title was not detected near the beginning of the first page.");
    }

    private void checkConclusionPresence(String fullText, ComplianceReport report) {
        if (hasConclusionHeading(fullText)) {
            report.addPass(RuleCode.CONCLUSION_PRESENT, "A conclusion section was detected.");
            return;
        }

        report.addFail(RuleCode.CONCLUSION_PRESENT, "A conclusion section was not detected. Add a Conclusion or Conclusion and Future Work section.");
    }

    private void checkReferenceAndCitationRules(String fullText, ComplianceReport report) {
        ReferencesSection referencesSection = findReferencesSection(fullText);

        if (referencesSection == null) {
            report.addFail(RuleCode.REFERENCES_PRESENT, "A References section was not detected near the end of the document.");
            report.addNotApplicable(RuleCode.REFERENCE_ENTRIES_PRESENT, "Reference entry checks were skipped because no References section was found.");
            report.addNotApplicable(RuleCode.REFERENCE_NUMBERING_SEQUENCE, "Reference numbering was not checked because no References section was found.");
            report.addFail(RuleCode.IN_TEXT_CITATION_PRESENT, "No IEEE-style in-text citations were detected in the document body.");
            report.addNotApplicable(RuleCode.CITATION_REFERENCE_MATCHING, "Citation-to-reference matching was skipped because no References section was found.");
            report.addNotApplicable(RuleCode.REFERENCE_USAGE, "Reference usage was not checked because no References section was found.");
            return;
        }

        if (referencesSection.usesBibliographyHeading()) {
            report.addPass(RuleCode.REFERENCES_PRESENT, "A Bibliography section was detected near the end of the document. IEEE usually prefers the heading 'References'.");
        } else {
            report.addPass(RuleCode.REFERENCES_PRESENT, "A References section was detected near the end of the document.");
        }

        List<Integer> referenceNumbers = extractReferenceEntryNumbers(referencesSection.sectionText());
        if (referenceNumbers.isEmpty()) {
            report.addFail(RuleCode.REFERENCE_ENTRIES_PRESENT, "No numbered reference entries like [1], [2], or [3] were detected in the References section.");
            report.addNotApplicable(RuleCode.REFERENCE_NUMBERING_SEQUENCE, "Reference numbering was not checked because no numbered reference entries were found.");
        } else {
            report.addPass(RuleCode.REFERENCE_ENTRIES_PRESENT, "Numbered reference entries were detected in the References section.");

            String numberingIssue = validateSequentialNumbers(referenceNumbers);
            if (numberingIssue == null) {
                report.addPass(RuleCode.REFERENCE_NUMBERING_SEQUENCE, "Reference entries are numbered sequentially starting from [1].");
            } else {
                report.addFail(RuleCode.REFERENCE_NUMBERING_SEQUENCE, numberingIssue);
            }
        }

        Set<Integer> citedNumbers = extractCitationNumbersFromBody(referencesSection.bodyText());
        if (citedNumbers.isEmpty()) {
            report.addFail(RuleCode.IN_TEXT_CITATION_PRESENT, "No IEEE-style in-text citations were detected before the References section.");
            report.addNotApplicable(RuleCode.CITATION_REFERENCE_MATCHING, "Citation-to-reference matching was skipped because no in-text citations were found.");
            report.addNotApplicable(RuleCode.REFERENCE_USAGE, "Reference usage was not checked because no in-text citations were found.");
            return;
        }

        report.addPass(RuleCode.IN_TEXT_CITATION_PRESENT, "IEEE-style in-text citations were detected in the document body.");

        if (referenceNumbers.isEmpty()) {
            report.addNotApplicable(RuleCode.CITATION_REFERENCE_MATCHING, "Citation-to-reference matching was skipped because no numbered reference entries were found.");
            report.addNotApplicable(RuleCode.REFERENCE_USAGE, "Reference usage was not checked because no numbered reference entries were found.");
            return;
        }

        Set<Integer> referenceNumberSet = new LinkedHashSet<>(referenceNumbers);
        Set<Integer> missingReferences = new LinkedHashSet<>(citedNumbers);
        missingReferences.removeAll(referenceNumberSet);

        if (missingReferences.isEmpty()) {
            report.addPass(RuleCode.CITATION_REFERENCE_MATCHING, "Every detected in-text citation has a matching numbered reference entry.");
        } else {
            report.addFail(RuleCode.CITATION_REFERENCE_MATCHING,
                    "Some in-text citations do not have matching reference entries: " + formatNumbers(missingReferences) + ".");
        }

        Set<Integer> unusedReferences = new LinkedHashSet<>(referenceNumberSet);
        unusedReferences.removeAll(citedNumbers);

        if (unusedReferences.isEmpty()) {
            report.addPass(RuleCode.REFERENCE_USAGE, "Every numbered reference entry is cited at least once in the document body.");
        } else {
            report.addFail(RuleCode.REFERENCE_USAGE,
                    "Some reference entries are not cited in the document body: " + formatNumbers(unusedReferences) + ".");
        }
    }

    private void checkFigureRules(PDDocument document, String fullText, ComplianceReport report) throws IOException {
        boolean hasFigureCaption = detectFigureCaptions(fullText);
        List<FigureCaption> figureCaptions = extractFigureCaptions(fullText);
        boolean hasPdfImages = detectPdfImages(document);
        boolean figurePresent = hasFigureCaption || hasPdfImages;

        if (figurePresent) {
            if (hasFigureCaption) {
                report.addPass(RuleCode.FIGURE_PRESENT, "At least one figure was detected in the paper.");
                report.addPass(RuleCode.FIGURE_CAPTION_PRESENT, "At least one figure caption was detected.");
                if (figureCaptions.isEmpty()) {
                    report.addNotApplicable(RuleCode.FIGURE_CAPTION_FORMAT, "Figure caption format was not checked because no strict figure caption lines were extracted.");
                    report.addNotApplicable(RuleCode.FIGURE_NUMBERING_SEQUENCE, "Figure numbering was not checked because no strict figure caption lines were extracted.");
                    report.addNotApplicable(RuleCode.FIGURE_IN_TEXT_MENTION, "Figure in-text mention checks were skipped because no strict figure caption lines were extracted.");
                } else {
                    checkFigureCaptionFormat(figureCaptions, report);
                    checkFigureNumberingSequence(figureCaptions, report);
                    checkFigureInTextMentions(fullText, figureCaptions, report);
                }
            } else {
                report.addPass(RuleCode.FIGURE_PRESENT, "At least one embedded PDF image was detected in the paper.");
                report.addFail(RuleCode.FIGURE_CAPTION_PRESENT, "A figure or embedded image was detected, but no valid figure caption pattern such as 'Fig. 1. ...' was found.");
                report.addNotApplicable(RuleCode.FIGURE_CAPTION_FORMAT, "Figure caption format was not checked because no figure captions were detected.");
                report.addNotApplicable(RuleCode.FIGURE_NUMBERING_SEQUENCE, "Figure numbering was not checked because no figure captions were detected.");
                report.addNotApplicable(RuleCode.FIGURE_IN_TEXT_MENTION, "Figure in-text mention checks were skipped because no figure captions were detected.");
            }
            return;
        }

        report.addFail(RuleCode.FIGURE_PRESENT, "No figure was detected in the paper. At least one figure is expected for this project requirement.");
        report.addNotApplicable(RuleCode.FIGURE_CAPTION_PRESENT, "Figure caption checks were skipped because no figure was detected.");
        report.addNotApplicable(RuleCode.FIGURE_CAPTION_FORMAT, "Figure caption format was not checked because no figure was detected.");
        report.addNotApplicable(RuleCode.FIGURE_NUMBERING_SEQUENCE, "Figure numbering was not checked because no figure was detected.");
        report.addNotApplicable(RuleCode.FIGURE_IN_TEXT_MENTION, "Figure in-text mention checks were skipped because no figure was detected.");
    }

    private void checkTableRules(String fullText, ComplianceReport report) {
        boolean hasTableCaption = detectTableCaptions(fullText);
        List<TableCaption> tableCaptions = extractTableCaptions(fullText);
        boolean hasTableLikeText = detectTableLikeText(fullText);
        boolean tablePresent = hasTableCaption || hasTableLikeText;

        if (tablePresent) {
            if (hasTableCaption) {
                report.addPass(RuleCode.TABLE_PRESENT, "At least one table was detected in the paper.");
                report.addPass(RuleCode.TABLE_CAPTION_PRESENT, "At least one table caption or title was detected.");
                if (tableCaptions.isEmpty()) {
                    report.addNotApplicable(RuleCode.TABLE_CAPTION_FORMAT, "Table caption format was not checked because no strict table caption lines were extracted.");
                    report.addNotApplicable(RuleCode.TABLE_NUMBERING_SEQUENCE, "Table numbering was not checked because no strict table caption lines were extracted.");
                    report.addNotApplicable(RuleCode.TABLE_IN_TEXT_MENTION, "Table in-text mention checks were skipped because no strict table caption lines were extracted.");
                } else {
                    checkTableCaptionFormat(tableCaptions, report);
                    checkTableNumberingSequence(tableCaptions, report);
                    checkTableInTextMentions(fullText, tableCaptions, report);
                }
            } else {
                report.addPass(RuleCode.TABLE_PRESENT, "Table-like structured content was detected in the paper.");
                report.addFail(RuleCode.TABLE_CAPTION_PRESENT, "Table-like content was detected, but no valid table caption or title such as 'TABLE I' or 'Table 1' was found.");
                report.addNotApplicable(RuleCode.TABLE_CAPTION_FORMAT, "Table caption format was not checked because no table captions were detected.");
                report.addNotApplicable(RuleCode.TABLE_NUMBERING_SEQUENCE, "Table numbering was not checked because no table captions were detected.");
                report.addNotApplicable(RuleCode.TABLE_IN_TEXT_MENTION, "Table in-text mention checks were skipped because no table captions were detected.");
            }
            return;
        }

        report.addFail(RuleCode.TABLE_PRESENT, "No table was detected in the paper. At least one table is expected for this project requirement.");
        report.addNotApplicable(RuleCode.TABLE_CAPTION_PRESENT, "Table caption checks were skipped because no table was detected.");
        report.addNotApplicable(RuleCode.TABLE_CAPTION_FORMAT, "Table caption format was not checked because no table was detected.");
        report.addNotApplicable(RuleCode.TABLE_NUMBERING_SEQUENCE, "Table numbering was not checked because no table was detected.");
        report.addNotApplicable(RuleCode.TABLE_IN_TEXT_MENTION, "Table in-text mention checks were skipped because no table was detected.");
    }

    private void checkEquationRules(String fullText, ComplianceReport report) {
        List<String> equationLikeLines = detectEquationLikeLines(fullText);
        if (equationLikeLines.isEmpty()) {
            report.addNotApplicable(RuleCode.EQUATION_DETECTION, "No equation-like content was detected. Equation validation is not applicable.");
            report.addNotApplicable(RuleCode.EQUATION_NUMBERING_FORMAT, "Equation numbering format was not checked because no equation-like content was detected.");
            report.addNotApplicable(RuleCode.EQUATION_NUMBERING_SEQUENCE, "Equation numbering sequence was not checked because no numbered equations were detected.");
            report.addNotApplicable(RuleCode.EQUATION_IN_TEXT_MENTION, "Equation in-text mention checks were skipped because no numbered equations were detected.");
            return;
        }

        report.addPass(RuleCode.EQUATION_DETECTION, "Equation-like content was detected in the paper.");

        List<Integer> equationNumbers = extractEquationNumbers(equationLikeLines);
        checkEquationNumberingFormat(equationLikeLines, equationNumbers, report);
        checkEquationNumberingSequence(equationNumbers, report);
        checkEquationInTextMentions(fullText, equationLikeLines, equationNumbers, report);
    }

    private String extractFullText(PDDocument document) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        return textStripper.getText(document);
    }

    private String extractFirstPageText(PDDocument document) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setStartPage(1);
        textStripper.setEndPage(1);
        return textStripper.getText(document);
    }

    private List<String> getMeaningfulLines(String text) {
        List<String> meaningfulLines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            String normalized = line.replaceAll("\\s+", " ").trim();
            if (!normalized.isBlank()) {
                meaningfulLines.add(normalized);
            }
        }
        return meaningfulLines;
    }

    private boolean isLikelyTitleLine(String line) {
        String normalized = line.replaceAll("\\s+", " ").trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (normalized.length() < 12 || !normalized.matches(".*[A-Za-z].*")) {
            return false;
        }

        if (lower.contains("@")
                || lower.startsWith("abstract")
                || lower.startsWith("keywords")
                || lower.startsWith("index terms")
                || lower.startsWith("introduction")
                || lower.startsWith("references")
                || lower.startsWith("bibliography")) {
            return false;
        }

        if (lower.startsWith("department")
                || lower.startsWith("university")
                || lower.startsWith("school")
                || lower.startsWith("faculty")
                || lower.startsWith("author")) {
            return false;
        }

        String[] words = normalized.split("\\s+");
        return words.length >= 3 || normalized.length() >= 25;
    }

    private boolean containsSectionHeading(String text, String... headings) {
        for (String heading : headings) {
            Pattern pattern = buildSectionHeadingPattern(heading);
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasConclusionHeading(String text) {
        Pattern conclusionHeadingPattern = Pattern.compile(
                "(?i)^\\s*(?:(?:[IVXLCDM]+|\\d+)\\s*\\.?\\s*)?"
                        + "(?:CONCLUSIONS?"
                        + "|CONCLUSION\\s+(?:AND|&)\\s+FUTURE\\s+(?:WORK|SCOPE))\\s*$"
        );

        for (String line : text.split("\\R")) {
            String normalizedLine = line.replaceAll("\\s+", " ").trim();
            if (!normalizedLine.isBlank() && conclusionHeadingPattern.matcher(normalizedLine).matches()) {
                return true;
            }
        }

        return false;
    }

    private ReferencesSection findReferencesSection(String fullText) {
        String[] lines = fullText.split("\\R");
        int meaningfulIndex = 0;

        for (int i = 0; i < lines.length; i++) {
            String normalized = lines[i].replaceAll("\\s+", " ").trim();
            if (normalized.isBlank()) {
                continue;
            }

            meaningfulIndex++;
            if (meaningfulIndex <= 8) {
                continue;
            }

            if (buildSectionHeadingPattern("References").matcher(normalized).matches()) {
                return buildReferencesSection(lines, i, false);
            }

            if (buildSectionHeadingPattern("Bibliography").matcher(normalized).matches()) {
                return buildReferencesSection(lines, i, true);
            }
        }

        return null;
    }

    private ReferencesSection buildReferencesSection(String[] lines, int sectionLineIndex, boolean bibliographyHeading) {
        StringBuilder bodyText = new StringBuilder();
        StringBuilder sectionText = new StringBuilder();

        for (int i = 0; i < sectionLineIndex; i++) {
            bodyText.append(lines[i]).append(System.lineSeparator());
        }

        for (int i = sectionLineIndex + 1; i < lines.length; i++) {
            sectionText.append(lines[i]).append(System.lineSeparator());
        }

        return new ReferencesSection(bodyText.toString(), sectionText.toString(), bibliographyHeading);
    }

    private Pattern buildSectionHeadingPattern(String heading) {
        String[] words = heading.trim().split("\\s+");
        StringBuilder headingRegex = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                headingRegex.append("\\s+");
            }
            headingRegex.append(Pattern.quote(words[i]));
        }

        return Pattern.compile("(?i)^\\s*(?:[IVXLC]+\\.?|\\d+(?:\\.\\d+)*\\.?)?\\s*" + headingRegex + "\\s*$");
    }

    private List<Integer> extractReferenceEntryNumbers(String referencesText) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = Pattern.compile("(?m)^\\s*\\[(\\d+)]").matcher(referencesText);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group(1)));
        }
        return numbers;
    }

    private Set<Integer> extractCitationNumbersFromBody(String bodyText) {
        Set<Integer> citationNumbers = new LinkedHashSet<>();

        // Expand ranges written as [1]-[3] before reading individual bracket groups.
        Matcher pairedRangeMatcher = Pattern.compile("\\[(\\d+)]\\s*-\\s*\\[(\\d+)]").matcher(bodyText);
        while (pairedRangeMatcher.find()) {
            addRange(citationNumbers,
                    Integer.parseInt(pairedRangeMatcher.group(1)),
                    Integer.parseInt(pairedRangeMatcher.group(2)));
        }

        Matcher bracketMatcher = Pattern.compile("\\[([^\\]]+)]").matcher(bodyText);
        while (bracketMatcher.find()) {
            String content = bracketMatcher.group(1).replaceAll("(?i),\\s*pp?\\..*$", "");
            Matcher tokenMatcher = Pattern.compile("\\d+\\s*-\\s*\\d+|\\d+").matcher(content);

            while (tokenMatcher.find()) {
                String token = tokenMatcher.group();
                if (token.contains("-")) {
                    String[] parts = token.split("\\s*-\\s*");
                    addRange(citationNumbers, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                } else {
                    citationNumbers.add(Integer.parseInt(token));
                }
            }
        }

        return citationNumbers;
    }

    private void addRange(Set<Integer> numbers, int start, int end) {
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        for (int value = start; value <= end; value++) {
            numbers.add(value);
        }
    }

    private String validateSequentialNumbers(List<Integer> numbers) {
        if (numbers.isEmpty()) {
            return "No numbered reference entries were found to validate.";
        }

        if (numbers.get(0) != 1) {
            return "Reference numbering should start from [1], but the first detected entry was [" + numbers.get(0) + "].";
        }

        for (int i = 1; i < numbers.size(); i++) {
            int expected = numbers.get(i - 1) + 1;
            int actual = numbers.get(i);

            if (actual == numbers.get(i - 1)) {
                return "Duplicate reference number detected: [" + actual + "].";
            }

            if (actual != expected) {
                return "Reference numbering is not sequential. Expected [" + expected + "] but found [" + actual + "].";
            }
        }

        return null;
    }

    private String validateSequentialNumbers(List<Integer> numbers, String label) {
        if (numbers.isEmpty()) {
            return "No numbered " + label.toLowerCase(Locale.ROOT) + " entries were found to validate.";
        }

        if (numbers.get(0) != 1) {
            return label + " numbering should start from 1, but the first detected entry was " + numbers.get(0) + ".";
        }

        for (int i = 1; i < numbers.size(); i++) {
            int expected = numbers.get(i - 1) + 1;
            int actual = numbers.get(i);

            if (actual == numbers.get(i - 1)) {
                return "Duplicate " + label.toLowerCase(Locale.ROOT) + " number detected: " + actual + ".";
            }

            if (actual != expected) {
                return label + " numbering is not sequential. Expected " + expected + " but found " + actual + ".";
            }
        }

        return null;
    }

    private String formatNumbers(Set<Integer> numbers) {
        return numbers.stream()
                .sorted()
                .map(number -> "[" + number + "]")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String formatPlainNumbers(List<Integer> numbers, String prefix) {
        return numbers.stream()
                .sorted()
                .map(number -> prefix + " " + number)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private void checkFigureCaptionFormat(List<FigureCaption> figureCaptions, ComplianceReport report) {
        boolean allPreferred = figureCaptions.stream().allMatch(FigureCaption::preferredFormat);
        if (allPreferred) {
            report.addPass(RuleCode.FIGURE_CAPTION_FORMAT, "Detected figure captions follow the preferred IEEE-style format 'Fig. <number>. <caption text>'.");
            return;
        }

        report.addWarn(RuleCode.FIGURE_CAPTION_FORMAT,
                "One or more figure captions use a non-preferred format. IEEE style prefers 'Fig. <number>. <caption text>'.");
    }

    private void checkFigureNumberingSequence(List<FigureCaption> figureCaptions, ComplianceReport report) {
        if (figureCaptions.size() < 2) {
            report.addPass(RuleCode.FIGURE_NUMBERING_SEQUENCE, "Fewer than two figure captions were detected, so figure numbering is acceptable.");
            return;
        }

        String numberingIssue = validateSequentialNumbers(
                figureCaptions.stream().map(FigureCaption::number).toList(),
                "Figure");
        if (numberingIssue == null) {
            report.addPass(RuleCode.FIGURE_NUMBERING_SEQUENCE, "Figure captions are numbered sequentially.");
        } else {
            report.addWarn(RuleCode.FIGURE_NUMBERING_SEQUENCE, numberingIssue);
        }
    }

    private void checkFigureInTextMentions(String fullText, List<FigureCaption> figureCaptions, ComplianceReport report) {
        String bodyText = removeMatchingLines(fullText, getFigureCaptionPattern());
        List<Integer> missingMentions = new ArrayList<>();

        for (FigureCaption caption : figureCaptions) {
            Pattern mentionPattern = Pattern.compile(
                    "(?i)\\b(?:fig\\.|figure)\\s*" + caption.number() + "(?:\\.)?(?=\\b|\\s|[,;:])"
            );
            if (!mentionPattern.matcher(bodyText).find()) {
                missingMentions.add(caption.number());
            }
        }

        if (missingMentions.isEmpty()) {
            report.addPass(RuleCode.FIGURE_IN_TEXT_MENTION, "Every detected figure caption is referenced in the body text.");
        } else {
            report.addWarn(RuleCode.FIGURE_IN_TEXT_MENTION,
                    "Some figures are not referenced in the body text: " + formatPlainNumbers(missingMentions, "Fig.") + ".");
        }
    }

    private List<FigureCaption> extractFigureCaptions(String text) {
        List<FigureCaption> captions = new ArrayList<>();
        Pattern figureCaptionPattern = getFigureCaptionPattern();

        for (String line : text.split("\\R")) {
            String normalizedLine = line.replaceAll("\\s+", " ").trim();
            if (normalizedLine.isBlank()) {
                continue;
            }

            Matcher matcher = figureCaptionPattern.matcher(normalizedLine);
            if (!matcher.matches()) {
                continue;
            }

            int figureNumber = Integer.parseInt(matcher.group(1));
            boolean preferred = normalizedLine.matches("(?i)^fig\\.\\s*\\d+\\s*\\.\\s+.+$");
            captions.add(new FigureCaption(figureNumber, normalizedLine, preferred));
        }

        return captions;
    }

    private Pattern getFigureCaptionPattern() {
        return Pattern.compile("(?i)^\\s*(?:fig\\.|figure)\\s*(\\d+)\\s*\\.\\s+.+$");
    }

    private boolean detectFigureCaptions(String text) {
        return !extractFigureCaptions(text).isEmpty();
    }

    private boolean detectPdfImages(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) {
                continue;
            }

            for (COSName xObjectName : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(xObjectName);
                if (xObject instanceof PDImageXObject) {
                    return true;
                }
            }
        }

        return false;
    }

    private void checkTableCaptionFormat(List<TableCaption> tableCaptions, ComplianceReport report) {
        boolean allPreferred = tableCaptions.stream().allMatch(TableCaption::preferredFormat);
        if (allPreferred) {
            report.addPass(RuleCode.TABLE_CAPTION_FORMAT, "Detected table captions follow the preferred IEEE-style Roman numeral format.");
            return;
        }

        report.addWarn(RuleCode.TABLE_CAPTION_FORMAT,
                "One or more table captions use a non-preferred numeric format. IEEE style prefers 'TABLE <Roman numeral>'.");
    }

    private void checkTableNumberingSequence(List<TableCaption> tableCaptions, ComplianceReport report) {
        if (tableCaptions.size() < 2) {
            report.addPass(RuleCode.TABLE_NUMBERING_SEQUENCE, "Fewer than two table captions were detected, so table numbering is acceptable.");
            return;
        }

        String numberingIssue = validateSequentialNumbers(
                tableCaptions.stream().map(TableCaption::number).toList(),
                "Table");
        if (numberingIssue == null) {
            report.addPass(RuleCode.TABLE_NUMBERING_SEQUENCE, "Table captions are numbered sequentially.");
        } else {
            report.addWarn(RuleCode.TABLE_NUMBERING_SEQUENCE, numberingIssue);
        }
    }

    private void checkTableInTextMentions(String fullText, List<TableCaption> tableCaptions, ComplianceReport report) {
        String bodyText = removeExactLines(fullText, tableCaptions.stream().map(TableCaption::rawLine).toList());
        List<String> missingMentions = new ArrayList<>();

        for (TableCaption caption : tableCaptions) {
            String romanNumber = toRoman(caption.number());
            Pattern mentionPattern = Pattern.compile(
                    "(?i)\\btable\\s*(?:" + Pattern.quote(caption.displayNumber()) + "|" + Pattern.quote(String.valueOf(caption.number())) + "|" + Pattern.quote(romanNumber) + ")\\b"
            );
            if (!mentionPattern.matcher(bodyText).find()) {
                missingMentions.add("Table " + caption.displayNumber());
            }
        }

        if (missingMentions.isEmpty()) {
            report.addPass(RuleCode.TABLE_IN_TEXT_MENTION, "Every detected table caption is referenced in the body text.");
        } else {
            report.addWarn(RuleCode.TABLE_IN_TEXT_MENTION,
                    "Some tables are not referenced in the body text: " + String.join(", ", missingMentions) + ".");
        }
    }

    private List<TableCaption> extractTableCaptions(String text) {
        List<TableCaption> captions = new ArrayList<>();
        Pattern tableCaptionPattern = getTableCaptionPattern();

        for (String line : text.split("\\R")) {
            String normalizedLine = line.replaceAll("\\s+", " ").trim();
            if (normalizedLine.isBlank()) {
                continue;
            }

            Matcher matcher = tableCaptionPattern.matcher(normalizedLine);
            if (!matcher.matches()) {
                continue;
            }

            String numberToken = matcher.group(1);
            String trailingTitle = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (trailingTitle != null && startsWithBodySentenceVerb(trailingTitle)) {
                continue;
            }

            int tableNumber = parseSectionNumber(numberToken);
            if (tableNumber <= 0) {
                continue;
            }

            boolean preferred = isRomanNumeral(numberToken);
            captions.add(new TableCaption(tableNumber, numberToken.toUpperCase(Locale.ROOT), normalizedLine, preferred));
        }

        return captions;
    }

    private Pattern getTableCaptionPattern() {
        return Pattern.compile("(?i)^\\s*table\\s+([ivxlcdm]+|\\d+)\\b(?:\\s*\\.\\s*(.+)|\\s+(.+))?$");
    }

    private boolean detectTableCaptions(String text) {
        return !extractTableCaptions(text).isEmpty();
    }

    private boolean detectTableLikeText(String text) {
        String[] lines = text.split("\\R");
        int structuredLines = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isBlank() || line.length() < 8) {
                continue;
            }

            boolean hasColumnSpacing = rawLine.matches(".*\\S\\s{2,}\\S.*");
            boolean hasTabSpacing = rawLine.contains("\t");
            boolean hasRepeatedSeparators = rawLine.matches(".*(?:\\|.*\\|.|-{3,}|={3,}).*");

            if (hasColumnSpacing || hasTabSpacing || hasRepeatedSeparators) {
                structuredLines++;
                if (structuredLines >= 2) {
                    return true;
                }
            } else {
                structuredLines = 0;
            }
        }

        return false;
    }

    private String removeMatchingLines(String text, Pattern pattern) {
        StringBuilder builder = new StringBuilder();
        for (String line : text.split("\\R")) {
            String normalizedLine = line.replaceAll("\\s+", " ").trim();
            if (!normalizedLine.isBlank() && pattern.matcher(normalizedLine).matches()) {
                continue;
            }
            builder.append(line).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String removeExactLines(String text, List<String> linesToRemove) {
        Set<String> normalizedLinesToRemove = new HashSet<>();
        for (String line : linesToRemove) {
            if (line != null) {
                normalizedLinesToRemove.add(line.replaceAll("\\s+", " ").trim());
            }
        }

        StringBuilder builder = new StringBuilder();
        for (String line : text.split("\\R")) {
            String normalizedLine = line.replaceAll("\\s+", " ").trim();
            if (!normalizedLine.isBlank() && normalizedLinesToRemove.contains(normalizedLine)) {
                continue;
            }
            builder.append(line).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private int parseSectionNumber(String token) {
        String normalized = token.trim();
        if (normalized.matches("\\d+")) {
            return Integer.parseInt(normalized);
        }
        if (isRomanNumeral(normalized)) {
            return romanToInt(normalized);
        }
        return -1;
    }

    private boolean isRomanNumeral(String token) {
        return token != null && token.matches("(?i)[ivxlcdm]+");
    }

    private int romanToInt(String roman) {
        int total = 0;
        int previous = 0;

        for (int i = roman.length() - 1; i >= 0; i--) {
            int current = romanCharValue(Character.toUpperCase(roman.charAt(i)));
            if (current < previous) {
                total -= current;
            } else {
                total += current;
                previous = current;
            }
        }

        return total;
    }

    private String toRoman(int number) {
        int remaining = number;
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                builder.append(numerals[i]);
                remaining -= values[i];
            }
        }

        return builder.toString();
    }

    private int romanCharValue(char value) {
        return switch (value) {
            case 'I' -> 1;
            case 'V' -> 5;
            case 'X' -> 10;
            case 'L' -> 50;
            case 'C' -> 100;
            case 'D' -> 500;
            case 'M' -> 1000;
            default -> 0;
        };
    }

    private List<String> detectEquationLikeLines(String text) {
        List<String> equationLines = new ArrayList<>();

        for (String line : text.split("\\R")) {
            String normalizedLine = line.replaceAll("\\s+", " ").trim();
            if (looksLikeDisplayedEquation(normalizedLine)) {
                equationLines.add(normalizedLine);
            }
        }

        return equationLines;
    }

    private boolean looksLikeDisplayedEquation(String line) {
        if (line.isBlank() || line.length() < 6) {
            return false;
        }

        if (line.matches("^\\[\\d+].*") || line.matches("^[A-Za-z]\\..*")) {
            return false;
        }

        int signalCount = 0;
        if (line.matches(".*\\([^)]*\\)\\s*=\\s*.*") || line.matches(".*\\b\\w+\\s*=\\s*.*")) {
            signalCount++;
        }
        if (line.matches(".*(?:\\+|\\-|\\*|/|=|\\u2211|\\u222B|\\u221A|\\u03C3|\\u03B8|\\u03B2|\\u03B1).*")) {
            signalCount++;
        }
        if (line.matches(".*\\(\\d+\\)\\s*$")) {
            signalCount++;
        }
        if (line.matches("(?i).*(?:sigmoid|argmin|argmax|loss|bce|f\\(x\\)|p\\([^)]*\\)|e\\([^)]*\\)|theta|alpha|beta|sigma).*")) {
            signalCount++;
        }

        boolean longEnough = line.length() >= 15;
        boolean hasMathShape = line.matches(".*[=+\\-*/()].*");

        return signalCount >= 2 && longEnough && hasMathShape;
    }

    private List<Integer> extractEquationNumbers(List<String> equationLines) {
        List<Integer> equationNumbers = new ArrayList<>();
        Pattern equationNumberPattern = Pattern.compile("\\((\\d+)\\)\\s*$");

        for (String line : equationLines) {
            Matcher matcher = equationNumberPattern.matcher(line);
            if (matcher.find()) {
                equationNumbers.add(Integer.parseInt(matcher.group(1)));
            }
        }

        return equationNumbers;
    }

    private void checkEquationNumberingFormat(List<String> equationLines, List<Integer> equationNumbers, ComplianceReport report) {
        if (equationLines.isEmpty()) {
            report.addNotApplicable(RuleCode.EQUATION_NUMBERING_FORMAT, "Equation numbering format was not checked because no equation-like content was detected.");
            return;
        }

        if (!equationNumbers.isEmpty()) {
            report.addPass(RuleCode.EQUATION_NUMBERING_FORMAT, "Displayed equations use numbering patterns such as (1), (2), or (3).");
            return;
        }

        report.addWarn(RuleCode.EQUATION_NUMBERING_FORMAT,
                "Equation-like content was detected, but equation numbers such as (1), (2) were not found.");
    }

    private void checkEquationNumberingSequence(List<Integer> equationNumbers, ComplianceReport report) {
        if (equationNumbers.isEmpty()) {
            report.addNotApplicable(RuleCode.EQUATION_NUMBERING_SEQUENCE, "Equation numbering sequence was not checked because no numbered equations were detected.");
            return;
        }

        if (equationNumbers.size() < 2) {
            report.addPass(RuleCode.EQUATION_NUMBERING_SEQUENCE, "Fewer than two numbered equations were detected, so equation numbering is acceptable.");
            return;
        }

        String numberingIssue = validateSequentialNumbers(equationNumbers, "Equation");
        if (numberingIssue == null) {
            report.addPass(RuleCode.EQUATION_NUMBERING_SEQUENCE, "Equation numbers are sequential.");
        } else {
            report.addWarn(RuleCode.EQUATION_NUMBERING_SEQUENCE, numberingIssue);
        }
    }

    private void checkEquationInTextMentions(String fullText, List<String> equationLines, List<Integer> equationNumbers, ComplianceReport report) {
        if (equationNumbers.isEmpty()) {
            report.addNotApplicable(RuleCode.EQUATION_IN_TEXT_MENTION, "Equation in-text mention checks were skipped because no numbered equations were detected.");
            return;
        }

        String bodyText = removeExactLines(fullText, equationLines);
        List<Integer> missingMentions = new ArrayList<>();

        for (Integer equationNumber : equationNumbers) {
            Pattern mentionPattern = Pattern.compile(
                    "(?i)(?:\\beq\\.?\\s*\\(" + equationNumber + "\\)"
                            + "|\\bequation\\s*\\(" + equationNumber + "\\)"
                            + "|\\b(?:using|from|in|see)\\s*\\(" + equationNumber + "\\))"
            );
            if (!mentionPattern.matcher(bodyText).find()) {
                missingMentions.add(equationNumber);
            }
        }

        if (missingMentions.isEmpty()) {
            report.addPass(RuleCode.EQUATION_IN_TEXT_MENTION, "Every numbered equation is referenced in the body text.");
        } else {
            report.addWarn(RuleCode.EQUATION_IN_TEXT_MENTION,
                    "Some numbered equations are not referenced in the body text: " + formatParenthesizedNumbers(missingMentions) + ".");
        }
    }

    private String formatParenthesizedNumbers(List<Integer> numbers) {
        return numbers.stream()
                .sorted()
                .map(number -> "(" + number + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private boolean startsWithBodySentenceVerb(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return normalized.matches("^(shows|show|illustrates|illustrate|further illustrates|presents|present|compares|compare|demonstrates|demonstrate|summarizes|summarize|lists|list|reports|report|contains|contain|provides|provide|describes|describe)\\b.*");
    }

    private void collectFontNames(PDFont font, Set<String> detectedFonts, Set<String> normalizedFonts) {
        if (font == null) {
            return;
        }

        addFontCandidate(font.getName(), detectedFonts, normalizedFonts);

        PDFontDescriptor descriptor = font.getFontDescriptor();
        if (descriptor != null) {
            addFontCandidate(descriptor.getFontName(), detectedFonts, normalizedFonts);
            addFontCandidate(descriptor.getFontFamily(), detectedFonts, normalizedFonts);
        }
    }

    private void addFontCandidate(String rawFontName, Set<String> detectedFonts, Set<String> normalizedFonts) {
        if (rawFontName == null) {
            return;
        }

        String trimmed = rawFontName.trim();
        if (trimmed.isBlank()) {
            return;
        }

        String withoutSubsetPrefix = trimmed.replaceFirst("^[A-Z]{6}\\+", "");
        detectedFonts.add(withoutSubsetPrefix);
        normalizedFonts.add(normalizeFontName(withoutSubsetPrefix));
    }

    private String normalizeFontName(String fontName) {
        return fontName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\-,]+", "");
    }

    private boolean containsCompatibleSerifFont(Set<String> normalizedFonts) {
        for (String font : normalizedFonts) {
            if (font.contains("timesnewroman")
                    || font.equals("times")
                    || font.contains("timesroman")
                    || font.contains("timesnewromanpsmt")
                    || font.contains("nimbusroman")
                    || font.contains("nimbusromno9l")
                    || font.contains("liberationserif")
                    || font.contains("texgyretermes")
                    || font.equals("cmr")
                    || font.startsWith("cmr")
                    || font.contains("computermodernroman")) {
                return true;
            }
        }
        return false;
    }

    private Set<String> findIncompatibleRealFonts(Set<String> detectedFonts) {
        Set<String> incompatibleFonts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (String font : detectedFonts) {
            if (font == null || font.isBlank() || isUnresolvedFontAlias(font)) {
                continue;
            }

            String normalized = normalizeFontName(font);
            if (normalized.contains("arial")
                    || normalized.contains("calibri")
                    || normalized.contains("helvetica")
                    || normalized.contains("verdana")
                    || normalized.contains("tahoma")
                    || normalized.contains("courier")) {
                incompatibleFonts.add(font);
            }
        }

        return incompatibleFonts;
    }

    private boolean containsOnlyFontAliases(Set<String> detectedFonts) {
        if (detectedFonts.isEmpty()) {
            return true;
        }

        for (String font : detectedFonts) {
            if (!isUnresolvedFontAlias(font)) {
                return false;
            }
        }

        return true;
    }

    private boolean isUnresolvedFontAlias(String fontName) {
        if (fontName == null || fontName.isBlank()) {
            return true;
        }

        String trimmed = fontName.trim();
        String collapsedAlias = trimmed
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\-,+]+", "");

        return trimmed.matches("(?i)f\\d+")
                || trimmed.matches("(?i)cidfont\\+?f\\d+")
                || collapsedAlias.matches("f\\d+")
                || collapsedAlias.matches("cidfontf\\d+");
    }

    private boolean isTextJustified(List<TextPosition> textPositions) {
        if (textPositions.size() < 2) return false;

        float avgSpacing = 0;
        for (int i = 1; i < textPositions.size(); i++) {
            avgSpacing += Math.abs(textPositions.get(i).getX() - textPositions.get(i - 1).getEndX());
        }
        avgSpacing /= (textPositions.size() - 1);

        return avgSpacing < 2.0;
    }

    private record ReferencesSection(String bodyText, String sectionText, boolean usesBibliographyHeading) {
    }

    private record FigureCaption(int number, String rawLine, boolean preferredFormat) {
    }

    private record TableCaption(int number, String displayNumber, String rawLine, boolean preferredFormat) {
    }
}
