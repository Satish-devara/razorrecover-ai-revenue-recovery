package com.razorrecover.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRecoveryCaseRequest(@NotNull UUID paymentId) {
}
