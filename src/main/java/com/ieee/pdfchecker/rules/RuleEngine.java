package com.ieee.pdfchecker.rules;

import com.ieee.pdfchecker.reports.ComplianceReport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        boolean foundValidFont = false;
        Set<String> detectedFonts = new HashSet<>();

        List<String> validFonts = Arrays.asList(
                "timesnewroman", "times-roman", "timesroman", "times",
                "nimbusromno9l-regu", "nimbusromno9l-medi", "nimbusromno9l-reguital", "nimbusromno9l-mediital",
                "cmr", "cmm", "cmmi", "cmsy", "cmex"
        );

        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            document.save(outStream);
            ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
            com.itextpdf.kernel.pdf.PdfReader reader = new com.itextpdf.kernel.pdf.PdfReader(inStream);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(reader);
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                com.itextpdf.kernel.pdf.PdfPage page = pdfDoc.getPage(i);
                com.itextpdf.kernel.pdf.PdfDictionary resources = page.getPdfObject()
                        .getAsDictionary(com.itextpdf.kernel.pdf.PdfName.Resources);
                if (resources == null) continue;
                com.itextpdf.kernel.pdf.PdfDictionary fonts = resources.getAsDictionary(com.itextpdf.kernel.pdf.PdfName.Font);
                if (fonts == null) continue;
                for (com.itextpdf.kernel.pdf.PdfName fontKey : fonts.keySet()) {
                    com.itextpdf.kernel.pdf.PdfDictionary fontDict = fonts.getAsDictionary(fontKey);
                    if (fontDict == null) continue;
                    com.itextpdf.kernel.pdf.PdfName baseFont = fontDict.getAsName(com.itextpdf.kernel.pdf.PdfName.BaseFont);
                    if (baseFont == null) continue;
                    String fontNameStr = baseFont.getValue();
                    String cleanFontName = fontNameStr.contains("+")
                            ? fontNameStr.substring(fontNameStr.indexOf("+") + 1)
                            : fontNameStr;
                    String normalizedFont = cleanFontName.toLowerCase().replaceAll("\\s+", "");
                    detectedFonts.add(cleanFontName);
                    for (String valid : validFonts) {
                        if (normalizedFont.contains(valid)) {
                            foundValidFont = true;
                            break;
                        }
                    }
                }
                if (foundValidFont) {
                    report.addInfo("Font is compliant. Times New Roman or an equivalent font was detected.");
                    report.setFontCompliant(true);
                    break;
                }
            }
            pdfDoc.close();
        } catch (Exception e) {
            report.addError("Error checking font using iText: " + e.getMessage());
            return;
        }

        if (!foundValidFont) {
            String fontsList = String.join(", ", detectedFonts);
            report.addError("Times New Roman or an equivalent font was not detected. Detected fonts: " + fontsList);
            report.setFontCompliant(false);
        }
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
        if (containsSectionHeading(fullText,
                "Conclusion",
                "Conclusions",
                "Conclusion and Future Work",
                "Conclusion & Future Work")) {
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

    private String formatNumbers(Set<Integer> numbers) {
        return numbers.stream()
                .sorted()
                .map(number -> "[" + number + "]")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
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
}
