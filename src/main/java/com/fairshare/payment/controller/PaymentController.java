package com.fairshare.payment.controller;

import com.fairshare.payment.dto.PaymentResponse;
import com.fairshare.payment.dto.RecordPaymentRequest;
import com.fairshare.payment.service.PaymentService;
import com.fairshare.shared.dto.ApiResponse;
import com.fairshare.shared.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Payments", description = "Record settlements and list group payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/v1/groups/{groupId}/payments")
    @Operation(summary = "List all settlement payments for a group")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId
    ) {
        List<PaymentResponse> payments = paymentService.listGroupPayments(principal.id(), groupId);
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }

    @PostMapping("/api/v1/groups/{groupId}/payments")
    @Operation(summary = "Record a settlement payment in a group")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody RecordPaymentRequest request
    ) {
        PaymentResponse response = paymentService.recordPayment(principal.id(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Settlement payment recorded successfully"));
    }

    @DeleteMapping("/api/v1/payments/{id}")
    @Operation(summary = "Delete a payment record")
    public ResponseEntity<ApiResponse<Void>> deletePayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        paymentService.deletePayment(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Payment deleted"));
    }
}
