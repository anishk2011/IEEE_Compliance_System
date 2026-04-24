package com.ieee.pdfchecker.dto;

public class ImprovementTrendsDto {

    private final long totalPapersCompared;
    private final long papersImproved;
    private final long papersWorsened;
    private final long papersUnchanged;
    private final double averageScoreDelta;

    public ImprovementTrendsDto(long totalPapersCompared, long papersImproved,
                                long papersWorsened, long papersUnchanged,
                                double averageScoreDelta) {
        this.totalPapersCompared = totalPapersCompared;
        this.papersImproved = papersImproved;
        this.papersWorsened = papersWorsened;
        this.papersUnchanged = papersUnchanged;
        this.averageScoreDelta = averageScoreDelta;
    }

    public long getTotalPapersCompared() {
        return totalPapersCompared;
    }

    public long getPapersImproved() {
        return papersImproved;
    }

    public long getPapersWorsened() {
        return papersWorsened;
    }

    public long getPapersUnchanged() {
        return papersUnchanged;
    }

    public double getAverageScoreDelta() {
        return averageScoreDelta;
    }
}
