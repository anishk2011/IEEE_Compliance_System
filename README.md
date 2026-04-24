# IEEE Compliance Platform Backend

This project is a Spring Boot backend for an IEEE paper compliance platform. It accepts PDF uploads, evaluates them against IEEE-style formatting rules, stores every check result in a database, tracks multiple versions of the same paper, and exposes analytics for recurring formatting problems and improvement trends over time.

The backend is designed for a college project/demo setting where the system should demonstrate more than one-time validation. In its current form, it supports upload-based compliance checking, paper history, version comparison, saved reports, and platform-level analytics from persisted results.

## Project Overview

The platform solves a simple workflow:

1. A user uploads a paper PDF.
2. The backend analyzes the PDF using the existing compliance rules.
3. The compliance result is returned immediately to the frontend.
4. The same result is persisted as a paper version with rule-wise saved results.
5. Users can later review paper history, compare versions, and inspect analytics across all checks.

This makes the project suitable not only as a checker, but as a lightweight compliance tracking system.

## Core Features

- PDF upload and IEEE compliance checking
- Rule-wise pass/fail reporting
- Persistent paper history
- Version tracking for repeated uploads of the same paper
- Saved report retrieval for past versions
- Version comparison using stored rule results
- Analytics for common mistakes and improvement trends

## Architecture / Backend Flow

The backend follows a simple layered design:

1. `PdfController` receives the uploaded PDF at `POST /api/pdf/upload`.
2. `PdfService` delegates the request to the orchestration layer.
3. `ComplianceCheckService` runs the existing analysis and then persists the result.
4. `RuleEngine` performs the actual PDF checks using PDFBox/iText-based logic.
5. Repositories store `Paper`, `PaperVersion`, and `RuleResult` records in MySQL through Spring Data JPA.
6. `PaperController` exposes read APIs for history, saved reports, progress, and comparison.
7. `AnalyticsController` exposes dashboard-style analytics over persisted data.

High-level request flow:

