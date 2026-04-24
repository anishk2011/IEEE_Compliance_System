package com.ieee.pdfchecker.repository;

import com.ieee.pdfchecker.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<Paper, Long> {
}
