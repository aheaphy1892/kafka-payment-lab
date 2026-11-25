# Security and production-readiness boundaries

This is an educational lab, **not** a deployable payment service.

- `/admin/payments` is **not authenticated or authorised**. Never expose it publicly. Add Spring Security, OAuth2 resource-server JWT validation, role-based access controls and audit logging before any deployment.
- Kafka and PostgreSQL use development-only plaintext credentials and listeners.
- No real card details, bank accounts, money movement or customer PII are processed.
- Outbox relay currently assumes one relay instance per service; production needs safe multi-instance claiming (e.g. `FOR UPDATE SKIP LOCKED` plus leases), monitoring and backpressure.
- Dead-letter handling requires operational alerting, inspection, replay policy and poison-message controls.
- The demo does not guarantee cross-service exactly-once effects or transactional ordering across independent partitions.
- The demo uses a single Kafka broker and no TLS/SASL, backup policy, schema registry, tracing or infrastructure hardening.
