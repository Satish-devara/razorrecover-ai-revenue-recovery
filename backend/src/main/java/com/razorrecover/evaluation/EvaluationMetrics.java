package com.razorrecover.evaluation;

import java.math.BigDecimal;

public record EvaluationMetrics(
        int datasetSize,
        BigDecimal revenueAtRisk,
        BigDecimal baselineRecoveredRevenue,
        BigDecimal aiRecoveredRevenue,
        double baselineRecoveryRate,
        double aiRecoveryRate,
        BigDecimal incrementalRecoveredRevenue,
        double recoveryImprovementPercent,
        double escalationRate
) {
}
