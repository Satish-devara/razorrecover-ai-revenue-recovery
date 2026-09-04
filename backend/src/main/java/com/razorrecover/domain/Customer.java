package com.razorrecover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "external_reference", nullable = false, length = 100)
    private String externalReference;

    @Column(length = 320)
    private String email;

    @Column(name = "successful_payment_count", nullable = false)
    private int successfulPaymentCount;

    @Column(name = "failed_payment_count", nullable = false)
    private int failedPaymentCount;

    protected Customer() {
    }
}
