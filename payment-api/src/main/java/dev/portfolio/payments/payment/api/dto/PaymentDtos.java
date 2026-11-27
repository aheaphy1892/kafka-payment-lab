package dev.portfolio.payments.payment.api.dto;

import dev.portfolio.payments.payment.api.entities.Payment;
import dev.portfolio.payments.payment.api.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record CreatePaymentRequest(@NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal amount,
                                       @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) {}

    public record PaymentResponse(UUID paymentId, BigDecimal amount, String currency, PaymentStatus status, Instant createdAt) {
        public static PaymentResponse from(Payment p) { return new PaymentResponse(p.getId(),p.getAmount(),p.getCurrency(),p.getStatus(),p.getCreatedAt()); }
    }
}
