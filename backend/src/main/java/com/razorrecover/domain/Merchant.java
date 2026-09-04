package com.razorrecover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "merchants")
public class Merchant extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "external_reference", nullable = false, unique = true, length = 100)
    private String externalReference;

    @Column(name = "auto_recovery_enabled", nullable = false)
    private boolean autoRecoveryEnabled = true;

    @Column(name = "max_automatic_recovery_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAutomaticRecoveryAmount;

    protected Merchant() {
    }
}
