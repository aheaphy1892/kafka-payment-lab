package dev.portfolio.payments.payment.api.entities;


import dev.portfolio.payments.payment.api.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(UUID id, BigDecimal amount, String currency) {
        this.id = id; this.amount = amount; this.currency = currency;
        this.status = PaymentStatus.PENDING; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void applyResult(PaymentStatus result) {
        if (status == PaymentStatus.PENDING) status = result;
        else if (status != result) throw new IllegalStateException("Conflicting payment result for " + id);
    }
}
