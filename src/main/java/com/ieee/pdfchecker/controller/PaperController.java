package com.ieee.pdfchecker.controller;

import com.ieee.pdfchecker.dto.ApiErrorResponse;
import com.ieee.pdfchecker.dto.PaperProgressDto;
import com.ieee.pdfchecker.dto.PaperSummaryDto;
import com.ieee.pdfchecker.dto.PaperVersionDto;
import com.ieee.pdfchecker.dto.SavedReportDto;
import com.ieee.pdfchecker.dto.VersionComparisonDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import com.ieee.pdfchecker.services.PaperQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = {
        "https://pdfcompliance.vercel.app",
        "http://localhost:3000"
})
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private static final Logger logger = LoggerFactory.getLogger(PaperController.class);
    private final PaperQueryService paperQueryService;

    public PaperController(PaperQueryService paperQueryService) {
        this.paperQueryService = paperQueryService;
    }

    @GetMapping
    public ResponseEntity<List<PaperSummaryDto>> getAllPapers() {
        return ResponseEntity.ok(paperQueryService.getAllPapers());
    }

    @GetMapping("/{paperId}/versions")
    public ResponseEntity<?> getVersions(@PathVariable Long paperId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(paperQueryService.getVersions(paperId));
        } catch (IllegalArgumentException e) {
            logger.warn("Paper lookup failed [{}]: {}", request.getRequestURI(), e.getMessage());
            return notFound(e.getMessage(), request.getRequestURI());
        }
    }

    @GetMapping("/{paperId}/versions/{versionId}/report")
    public ResponseEntity<?> getSavedReport(@PathVariable Long paperId,
                                            @PathVariable Long versionId,
                                            HttpServletRequest request) {
        try {
            return ResponseEntity.ok(paperQueryService.getSavedReport(paperId, versionId));
        } catch (IllegalArgumentException e) {
            logger.warn("Saved report lookup failed [{}]: {}", request.getRequestURI(), e.getMessage());
            return notFound(e.getMessage(), request.getRequestURI());
        }
    }

    @GetMapping("/{paperId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable Long paperId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(paperQueryService.getProgress(paperId));
        } catch (IllegalArgumentException e) {
            logger.warn("Paper progress lookup failed [{}]: {}", request.getRequestURI(), e.getMessage());
            return notFound(e.getMessage(), request.getRequestURI());
        }
    }

    @GetMapping("/{paperId}/compare")
    public ResponseEntity<?> compareVersions(@PathVariable Long paperId,
                                             @RequestParam Integer fromVersion,
                                             @RequestParam Integer toVersion,
                                             HttpServletRequest request) {
        try {
            return ResponseEntity.ok(paperQueryService.compareVersions(paperId, fromVersion, toVersion));
        } catch (IllegalArgumentException e) {
            logger.warn("Paper comparison failed [{}]: {}", request.getRequestURI(), e.getMessage());
            return notFound(e.getMessage(), request.getRequestURI());
        }
    }

    private ResponseEntity<ApiErrorResponse> notFound(String message, String path) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        HttpStatus.NOT_FOUND.getReasonPhrase(),
                        message != null ? message : "Requested paper resource was not found.",
                        path
                )
        );
    }
}
