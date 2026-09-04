package com.razorrecover.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "recovery_policies")
public class RecoveryPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Column(name = "policy_code", nullable = false, length = 100)
    private String policyCode;

    @Column(nullable = false, length = 40)
    private String version;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(nullable = false)
    private int priority = 100;

    @Column(nullable = false)
    private boolean active = true;

    protected RecoveryPolicy() {
    }
}
