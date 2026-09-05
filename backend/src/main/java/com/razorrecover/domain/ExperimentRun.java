package com.razorrecover.domain;

import com.razorrecover.domain.enums.ExperimentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "experiment_runs")
public class ExperimentRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "strategy_version", nullable = false, length = 80)
    private String strategyVersion;

    @Column(name = "dataset_size", nullable = false)
    private int datasetSize;

    @Column(name = "random_seed", nullable = false)
    private long randomSeed;

    @Column(nullable = false, length = 40)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ExperimentStatus status;

    @Column(name = "metrics_summary", columnDefinition = "text")
    private String metricsSummary;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ExperimentRun() {
    }

    public static ExperimentRun create(
            String name,
            String strategyVersion,
            int datasetSize,
            long randomSeed) {

        ExperimentRun run = new ExperimentRun();
        run.name = name;
        run.strategyVersion = strategyVersion;
        run.datasetSize = datasetSize;
        run.randomSeed = randomSeed;
        run.status = ExperimentStatus.CREATED;
        return run;
    }

    public void markRunning() {
        this.status = ExperimentStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String metricsSummary) {
        this.status = ExperimentStatus.COMPLETED;
        this.metricsSummary = metricsSummary;
        this.completedAt = Instant.now();
    }

    public void markFailed(String metricsSummary) {
        this.status = ExperimentStatus.FAILED;
        this.metricsSummary = metricsSummary;
        this.completedAt = Instant.now();
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getStrategyVersion() {
        return strategyVersion;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public String getMetricsSummary() {
        return metricsSummary;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}