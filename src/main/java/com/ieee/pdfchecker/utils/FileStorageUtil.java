package com.ieee.pdfchecker.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FileStorageUtil {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private final Path uploadRoot;

    public FileStorageUtil(@Value("${app.file-storage.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public Path storePdf(MultipartFile file) throws IOException {
        Files.createDirectories(uploadRoot);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "paper.pdf";
        String safeName = sanitizeFileName(originalName);
        String storedName = FILE_TS.format(LocalDateTime.now()) + "_" + safeName;
        Path target = uploadRoot.resolve(storedName).normalize();

        if (!target.startsWith(uploadRoot)) {
            throw new IOException("Resolved upload path is invalid.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    private String sanitizeFileName(String fileName) {
        String cleaned = Paths.get(fileName).getFileName().toString();
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "paper.pdf" : cleaned;
    }
}
