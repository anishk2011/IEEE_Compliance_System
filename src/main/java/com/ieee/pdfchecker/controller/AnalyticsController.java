package com.ieee.pdfchecker.controller;

import com.ieee.pdfchecker.dto.ApiErrorResponse;
import com.ieee.pdfchecker.dto.AnalyticsSummaryDto;
import com.ieee.pdfchecker.dto.CommonMistakeDto;
import com.ieee.pdfchecker.dto.ImprovementTrendsDto;
import com.ieee.pdfchecker.dto.PaperMistakeBreakdownDto;
import com.ieee.pdfchecker.services.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = {
        "https://pdfcompliance.vercel.app",
        "http://localhost:3000"
})
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/common-mistakes")
    public ResponseEntity<List<CommonMistakeDto>> getCommonMistakes() {
        return ResponseEntity.ok(analyticsService.getCommonMistakes());
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDto> getSummary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    @GetMapping("/improvement-trends")
    public ResponseEntity<ImprovementTrendsDto> getImprovementTrends() {
        return ResponseEntity.ok(analyticsService.getImprovementTrends());
    }

    @GetMapping("/papers/{paperId}/mistake-breakdown")
    public ResponseEntity<?> getPaperMistakeBreakdown(@PathVariable Long paperId, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(analyticsService.getPaperMistakeBreakdown(paperId));
        } catch (IllegalArgumentException e) {
            logger.warn("Analytics lookup failed [{}]: {}", request.getRequestURI(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new ApiErrorResponse(
                            LocalDateTime.now(),
                            HttpStatus.NOT_FOUND.value(),
                            HttpStatus.NOT_FOUND.getReasonPhrase(),
                            e.getMessage() != null ? e.getMessage() : "Requested analytics resource was not found.",
                            request.getRequestURI()
                    )
            );
        }
    }
}
