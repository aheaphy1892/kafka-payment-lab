package dev.portfolio.payments.payment.api.repository.specifications;

import dev.portfolio.payments.payment.api.entities.Payment;
import dev.portfolio.payments.payment.api.enums.PaymentStatus;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import java.time.Instant;

public final class PaymentSpecifications {

    private PaymentSpecifications() {}

    public static Specification<Payment> filtered(PaymentStatus status, String currency, Instant from, Instant to) {
        Specification<Payment> spec = Specification.unrestricted();

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (currency != null && !currency.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("currency"), currency.toUpperCase(
                Locale.ROOT)));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }

        return spec;
    }
}
