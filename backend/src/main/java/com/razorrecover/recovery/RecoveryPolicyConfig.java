package com.razorrecover.recovery;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recovery")
public class RecoveryPolicyConfig {

    private int maxRetries = 3;

    private Duration recoveryWindow = Duration.ofHours(24);

    private Duration cooldown = Duration.ofMinutes(5);

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRecoveryWindow() {
        return recoveryWindow;
    }

    public void setRecoveryWindow(Duration recoveryWindow) {
        this.recoveryWindow = recoveryWindow;
    }

    public Duration getCooldown() {
        return cooldown;
    }

    public void setCooldown(Duration cooldown) {
        this.cooldown = cooldown;
    }
}
