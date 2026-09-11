package com.fairshare.expense;

import com.fairshare.expense.engine.DebtSimplifier;
import com.fairshare.group.dto.DebtResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DebtSimplifierTest {

    private DebtSimplifier simplifier;

    @BeforeEach
    void setUp() {
        simplifier = new DebtSimplifier();
    }

    @Test
    void testTransitiveDebtSimplification() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        // Suppose A owes B $20, and B owes C $20.
        // Net balances: A: -2000, B: 0, C: +2000
        Map<UUID, Long> balances = new LinkedHashMap<>();
        balances.put(a, -2000L);
        balances.put(b, 0L);
        balances.put(c, 2000L);

        List<DebtResponse> debts = simplifier.simplify(balances);

        // Should produce exactly 1 direct debt: A owes C $20
        assertEquals(1, debts.size());
        assertEquals(a, debts.get(0).fromUserId());
        assertEquals(c, debts.get(0).toUserId());
        assertEquals(2000L, debts.get(0).amountMinor());
    }

    @Test
    void testComplexMultipleDebts() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();

        // Net: A: -5000, B: -3000, C: +6000, D: +2000 (Sum = 0)
        Map<UUID, Long> balances = new LinkedHashMap<>();
        balances.put(a, -5000L);
        balances.put(b, -3000L);
        balances.put(c, 6000L);
        balances.put(d, 2000L);

        List<DebtResponse> debts = simplifier.simplify(balances);

        // Sum of all settlements must equal total positive balance (8000)
        long totalSettled = debts.stream().mapToLong(DebtResponse::amountMinor).sum();
        assertEquals(8000L, totalSettled);
        assertTrue(debts.size() <= 3, "Transactions should be minimal");
    }

    @Test
    void testAllZeroBalancesYieldsEmptyDebts() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        Map<UUID, Long> balances = Map.of(a, 0L, b, 0L);
        List<DebtResponse> debts = simplifier.simplify(balances);

        assertTrue(debts.isEmpty());
    }
}
