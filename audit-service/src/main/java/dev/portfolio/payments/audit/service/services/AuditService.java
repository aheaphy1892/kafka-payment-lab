package dev.portfolio.payments.audit.service.services;
import dev.portfolio.payments.audit.service.repositories.AuditRepository;
import dev.portfolio.payments.contracts.PaymentEvents;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository=repository;
    }

    @Transactional
    public void record(PaymentEvents.Requested event,int partition,long offset) {
        repository.append(event,partition,offset);
    }
}
