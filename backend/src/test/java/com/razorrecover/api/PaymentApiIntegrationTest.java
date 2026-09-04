package com.razorrecover.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "razorpay.enabled=false"
})
@AutoConfigureMockMvc
class PaymentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndRetrievesSuccessfulPayment() throws Exception {

        String paymentId = createPayment("SUCCESS");

        mockMvc.perform(
                        get("/api/payments/{paymentId}", paymentId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.failureReason").isEmpty());
    }

    @Test
    void retriesFailedPaymentToSuccessWithDeterministicScenario()
            throws Exception {

        String paymentId = createPayment("RETRY_THEN_SUCCESS");

        mockMvc.perform(
                        post("/api/payments/{paymentId}/retries", paymentId)
                                .header(
                                        "Idempotency-Key",
                                        "retry-success-test"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));

        mockMvc.perform(
                        get("/api/payments/{paymentId}/attempts", paymentId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].status").value("SUCCEEDED"));
    }

    @Test
    void retryThenFailureRemainsFailed() throws Exception {

        String paymentId = createPayment("RETRY_THEN_FAILURE");

        mockMvc.perform(
                        post("/api/payments/{paymentId}/retries", paymentId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(
                        jsonPath("$.failureReason")
                                .value("PERMANENT_FAILURE")
                );
    }

    @Test
    void rejectsRetryOfSuccessfulPayment() throws Exception {

        String paymentId = createPayment("SUCCESS");

        mockMvc.perform(
                        post("/api/payments/{paymentId}/retries", paymentId)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("INVALID_STATE")
                );
    }

    @Test
    void opensRecoveryCaseAndReturnsPaymentAuditHistory()
            throws Exception {

        String paymentId =
                createPayment("TEMPORARY_NETWORK_FAILURE");

        MvcResult recoveryResult =
                mockMvc.perform(
                                post("/api/recovery-cases")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"paymentId\":\""
                                                        + paymentId
                                                        + "\"}"
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.status")
                                        .value("OPEN")
                        )
                        .andReturn();

        String recoveryCaseId =
                json(recoveryResult)
                        .path("id")
                        .asText();

        MvcResult auditResult =
                mockMvc.perform(
                                get(
                                        "/api/recovery-cases/{recoveryCaseId}/audit-events",
                                        recoveryCaseId
                                )
                        )
                        .andExpect(status().isOk())
                        .andReturn();

        String auditBody =
                auditResult
                        .getResponse()
                        .getContentAsString();

        assertThat(auditBody)
                .contains(
                        "PAYMENT_CREATED",
                        "PAYMENT_FAILED",
                        "RECOVERY_CASE_CREATED"
                );
    }

    @Test
    void simulatesFailureForPendingPayment()
            throws Exception {

        MvcResult createResult =
                mockMvc.perform(
                                post("/api/payments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "amount":250.00,
                                                  "currency":"INR",
                                                  "scenario":"SUCCESS",
                                                  "processImmediately":false
                                                }
                                                """
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andExpect(
                                jsonPath("$.status")
                                        .value("PENDING")
                        )
                        .andReturn();

        String paymentId =
                json(createResult)
                        .path("id")
                        .asText();

        mockMvc.perform(
                        post(
                                "/api/payments/{paymentId}/simulate-failure",
                                paymentId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "scenario":"TIMEOUT"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("FAILED")
                );
    }

    @Test
    void getsPaymentAttempts() throws Exception {

        String paymentId =
                createPayment("RETRY_THEN_SUCCESS");

        mockMvc.perform(
                        post("/api/payments/{paymentId}/retries", paymentId)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("SUCCEEDED")
                );

        mockMvc.perform(
                        get(
                                "/api/payments/{paymentId}/attempts",
                                paymentId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    private String createPayment(String scenario)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/api/payments")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {
                                                  "amount":250.00,
                                                  "currency":"INR",
                                                  "scenario":"%s",
                                                  "processImmediately":true
                                                }
                                                """.formatted(scenario)
                                        )
                        )
                        .andExpect(status().isCreated())
                        .andReturn();

        return json(result)
                .path("id")
                .asText();
    }

    private JsonNode json(MvcResult result)
            throws Exception {

        return objectMapper.readTree(
                result
                        .getResponse()
                        .getContentAsString()
        );
    }
}