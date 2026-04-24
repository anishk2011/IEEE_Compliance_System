package com.ieee.pdfchecker.repository;

import com.ieee.pdfchecker.entity.PaperVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperVersionRepository extends JpaRepository<PaperVersion, Long> {

    List<PaperVersion> findByPaperIdOrderByVersionNumberDesc(Long paperId);

    List<PaperVersion> findByPaperIdOrderByVersionNumberAsc(Long paperId);

    Optional<PaperVersion> findTopByPaperIdOrderByVersionNumberDesc(Long paperId);

    Optional<PaperVersion> findByPaperIdAndId(Long paperId, Long id);

    Optional<PaperVersion> findByPaperIdAndVersionNumber(Long paperId, Integer versionNumber);
}
