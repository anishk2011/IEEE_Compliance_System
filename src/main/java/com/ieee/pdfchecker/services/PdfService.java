package com.ieee.pdfchecker.services;

import com.ieee.pdfchecker.reports.ComplianceReport;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class PdfService {

    private final ComplianceCheckService complianceCheckService;

    public PdfService(ComplianceCheckService complianceCheckService) {
        this.complianceCheckService = complianceCheckService;
    }

    public ComplianceReport processPdf(File file) {
        return processPdf(file, file.getName(), null);
    }

    public ComplianceReport processPdf(File file, String originalFileName, Long paperId) {
        return complianceCheckService.analyzeAndSave(file, originalFileName, paperId);
    }
}
