import { useEffect, useMemo, useState } from "react";

const API_BASE_URL = "http://localhost:8080";

const emptyAnalytics = {
  summary: null,
  commonMistakes: []
};

function App() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [paperId, setPaperId] = useState("");
  const [uploadResult, setUploadResult] = useState(null);
  const [uploadError, setUploadError] = useState("");
  const [analyticsError, setAnalyticsError] = useState("");
  const [analytics, setAnalytics] = useState(emptyAnalytics);
  const [isUploading, setIsUploading] = useState(false);
  const [isAnalyticsLoading, setIsAnalyticsLoading] = useState(true);

  const normalizedItems = useMemo(() => {
    if (!uploadResult?.items) {
      return [];
    }

    return uploadResult.items.map((item, index) => ({
      id: `${item.ruleCode || item.rule || "rule"}-${index}`,
      ruleName: item.rule || item.ruleName || "Unknown rule",
      status: (item.status || "unknown").toUpperCase(),
      message: item.message || "-",
      suggestion: item.suggestion || "No suggestion provided"
    }));
  }, [uploadResult]);

  useEffect(() => {
    void loadAnalytics();
  }, []);

  async function loadAnalytics() {
    setIsAnalyticsLoading(true);
    setAnalyticsError("");

    try {
      const [summaryResponse, mistakesResponse] = await Promise.all([
        fetch(`${API_BASE_URL}/api/analytics/summary`),
        fetch(`${API_BASE_URL}/api/analytics/common-mistakes`)
      ]);

      const summaryData = await parseResponse(summaryResponse);
      const mistakesData = await parseResponse(mistakesResponse);

      setAnalytics({
        summary: summaryData,
        commonMistakes: Array.isArray(mistakesData) ? mistakesData : []
      });
    } catch (error) {
      setAnalyticsError(error.message || "Failed to load analytics.");
    } finally {
      setIsAnalyticsLoading(false);
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!selectedFile) {
      setUploadError("Please choose a PDF file before uploading.");
      return;
    }

    setIsUploading(true);
    setUploadError("");

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);

      if (paperId.trim()) {
        formData.append("paperId", paperId.trim());
      }

      const response = await fetch(`${API_BASE_URL}/api/pdf/upload`, {
        method: "POST",
        body: formData
      });

      const data = await parseResponse(response);
      setUploadResult(data);
      await loadAnalytics();
    } catch (error) {
      setUploadResult(null);
      setUploadError(error.message || "Upload failed.");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <div className="page-shell">
      <main className="page-content">
        <section className="hero card">
          <div>
            <p className="eyebrow">IEEE Compliance Platform</p>
            <h1>Upload a PDF and review compliance results in one place.</h1>
            <p className="hero-copy">
              This demo UI sends files directly to the backend, shows the latest
              rule-wise report, and surfaces simple platform analytics.
            </p>
          </div>
          <div className="hero-meta">
            <span className="meta-chip">Backend: {API_BASE_URL}</span>
            <span className="meta-chip">Single-page demo</span>
          </div>
        </section>

        <section className="grid-layout">
          <div className="card">
            <div className="section-head">
              <div>
                <p className="section-label">Upload PDF</p>
                <h2>Check a new paper or add a new version</h2>
              </div>
            </div>

            <form className="upload-form" onSubmit={handleSubmit}>
              <label className="field">
                <span>PDF file</span>
                <input
                  type="file"
                  accept="application/pdf,.pdf"
                  onChange={(event) =>
                    setSelectedFile(event.target.files?.[0] || null)
                  }
                />
              </label>

              <label className="field">
                <span>Existing paper ID (optional)</span>
                <input
                  type="number"
                  min="1"
                  placeholder="Leave empty to create a new paper"
                  value={paperId}
                  onChange={(event) => setPaperId(event.target.value)}
                />
              </label>

              <button className="primary-button" type="submit" disabled={isUploading}>
                {isUploading ? "Uploading..." : "Upload PDF"}
              </button>
            </form>

            {uploadError ? <p className="feedback error">{uploadError}</p> : null}
          </div>

          <div className="card">
            <div className="section-head">
              <div>
                <p className="section-label">Analytics</p>
                <h2>Simple platform snapshot</h2>
              </div>
              <button
                className="secondary-button"
                type="button"
                onClick={() => void loadAnalytics()}
                disabled={isAnalyticsLoading}
              >
                {isAnalyticsLoading ? "Refreshing..." : "Refresh"}
              </button>
            </div>

            {analyticsError ? <p className="feedback error">{analyticsError}</p> : null}

            <div className="stat-grid">
              <article className="stat-card">
                <span>Total papers</span>
                <strong>
                  {isAnalyticsLoading
                    ? "..."
                    : analytics.summary?.totalPapers ?? 0}
                </strong>
              </article>
              <article className="stat-card">
                <span>Average score</span>
                <strong>
                  {isAnalyticsLoading
                    ? "..."
                    : formatScore(analytics.summary?.averageScore)}
                </strong>
              </article>
              <article className="stat-card">
                <span>Common failed rule</span>
                <strong>
                  {isAnalyticsLoading
                    ? "..."
                    : analytics.summary?.mostCommonFailedRuleName || "No data yet"}
                </strong>
              </article>
            </div>

            <div className="mistakes-list">
              <h3>Most common mistakes</h3>
              {isAnalyticsLoading ? (
                <p className="muted">Loading analytics...</p>
              ) : analytics.commonMistakes.length === 0 ? (
                <p className="muted">No mistakes recorded yet.</p>
              ) : (
                analytics.commonMistakes.slice(0, 5).map((mistake) => (
                  <article className="mistake-item" key={mistake.ruleCode}>
                    <div>
                      <strong>{mistake.ruleName}</strong>
                      <p>
                        Failed {mistake.failCount} time(s) across{" "}
                        {mistake.affectedPaperCount} paper(s)
                      </p>
                    </div>
                    <p>{mistake.latestSuggestion || "No suggestion available"}</p>
                  </article>
                ))
              )}
            </div>
          </div>
        </section>

        <section className="card">
          <div className="section-head">
            <div>
              <p className="section-label">Latest Result</p>
              <h2>Upload response</h2>
            </div>
          </div>

          {!uploadResult ? (
            <p className="muted">
              Upload a PDF to see its score, IDs, and rule-wise results here.
            </p>
          ) : (
            <>
              <div className="result-grid">
                <article className="stat-card">
                  <span>Score</span>
                  <strong>{formatScore(uploadResult.score)}</strong>
                </article>
                <article className="stat-card">
                  <span>Paper ID</span>
                  <strong>{uploadResult.paperId ?? "-"}</strong>
                </article>
                <article className="stat-card">
                  <span>Version ID</span>
                  <strong>{uploadResult.versionId ?? "-"}</strong>
                </article>
                <article className="stat-card">
                  <span>File</span>
                  <strong>{uploadResult.fileName || selectedFile?.name || "-"}</strong>
                </article>
              </div>

              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Rule name</th>
                      <th>Status</th>
                      <th>Message</th>
                      <th>Suggestion</th>
                    </tr>
                  </thead>
                  <tbody>
                    {normalizedItems.length === 0 ? (
                      <tr>
                        <td colSpan="4" className="empty-row">
                          No rule results returned for this upload.
                        </td>
                      </tr>
                    ) : (
                      normalizedItems.map((item) => (
                        <tr key={item.id}>
                          <td>{item.ruleName}</td>
                          <td>
                            <span
                              className={`status-pill ${
                                item.status === "PASS" ? "pass" : "fail"
                              }`}
                            >
                              {item.status}
                            </span>
                          </td>
                          <td>{item.message}</td>
                          <td>{item.suggestion}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  );
}

async function parseResponse(response) {
  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message || "Request failed.");
  }

  return data;
}

function formatScore(score) {
  if (score === null || score === undefined || Number.isNaN(Number(score))) {
    return "-";
  }

  return `${Number(score).toFixed(1)}%`;
}

export default App;
