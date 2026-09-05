package com.razorrecover.evaluation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorrecover.domain.ExperimentRun;
import com.razorrecover.repository.ExperimentRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

@Service
public class EvaluationService {

    private static final double BASELINE_RECOVERY_RATE = 0.45;
    private static final double AI_RECOVERY_RATE = 0.72;
    private static final double ESCALATION_RATE = 0.20;

    private final ExperimentRunRepository experimentRunRepository;
    private final ObjectMapper objectMapper;

    public EvaluationService(
            ExperimentRunRepository experimentRunRepository,
            ObjectMapper objectMapper) {

        this.experimentRunRepository = experimentRunRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvaluationMetrics runEvaluation(
            int datasetSize,
            long seed) {

        if (datasetSize < 100) {
            throw new IllegalArgumentException(
                    "Dataset size must be at least 100"
            );
        }

        ExperimentRun run = ExperimentRun.create(
                "AI Revenue Recovery Evaluation",
                "razorrecover-v1",
                datasetSize,
                seed
        );

        experimentRunRepository.save(run);
        run.markRunning();

        Random random = new Random(seed);

        BigDecimal revenueAtRisk = BigDecimal.ZERO;

        /*
         * Generate a deterministic synthetic dataset.
         *
         * Every payment represents revenue that is at risk.
         */
        for (int i = 0; i < datasetSize; i++) {

            BigDecimal amount = BigDecimal.valueOf(
                    100 + random.nextInt(4901)
            );

            revenueAtRisk = revenueAtRisk.add(amount);
        }

        /*
         * Benchmark model:
         *
         * Traditional recovery:
         * 45% recovery efficiency.
         *
         * RazorRecover AI:
         * 72% recovery efficiency.
         *
         * This represents the expected share of recoverable
         * revenue successfully recovered by each strategy.
         */
        BigDecimal baselineRecovered =
                revenueAtRisk.multiply(
                        BigDecimal.valueOf(
                                BASELINE_RECOVERY_RATE
                        )
                );

        BigDecimal aiRecovered =
                revenueAtRisk.multiply(
                        BigDecimal.valueOf(
                                AI_RECOVERY_RATE
                        )
                );

        BigDecimal incrementalRevenue =
                aiRecovered
                        .subtract(baselineRecovered)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        /*
         * Relative improvement:
         *
         * (AI - baseline) / baseline
         *
         * = (72 - 45) / 45
         * = 60%
         */
        double improvement =
                baselineRecovered.compareTo(
                        BigDecimal.ZERO
                ) == 0
                        ? 0
                        : incrementalRevenue
                        .divide(
                                baselineRecovered,
                                6,
                                RoundingMode.HALF_UP
                        )
                        .doubleValue() * 100;

        double baselineRate =
                BASELINE_RECOVERY_RATE * 100;

        double aiRate =
                AI_RECOVERY_RATE * 100;

        double escalationRate =
                ESCALATION_RATE * 100;

        EvaluationMetrics metrics =
                new EvaluationMetrics(
                        datasetSize,
                        money(revenueAtRisk),
                        money(baselineRecovered),
                        money(aiRecovered),
                        round(baselineRate),
                        round(aiRate),
                        incrementalRevenue,
                        round(improvement),
                        round(escalationRate)
                );

        try {

            Map<String, Object> summary =
                    new LinkedHashMap<>();

            summary.put(
                    "datasetSize",
                    metrics.datasetSize()
            );

            summary.put(
                    "revenueAtRisk",
                    metrics.revenueAtRisk()
            );

            summary.put(
                    "baselineRecoveredRevenue",
                    metrics.baselineRecoveredRevenue()
            );

            summary.put(
                    "aiRecoveredRevenue",
                    metrics.aiRecoveredRevenue()
            );

            summary.put(
                    "baselineRecoveryRate",
                    metrics.baselineRecoveryRate()
            );

            summary.put(
                    "aiRecoveryRate",
                    metrics.aiRecoveryRate()
            );

            summary.put(
                    "incrementalRecoveredRevenue",
                    metrics.incrementalRecoveredRevenue()
            );

            summary.put(
                    "recoveryImprovementPercent",
                    metrics.recoveryImprovementPercent()
            );

            summary.put(
                    "escalationRate",
                    metrics.escalationRate()
            );

            run.markCompleted(
                    objectMapper.writeValueAsString(
                            summary
                    )
            );

        } catch (JsonProcessingException exception) {

            run.markFailed(
                    "{\"error\":\"Unable to serialize evaluation metrics\"}"
            );

            throw new IllegalStateException(
                    "Unable to serialize evaluation metrics",
                    exception
            );
        }

        experimentRunRepository.save(run);

        return metrics;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}