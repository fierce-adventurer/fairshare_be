package com.fairshare.billing.controller;

import com.fairshare.billing.dto.BillResponse;
import com.fairshare.billing.dto.CreateBillRequest;
import com.fairshare.billing.service.UpcomingBillService;
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
@RequestMapping("/api/v1/bills")
@Tag(name = "Upcoming Bills", description = "Track upcoming and recurring due bills")
public class UpcomingBillController {

    private final UpcomingBillService billService;

    public UpcomingBillController(UpcomingBillService billService) {
        this.billService = billService;
    }

    @GetMapping
    @Operation(summary = "List upcoming and overdue bills for current user")
    public ResponseEntity<ApiResponse<List<BillResponse>>> listBills(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<BillResponse> bills = billService.listUserBills(principal.id());
        return ResponseEntity.ok(ApiResponse.ok(bills));
    }

    @PostMapping
    @Operation(summary = "Create an upcoming bill with due date and assignees")
    public ResponseEntity<ApiResponse<BillResponse>> createBill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateBillRequest request
    ) {
        BillResponse response = billService.createBill(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Upcoming bill created successfully"));
    }

    @PutMapping("/{id}/pay")
    @Operation(summary = "Mark an upcoming bill as paid")
    public ResponseEntity<ApiResponse<BillResponse>> markPaid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        BillResponse response = billService.markPaid(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Bill marked as paid"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an upcoming bill")
    public ResponseEntity<ApiResponse<Void>> deleteBill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        billService.deleteBill(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Bill deleted successfully"));
    }
}
