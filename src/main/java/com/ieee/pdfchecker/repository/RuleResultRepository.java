package com.ieee.pdfchecker.repository;

import com.ieee.pdfchecker.entity.RuleResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleResultRepository extends JpaRepository<RuleResult, Long> {

    List<RuleResult> findByPaperVersionId(Long paperVersionId);

    List<RuleResult> findByPaperVersionIdOrderByIdAsc(Long paperVersionId);
}
