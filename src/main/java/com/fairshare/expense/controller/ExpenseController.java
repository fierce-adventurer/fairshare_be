package com.fairshare.expense.controller;

import com.fairshare.expense.dto.CreateExpenseRequest;
import com.fairshare.expense.dto.ExpenseResponse;
import com.fairshare.expense.dto.UpdateExpenseRequest;
import com.fairshare.expense.service.ExpenseService;
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
@Tag(name = "Expenses", description = "Expense creation, split calculation, updates, and querying")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/api/v1/groups/{groupId}/expenses")
    @Operation(summary = "List all expenses for a group")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> listGroupExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId
    ) {
        List<ExpenseResponse> expenses = expenseService.listGroupExpenses(principal.id(), groupId);
        return ResponseEntity.ok(ApiResponse.ok(expenses));
    }

    @PostMapping("/api/v1/groups/{groupId}/expenses")
    @Operation(summary = "Record a new expense in a group with split allocations")
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        ExpenseResponse expense = expenseService.createExpense(principal.id(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(expense, "Expense created successfully"));
    }

    @GetMapping("/api/v1/users/me/expenses")
    @Operation(summary = "Unified view: get all expenses across all groups for current user")
    public ResponseEntity<ApiResponse<List<ExpenseResponse>>> listAllUserExpenses(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ExpenseResponse> expenses = expenseService.listAllUserExpenses(principal.id());
        return ResponseEntity.ok(ApiResponse.ok(expenses));
    }

    @GetMapping("/api/v1/expenses/{id}")
    @Operation(summary = "Get single expense details")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        ExpenseResponse expense = expenseService.getExpense(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(expense));
    }

    @PutMapping("/api/v1/expenses/{id}")
    @Operation(summary = "Update an existing expense and re-allocate splits")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        ExpenseResponse expense = expenseService.updateExpense(principal.id(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(expense, "Expense updated successfully"));
    }

    @DeleteMapping("/api/v1/expenses/{id}")
    @Operation(summary = "Soft delete an expense")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        expenseService.deleteExpense(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Expense deleted successfully"));
    }
}
