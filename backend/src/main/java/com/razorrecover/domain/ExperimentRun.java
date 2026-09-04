package com.razorrecover.domain;

import com.razorrecover.domain.enums.ExperimentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ExperimentStatus status;

    @Column(name = "metrics_summary", columnDefinition = "text")
    private String metricsSummary;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ExperimentRun() {
    }
}
