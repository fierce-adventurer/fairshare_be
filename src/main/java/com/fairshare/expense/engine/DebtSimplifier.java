package com.fairshare.expense.engine;

import com.fairshare.group.dto.DebtResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DebtSimplifier {

    public List<DebtResponse> simplify(Map<UUID, Long> netBalances) {
        long sum = netBalances.values().stream().mapToLong(Long::longValue).sum();
        if (sum != 0) {
            throw new IllegalStateException("Balances must sum to zero before simplification, but got: " + sum);
        }

        record BalanceEntry(UUID userId, long amount) {}

        List<BalanceEntry> debtors = netBalances.entrySet().stream()
                .filter(e -> e.getValue() < 0)
                .map(e -> new BalanceEntry(e.getKey(), -e.getValue()))
                .sorted(Comparator.comparingLong(BalanceEntry::amount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        List<BalanceEntry> creditors = netBalances.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new BalanceEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(BalanceEntry::amount).reversed())
                .collect(Collectors.toCollection(ArrayList::new));

        List<DebtResponse> result = new ArrayList<>();
        int dIdx = 0;
        int cIdx = 0;

        while (dIdx < debtors.size() && cIdx < creditors.size()) {
            BalanceEntry debtor = debtors.get(dIdx);
            BalanceEntry creditor = creditors.get(cIdx);

            long settleAmount = Math.min(debtor.amount(), creditor.amount());
            if (settleAmount > 0) {
                result.add(new DebtResponse(debtor.userId(), creditor.userId(), settleAmount));
            }

            debtors.set(dIdx, new BalanceEntry(debtor.userId(), debtor.amount() - settleAmount));
            creditors.set(cIdx, new BalanceEntry(creditor.userId(), creditor.amount() - settleAmount));

            if (debtors.get(dIdx).amount() == 0) dIdx++;
            if (creditors.get(cIdx).amount() == 0) cIdx++;
        }

        return result;
    }
}