```text
Upload PDF
  -> PdfController
  -> PdfService
  -> ComplianceCheckService
  -> RuleEngine
  -> ComplianceReport
  -> Save Paper / PaperVersion / RuleResult
  -> Return compliance response
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
| `POST` | `/api/pdf/upload` | Upload and check a PDF, optionally as a new version of an existing paper |
| `GET` | `/api/papers` | List all tracked papers with latest version summary |
| `GET` | `/api/papers/{paperId}/versions` | List all versions for one paper |
| `GET` | `/api/papers/{paperId}/versions/{versionId}/report` | Fetch the saved report for a specific version |
| `GET` | `/api/papers/{paperId}/progress` | Show version-by-version score trend |
| `GET` | `/api/papers/{paperId}/compare?fromVersion=1&toVersion=2` | Compare two versions of the same paper |
| `GET` | `/api/analytics/common-mistakes` | Show most frequent failed rules across all saved results |
| `GET` | `/api/analytics/summary` | Return dashboard-style platform metrics |
| `GET` | `/api/analytics/improvement-trends` | Show aggregate improvement trends across papers |
| `GET` | `/api/analytics/papers/{paperId}/mistake-breakdown` | Show failed-rule breakdown for a single paper |

### Upload Endpoint

#### `POST /api/pdf/upload`

Uploads a PDF, checks compliance, persists the result, and returns the current compliance response.

Request:

- form-data field `file` as `MultipartFile`
- optional form-data field `paperId`

Behavior:

- if `paperId` is not provided, a new `Paper` is created
- if `paperId` is provided, the upload is stored as the next `PaperVersion`
- the current analysis result is still returned immediately to the frontend

Response includes the usual compliance output plus persisted identifiers such as:

- `paperId`
- `versionId`
- `score`
- `items`

### Paper History Endpoints

#### `GET /api/papers`

Returns all tracked papers with summary fields:

- `paperId`
- `displayName`
- `createdAt`
- `latestVersionNumber`
- `latestScore`
- `totalVersions`

#### `GET /api/papers/{paperId}/versions`

Returns all saved versions for a paper:

- `versionId`
- `versionNumber`
- `originalFileName`
- `createdAt`
- `overallCompliant`
- `score`

#### `GET /api/papers/{paperId}/versions/{versionId}/report`

Returns the saved report for a single version:

- `paperId`
- `versionId`
- `score`
- `overallCompliant`
- saved `RuleResult` rows with rule name, code, status, message, suggestion, and severity

### Comparison Endpoint

#### `GET /api/papers/{paperId}/compare?fromVersion=1&toVersion=2`

Compares two saved versions of the same paper using stored `RuleResult` rows and their `ruleCode`.

Response groups rules into:

- `improved`
- `worsened`
- `unchanged`

Each comparison item includes:

- `ruleCode`
- `ruleName`
- `fromStatus`
- `toStatus`

### Analytics Endpoints

#### `GET /api/analytics/common-mistakes`

Shows the most frequently failed rules across all persisted rule results.

Each item includes:

- `ruleCode`
- `ruleName`
- `failCount`
- `affectedPaperCount`
- `latestSuggestion`

#### `GET /api/analytics/summary`

Returns overall platform metrics:

- `totalPapers`
- `totalVersions`
- `totalChecks`
- `averageScore`
- `overallComplianceRate`
- `totalFailedRuleResults`
- `totalPassedRuleResults`
- `mostCommonFailedRuleCode`
- `mostCommonFailedRuleName`

#### `GET /api/analytics/improvement-trends`

Returns aggregate trend information based on the first and latest version of each paper with at least two versions:

- `totalPapersCompared`
- `papersImproved`
- `papersWorsened`
- `papersUnchanged`
- `averageScoreDelta`

#### `GET /api/analytics/papers/{paperId}/mistake-breakdown`

Returns failed-rule breakdown for one paper across all versions:

- `ruleCode`
- `ruleName`
- `failCount`
- `firstSeenVersion`
- `lastSeenVersion`

## How Version Tracking Works

Version tracking is based on the `Paper` and `PaperVersion` tables:

1. The first upload creates a new `Paper`.
2. The upload is stored as version `1`.
3. Later uploads can pass the same `paperId`.
4. The backend finds the latest version number and creates the next version.
5. Each version stores:
   - file metadata
   - overall compliance status
   - score
   - full saved rule-wise results

This allows the frontend or evaluator to inspect how a paper improves over time rather than treating each check as isolated.

## Analytics Capabilities

The analytics layer is intentionally simple but useful for demo and review:

- identify the most common formatting mistakes across all uploads
- view overall system-wide compliance statistics
- track whether papers improve across revisions
- inspect repeated failures within a single paper

These analytics are calculated from persisted `PaperVersion` and `RuleResult` data, which makes the platform suitable for history-aware review and presentation.

## Local Setup Instructions

### Prerequisites

- Java 21
- Maven Wrapper (`mvnw` is included)
- MySQL running locally or remotely

### Environment Variables

The application expects database configuration through environment variables:

```bash
DB_URL=jdbc:mysql://localhost:3306/ieee_compliance
DB_USERNAME=root
DB_PASSWORD=your_password
JPA_DDL_AUTO=update
UPLOAD_DIR=uploads
PORT=8080
```

### Run Locally

1. Create a MySQL database, for example `ieee_compliance`.
2. Set the environment variables above.
3. Start the backend:

```bash
./mvnw spring-boot:run
```

4. The server will run on:

```text
http://localhost:8080
```

## Example Usage Flow

### 1. Upload a new paper

Send a multipart request to:

```text
POST /api/pdf/upload
```

with:

- `file`: the PDF file

The backend:

- analyzes the PDF
- creates a new `Paper`
- creates version `1`
- stores all rule results
- returns the compliance response

### 2. Upload an improved version

Send another multipart request to:

```text
POST /api/pdf/upload
```

with:

- `file`: the updated PDF
- `paperId`: the existing paper ID

The backend:

- runs the same compliance analysis
- creates the next `PaperVersion`
- stores new rule results
- returns the latest response

### 3. View paper history

Use:

```text
GET /api/papers
GET /api/papers/{paperId}/versions
```

to inspect saved papers and their versions.

### 4. Compare versions

Use:

```text
GET /api/papers/{paperId}/compare?fromVersion=1&toVersion=2
```

to see which rules improved, worsened, or stayed unchanged.

### 5. View analytics

Use:

```text
GET /api/analytics/summary
GET /api/analytics/common-mistakes
GET /api/analytics/improvement-trends
```

to demonstrate platform-level insights across uploads.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL
- Apache PDFBox
- iText

## Frontend

Frontend repository:

`https://github.com/1Ninad/Compliance-Checker-Frontend`
