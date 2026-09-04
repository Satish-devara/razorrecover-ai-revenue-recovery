package com.razorrecover.repository;

import com.razorrecover.domain.PaymentAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
}
