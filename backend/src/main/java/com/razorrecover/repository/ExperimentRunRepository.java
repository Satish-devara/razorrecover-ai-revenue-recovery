package com.razorrecover.repository;

import com.razorrecover.domain.ExperimentRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, UUID> {
}
