package com.ieee.pdfchecker.controller;
import com.ieee.pdfchecker.dto.ApiErrorResponse;
import com.ieee.pdfchecker.reports.ComplianceReport;
import com.ieee.pdfchecker.services.PdfService;
import com.ieee.pdfchecker.utils.FileStorageUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

@CrossOrigin(origins = {
        "https://pdfcompliance.vercel.app",
        "http://localhost:3000"
})
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private static final Logger logger = LoggerFactory.getLogger(PdfController.class);
    private final PdfService pdfService;
    private final FileStorageUtil fileStorageUtil;

    public PdfController(PdfService pdfService, FileStorageUtil fileStorageUtil) {
        this.pdfService = pdfService;
        this.fileStorageUtil = fileStorageUtil;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "paperId", required = false) Long paperId,
                                       HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            logger.warn("Rejected upload request [{}]: empty or missing file", request.getRequestURI());
            return badRequest("Uploaded file is missing or empty.", request.getRequestURI());
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            logger.warn("Rejected upload request [{}]: invalid file type [{}]", request.getRequestURI(), originalFilename);
            return badRequest("Only PDF uploads are supported.", request.getRequestURI());
        }

        try {
            Path storedPath = fileStorageUtil.storePdf(file);
            File storedFile = storedPath.toFile();

            ComplianceReport report = pdfService.processPdf(storedFile, originalFilename, paperId);
            return ResponseEntity.ok(report);

        } catch (IllegalArgumentException e) {
            logger.warn("Upload request failed validation [{}]: {}", request.getRequestURI(), e.getMessage());
            return badRequest(e.getMessage() != null ? e.getMessage() : "Invalid upload request.", request.getRequestURI());
        } catch (IOException e) {
            logger.error("PDF upload or processing failed [{}]", request.getRequestURI(), e);
            return error(HttpStatus.BAD_REQUEST, "Failed to read or process the uploaded PDF.", request.getRequestURI());
        } catch (RuntimeException e) {
            logger.error("Unexpected PDF processing failure [{}]", request.getRequestURI(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error while processing the PDF.", request.getRequestURI());
        }
    }

    private ResponseEntity<ApiErrorResponse> badRequest(String message, String path) {
        return error(HttpStatus.BAD_REQUEST, message, path);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        path
                )
        );
    }
}
