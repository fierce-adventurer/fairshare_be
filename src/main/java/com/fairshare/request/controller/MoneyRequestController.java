package com.fairshare.request.controller;

import com.fairshare.request.dto.CreateMoneyRequestRequest;
import com.fairshare.request.dto.MoneyRequestResponse;
import com.fairshare.request.service.MoneyRequestService;
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
@RequestMapping("/api/v1/requests")
@Tag(name = "Money Requests", description = "Money request tracking with expiration for accountability")
public class MoneyRequestController {

    private final MoneyRequestService requestService;

    public MoneyRequestController(MoneyRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping
    @Operation(summary = "List all incoming and outgoing money requests for current user")
    public ResponseEntity<ApiResponse<List<MoneyRequestResponse>>> listRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<MoneyRequestResponse> requests = requestService.listUserRequests(principal.id());
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @PostMapping
    @Operation(summary = "Create a new money request with expiration timestamp")
    public ResponseEntity<ApiResponse<MoneyRequestResponse>> createRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateMoneyRequestRequest request
    ) {
        MoneyRequestResponse response = requestService.createRequest(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Money request sent"));
    }

    @PutMapping("/{id}/settle")
    @Operation(summary = "Mark a money request as settled")
    public ResponseEntity<ApiResponse<MoneyRequestResponse>> settleRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        MoneyRequestResponse response = requestService.settleRequest(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Money request settled"));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an open money request")
    public ResponseEntity<ApiResponse<MoneyRequestResponse>> cancelRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        MoneyRequestResponse response = requestService.cancelRequest(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Money request cancelled"));
    }
}
