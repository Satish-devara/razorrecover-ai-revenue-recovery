package com.razorrecover;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.razorrecover.recovery.RecoveryPolicyConfig;

@SpringBootApplication
@EnableConfigurationProperties(RecoveryPolicyConfig.class)
public class RazorRecoverApplication {

    public static void main(String[] args) {
        SpringApplication.run(RazorRecoverApplication.class, args);
    }
}
