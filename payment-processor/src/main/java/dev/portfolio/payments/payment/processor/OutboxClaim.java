package dev.portfolio.payments.payment.processor;

import java.util.UUID;

/** A short-lived lease for one outbox row claimed by a relay instance. */
public record OutboxClaim(UUID id, String aggregateId, String topic, String payload) {}
