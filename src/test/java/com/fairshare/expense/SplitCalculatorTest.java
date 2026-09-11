package com.fairshare.expense;

import com.fairshare.expense.engine.SplitCalculator;
import com.fairshare.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculatorTest {

    private SplitCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SplitCalculator();
    }

    @Test
    void testEqualSplitWithRemainderUnits() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();

        // 100.00 split 3 ways -> 33.34, 33.33, 33.33
        List<SplitCalculator.AllocationResult> results = calculator.splitEqually(10000, List.of(u1, u2, u3));

        assertEquals(3, results.size());
        long sum = results.stream().mapToLong(SplitCalculator.AllocationResult::amountMinor).sum();
        assertEquals(10000, sum, "Sum of allocations must equal 10000 minor units");

        assertEquals(3334, results.get(0).amountMinor());
        assertEquals(3333, results.get(1).amountMinor());
        assertEquals(3333, results.get(2).amountMinor());
    }

    @Test
    void testSplitByShares() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        // 3000 split in 2:1 ratio -> 2000, 1000
        List<SplitCalculator.AllocationResult> results = calculator.splitByShares(3000, Map.of(u1, 2, u2, 1));

        long sum = results.stream().mapToLong(SplitCalculator.AllocationResult::amountMinor).sum();
        assertEquals(3000, sum);
    }

    @Test
    void testSplitByPercentages() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        // 5000 with 60% and 40%
        List<SplitCalculator.AllocationResult> results = calculator.splitByPercentages(5000, Map.of(u1, 60.0, u2, 40.0));

        long sum = results.stream().mapToLong(SplitCalculator.AllocationResult::amountMinor).sum();
        assertEquals(5000, sum);
    }

    @Test
    void testValidateExpenseInvariantSuccess() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        List<SplitCalculator.AllocationResult> payers = List.of(new SplitCalculator.AllocationResult(u1, 5000));
        List<SplitCalculator.AllocationResult> shares = List.of(
                new SplitCalculator.AllocationResult(u1, 2500),
                new SplitCalculator.AllocationResult(u2, 2500)
        );

        assertDoesNotThrow(() -> calculator.validateExpenseInvariant(5000, payers, shares));
    }

    @Test
    void testValidateExpenseInvariantMismatchThrows() {
        UUID u1 = UUID.randomUUID();
        List<SplitCalculator.AllocationResult> payers = List.of(new SplitCalculator.AllocationResult(u1, 4000));
        List<SplitCalculator.AllocationResult> shares = List.of(new SplitCalculator.AllocationResult(u1, 5000));

        assertThrows(BadRequestException.class, () -> calculator.validateExpenseInvariant(5000, payers, shares));
    }
}
