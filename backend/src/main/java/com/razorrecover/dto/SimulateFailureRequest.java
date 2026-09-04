package com.razorrecover.dto;

import com.razorrecover.domain.enums.SimulationScenario;
import jakarta.validation.constraints.NotNull;

public record SimulateFailureRequest(@NotNull SimulationScenario scenario) {
}
