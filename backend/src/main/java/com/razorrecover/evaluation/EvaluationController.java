package com.razorrecover.evaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/run")
    public ResponseEntity<EvaluationMetrics> runEvaluation(
            @RequestParam(defaultValue = "1000")
            @Min(100)
            @Max(100000)
            int datasetSize,

            @RequestParam(defaultValue = "42")
            long seed) {

        return ResponseEntity.ok(
                evaluationService.runEvaluation(datasetSize, seed)
        );
    }
}
