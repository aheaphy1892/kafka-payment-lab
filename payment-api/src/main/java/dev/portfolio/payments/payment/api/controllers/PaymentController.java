package dev.portfolio.payments.payment.api.controllers;

import dev.portfolio.payments.payment.api.services.PaymentService;
import dev.portfolio.payments.payment.api.dto.PaymentDtos;
import dev.portfolio.payments.payment.api.dto.PaymentDtos.PaymentResponse;
import jakarta.validation.Valid;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<PaymentResponse> submit(@Valid @RequestBody PaymentDtos.CreatePaymentRequest request) {
        var response = service.submit(request);
        return ResponseEntity.accepted().location(java.net.URI.create("/payments/" + response.paymentId())).body(response);
    }

    @GetMapping("/{id}")
    public PaymentDtos.PaymentResponse get(@PathVariable UUID id) { return service.get(id); }
}
