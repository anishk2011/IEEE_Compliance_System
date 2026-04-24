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
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private boolean isTextJustified(List<TextPosition> textPositions) {
        if (textPositions.size() < 2) return false;

        float avgSpacing = 0;
        for (int i = 1; i < textPositions.size(); i++) {
            avgSpacing += Math.abs(textPositions.get(i).getX() - textPositions.get(i - 1).getEndX());
        }
        avgSpacing /= (textPositions.size() - 1);

        return avgSpacing < 2.0;
    }
}
