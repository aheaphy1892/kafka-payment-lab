package dev.portfolio.payments.payment.api.controllers;


import dev.portfolio.payments.payment.api.services.PaymentService;
import dev.portfolio.payments.payment.api.dto.PaymentDtos.PaymentResponse;
import dev.portfolio.payments.payment.api.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/payments")
public class AdminPaymentController {

    private final PaymentService service;

    public AdminPaymentController(PaymentService service) { this.service = service; }

    @GetMapping
    public Page<PaymentResponse> list(
        @RequestParam(required=false) PaymentStatus status,
        @RequestParam(required=false) String currency,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @PageableDefault(size=20, sort="createdAt", direction= Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getPageSize()>100) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Maximum page size is 100");

        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid date range");

        var allowed = java.util.Set.of("createdAt", "amount", "currency", "status", "id");

        for (var order : pageable.getSort()) if (!allowed.contains(order.getProperty())) throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported sort field");

        return service.search(status, currency, createdFrom, createdTo, pageable);
    }
}
