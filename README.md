# IEEE Compliance Platform

This project is a Spring Boot backend for an IEEE paper compliance platform. It accepts PDF uploads, evaluates them against IEEE-style formatting rules, stores every check result in a database, tracks multiple versions of the same paper, and exposes analytics for recurring formatting problems and improvement trends over time.

This repository contains both the backend and frontend required to demonstrate a complete end-to-end system.

## Project Overview

The platform solves a simple workflow:

1. A user uploads a paper PDF through the frontend.
2. The backend analyzes the PDF using compliance rules.
3. The compliance result is returned immediately to the frontend.
4. The same result is persisted as a paper version with rule-wise saved results.
5. Users can review paper history, compare versions, and inspect analytics across all checks.

This makes the project a complete compliance tracking platform rather than just a one-time checker.

## Core Features

- PDF upload and IEEE compliance checking
- Rule-wise pass/fail reporting with suggestions
- Persistent paper history using MySQL
- Version tracking for repeated uploads of the same paper
- Saved report retrieval for past versions
- Version comparison using stored rule results
- Analytics for common mistakes and improvement trends

## Architecture / Backend Flow

The backend follows a simple layered design:

1. `PdfController` receives the uploaded PDF at `POST /api/pdf/upload`
2. `PdfService` delegates the request to the orchestration layer
3. `ComplianceCheckService` runs the analysis and persists results
4. `RuleEngine` performs PDF checks using PDFBox/iText logic
5. Data is stored in MySQL using Spring Data JPA
6. `PaperController` exposes history, report, and comparison APIs
7. `AnalyticsController` exposes analytics endpoints

## Flow:

```text
Upload PDF
  -> PdfController
  -> PdfService
  -> ComplianceCheckService
  -> RuleEngine
  -> ComplianceReport
  -> Save Paper / PaperVersion / RuleResult
  -> Return response to frontend
```

## Database Entities

### `Paper`

Represents a logical paper tracked by the platform.

- `id`
- `displayName`
- `createdAt`

### `PaperVersion`

Represents one uploaded version of a paper.

- `id`
- `paper`
- `versionNumber`
- `originalFileName`
- `storedFilePath`
- `overallCompliant`
- `score`
- `createdAt`

Note:
For demo simplicity, uploaded PDFs are stored in a local backend uploads directory, and `storedFilePath` contains that saved local path.

### `RuleResult`

Represents one saved rule outcome for a specific paper version.

- `id`
- `paperVersion`
- `ruleCode`
- `ruleName`
- `status`
- `message`
- `suggestion`
- `severity`

## API Endpoints

### Endpoint Summary

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/pdf/upload` | Upload and check PDF |
| `GET` | `/api/papers` | List all tracked papers with latest version summary |
| `GET` | `/api/papers/{paperId}/versions` | List all versions for one paper |
| `GET` | `/api/papers/{paperId}/versions/{versionId}/report` | Fetch the saved report for a specific version |
| `GET` | `/api/papers/{paperId}/progress` | Show version-by-version score trend |
| `GET` | `/api/papers/{paperId}/compare?fromVersion=1&toVersion=2` | Compare two versions of the same paper |
| `GET` | `/api/analytics/common-mistakes` | Show most frequent failed rules across all saved results |
| `GET` | `/api/analytics/summary` | Return dashboard-style platform metrics |
| `GET` | `/api/analytics/improvement-trends` | Show aggregate improvement trends across papers |
| `GET` | `/api/analytics/papers/{paperId}/mistake-breakdown` | Show failed-rule breakdown for a single paper |

## Frontend

The frontend is included in this repository under the `frontend/` directory.

It is a simple React (Vite) single-page application that:
- allows users to upload PDF files
- displays compliance results (score, rule-wise output, suggestions)
- supports version uploads using paperId
- shows basic analytics (summary and common mistakes)

### Run Frontend

bash - 
cd frontend
npm install
npm run dev

Open in browser: http://localhost:3000

## Backend Setup -
Prerequisites
Java 21
MySQL

### Environment Variables
DB_URL=jdbc:mysql://localhost:3306/ieee_compliance
DB_USERNAME=root
DB_PASSWORD=your_password
JPA_DDL_AUTO=update
UPLOAD_DIR=uploads
PORT=8080

Run Backend
./mvnw spring-boot:run

Backend runs on: http://localhost:8080

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- Apache PDFBox
- iText
- React (Vite)
