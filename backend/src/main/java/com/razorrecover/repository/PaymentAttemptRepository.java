package com.razorrecover.repository;

import com.razorrecover.domain.PaymentAttempt;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);

    int countByPaymentId(UUID paymentId);
}
